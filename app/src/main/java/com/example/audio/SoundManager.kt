package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.chess.SoundProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.Semaphore
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class ChessSound {
    MOVE,
    CAPTURE,
    CASTLE,
    CHECK,
    VICTORY,
    DEFEAT,
    PUZZLE_SOLVED
}

/**
 * Sound synthesizer adhering to Ruqaa Section 12 & 13:
 * - Default disabled (sound = false), profile SOFT, volume = 55%
 * - 3 profiles (soft, wood, digital) across move/capture/check/end events
 * - Max 2 concurrent channels via Semaphore(2), USAGE_GAME + CONTENT_TYPE_SONIFICATION
 */
class SoundManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sampleRate = 22050
    private val channelLimit = Semaphore(2)

    @Volatile
    var enabled: Boolean = false

    @Volatile
    var profile: SoundProfile = SoundProfile.SOFT

    @Volatile
    var volumePercent: Int = 55

    fun play(sound: ChessSound) {
        if (!enabled || volumePercent <= 0) return
        if (!channelLimit.tryAcquire()) return
        scope.launch {
            try {
                runCatching {
                    val gain = (volumePercent.coerceIn(0, 100) / 100.0)
                    val samples = when (profile) {
                        SoundProfile.SOFT -> synthesizeForProfile(sound, baseFreq = 180.0, decay = 65.0, sharpBoost = 0.20, digital = false, gain = gain)
                        SoundProfile.WOOD -> synthesizeForProfile(sound, baseFreq = 220.0, decay = 50.0, sharpBoost = 0.45, digital = false, gain = gain)
                        SoundProfile.DIGITAL -> synthesizeForProfile(sound, baseFreq = 460.0, decay = 38.0, sharpBoost = 0.55, digital = true, gain = gain)
                    }
                    playPcm(samples)
                }
            } finally {
                channelLimit.release()
            }
        }
    }

    private fun synthesizeForProfile(
        sound: ChessSound,
        baseFreq: Double,
        decay: Double,
        sharpBoost: Double,
        digital: Boolean,
        gain: Double
    ): ShortArray {
        return when (sound) {
            ChessSound.MOVE -> synthesizeTap(baseFreq, 52, decay, sharpBoost * 0.5, digital, gain)
            ChessSound.CAPTURE -> synthesizeTap(baseFreq * 0.78, 78, decay * 0.85, sharpBoost, digital, gain)
            ChessSound.CASTLE -> combine(
                synthesizeTap(baseFreq, 46, decay, sharpBoost * 0.5, digital, gain),
                ShortArray(sampleRate / 22),
                synthesizeTap(baseFreq * 1.15, 52, decay, sharpBoost * 0.5, digital, gain)
            )
            ChessSound.CHECK -> synthesizeChime(
                if (digital) doubleArrayOf(784.0, 1046.5) else doubleArrayOf(620.0, 820.0),
                noteMs = 70,
                gain = gain
            )
            ChessSound.VICTORY -> synthesizeChime(doubleArrayOf(523.25, 659.25, 783.99, 1046.5), noteMs = 90, gain = gain)
            ChessSound.DEFEAT -> synthesizeChime(doubleArrayOf(392.0, 311.13, 261.63), noteMs = 105, gain = gain)
            ChessSound.PUZZLE_SOLVED -> synthesizeChime(doubleArrayOf(587.33, 739.99, 880.0, 1174.66), noteMs = 80, gain = gain)
        }
    }

    private fun synthesizeTap(
        freqHz: Double,
        durationMs: Int,
        decay: Double,
        harmonicBoost: Double,
        digital: Boolean,
        gain: Double
    ): ShortArray {
        val count = (sampleRate * durationMs) / 1000
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * decay)
            val body = if (digital) {
                sin(2.0 * PI * freqHz * t) + harmonicBoost * sin(2.0 * PI * (freqHz * 2.0) * t)
            } else {
                sin(2.0 * PI * freqHz * t) + harmonicBoost * sin(2.0 * PI * (freqHz * 2.3) * t)
            }
            val v = (body * env * 0.45 * gain * Short.MAX_VALUE).toInt()
            out[i] = v.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun synthesizeChime(freqs: DoubleArray, noteMs: Int, gain: Double): ShortArray {
        val perNote = (sampleRate * noteMs) / 1000
        val out = ShortArray(perNote * freqs.size)
        freqs.forEachIndexed { idx, freq ->
            val offset = idx * perNote
            for (i in 0 until perNote) {
                val t = i.toDouble() / sampleRate
                val env = exp(-t * 14.0)
                val tone = sin(2.0 * PI * freq * t) + 0.3 * sin(2.0 * PI * freq * 2.0 * t)
                val v = (tone * env * 0.35 * gain * Short.MAX_VALUE).toInt()
                out[offset + i] = v.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        return out
    }

    private fun combine(vararg parts: ShortArray): ShortArray {
        val total = parts.sumOf { it.size }
        val res = ShortArray(total)
        var pos = 0
        for (p in parts) {
            p.copyInto(res, pos)
            pos += p.size
        }
        return res
    }

    private fun playPcm(samples: ShortArray) {
        val byteLen = samples.size * 2
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(byteLen.coerceAtLeast(1024))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        try {
            track.write(samples, 0, samples.size)
            track.play()
            val durationMs = (samples.size * 1000L) / sampleRate + 50L
            Thread.sleep(durationMs)
        } finally {
            runCatching { track.stop() }
            runCatching { track.release() }
        }
    }
}

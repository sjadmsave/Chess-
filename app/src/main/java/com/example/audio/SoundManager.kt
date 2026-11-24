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
import kotlin.random.Random

enum class ChessSound {
    MOVE,
    CAPTURE,
    CASTLE,
    CHECK,
    PROMOTION,
    ILLEGAL,
    LOW_TIME,
    HINT,
    DRAW,
    GAME_START,
    BLUNDER,
    PREMOVE,
    VICTORY,
    DEFEAT,
    PUZZLE_SOLVED
}

/**
 * Multi-profile chess audio synthesizer:
 * - 8 distinct acoustic profiles (SOFT, WOOD, DIGITAL, MARBLE, CLASSIC_CLICK, TOURNAMENT, CRYSTAL, ARCADE)
 * - 15 distinct chess game & coaching events
 * - Safe AudioTrack PCM playback bounded by Semaphore(2)
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
        triggerPcm(sound, profile, volumePercent)
    }

    fun previewProfile(targetProfile: SoundProfile, volumeOverride: Int = volumePercent) {
        val gainVol = volumeOverride.coerceAtLeast(35)
        triggerPcm(ChessSound.CAPTURE, targetProfile, gainVol)
    }

    private fun triggerPcm(sound: ChessSound, activeProfile: SoundProfile, volPct: Int) {
        if (!channelLimit.tryAcquire()) return
        scope.launch {
            try {
                runCatching {
                    val gain = (volPct.coerceIn(0, 100) / 100.0)
                    val samples = synthesizeForProfile(sound, activeProfile, gain)
                    playPcm(samples)
                }
            } finally {
                channelLimit.release()
            }
        }
    }

    private data class ProfileTimbre(
        val baseFreq: Double,
        val decay: Double,
        val harmonicRatio: Double,
        val harmonicBoost: Double,
        val noiseMix: Double,
        val digitalSquareMix: Double
    )

    private fun timbreFor(p: SoundProfile): ProfileTimbre = when (p) {
        SoundProfile.SOFT -> ProfileTimbre(175.0, 62.0, 2.1, 0.22, 0.14, 0.0)
        SoundProfile.WOOD -> ProfileTimbre(235.0, 50.0, 2.45, 0.48, 0.34, 0.0)
        SoundProfile.DIGITAL -> ProfileTimbre(480.0, 42.0, 2.0, 0.50, 0.02, 0.22)
        SoundProfile.MARBLE -> ProfileTimbre(620.0, 70.0, 3.1, 0.42, 0.26, 0.0)
        SoundProfile.CLASSIC_CLICK -> ProfileTimbre(320.0, 75.0, 2.75, 0.55, 0.40, 0.0)
        SoundProfile.TOURNAMENT -> ProfileTimbre(195.0, 44.0, 2.25, 0.52, 0.38, 0.0)
        SoundProfile.CRYSTAL -> ProfileTimbre(980.0, 26.0, 2.5, 0.36, 0.04, 0.0)
        SoundProfile.ARCADE -> ProfileTimbre(540.0, 34.0, 1.5, 0.58, 0.05, 0.36)
    }

    private fun synthesizeForProfile(
        sound: ChessSound,
        activeProfile: SoundProfile,
        gain: Double
    ): ShortArray {
        val tb = timbreFor(activeProfile)
        return when (sound) {
            ChessSound.MOVE -> synthesizePercussive(
                freqHz = tb.baseFreq,
                durationMs = 56,
                decay = tb.decay,
                tb = tb,
                gain = gain
            )
            ChessSound.PREMOVE -> synthesizePercussive(
                freqHz = tb.baseFreq * 1.28,
                durationMs = 40,
                decay = tb.decay * 1.25,
                tb = tb,
                gain = gain * 0.72
            )
            ChessSound.CAPTURE -> combine(
                synthesizePercussive(tb.baseFreq * 1.35, 34, tb.decay * 1.1, tb, gain * 0.85),
                synthesizePercussive(tb.baseFreq * 0.72, 74, tb.decay * 0.82, tb, gain)
            )
            ChessSound.CASTLE -> combine(
                synthesizePercussive(tb.baseFreq, 46, tb.decay, tb, gain),
                ShortArray(sampleRate / 22),
                synthesizePercussive(tb.baseFreq * 1.18, 56, tb.decay, tb, gain)
            )
            ChessSound.CHECK -> synthesizeChime(
                freqs = if (tb.digitalSquareMix > 0.1) doubleArrayOf(784.0, 1046.5) else doubleArrayOf(622.25, 830.61),
                noteMs = 68,
                gain = gain
            )
            ChessSound.PROMOTION -> synthesizeChime(
                freqs = doubleArrayOf(440.0, 554.37, 659.25, 880.0),
                noteMs = 64,
                gain = gain
            )
            ChessSound.ILLEGAL -> synthesizeChime(
                freqs = doubleArrayOf(196.0, 164.81),
                noteMs = 65,
                gain = gain * 0.85
            )
            ChessSound.LOW_TIME -> synthesizeChime(
                freqs = doubleArrayOf(880.0, 880.0),
                noteMs = 55,
                gain = gain * 0.90
            )
            ChessSound.HINT -> synthesizeChime(
                freqs = doubleArrayOf(587.33, 880.0),
                noteMs = 70,
                gain = gain * 0.82
            )
            ChessSound.BLUNDER -> synthesizeChime(
                freqs = doubleArrayOf(349.23, 277.18, 220.0),
                noteMs = 82,
                gain = gain * 0.9
            )
            ChessSound.DRAW -> synthesizeChime(
                freqs = doubleArrayOf(440.0, 440.0, 523.25),
                noteMs = 85,
                gain = gain * 0.85
            )
            ChessSound.GAME_START -> synthesizeChime(
                freqs = doubleArrayOf(392.0, 493.88, 587.33),
                noteMs = 70,
                gain = gain * 0.85
            )
            ChessSound.VICTORY -> synthesizeChime(
                freqs = doubleArrayOf(523.25, 659.25, 783.99, 1046.5),
                noteMs = 90,
                gain = gain
            )
            ChessSound.DEFEAT -> synthesizeChime(
                freqs = doubleArrayOf(392.0, 311.13, 261.63),
                noteMs = 105,
                gain = gain
            )
            ChessSound.PUZZLE_SOLVED -> synthesizeChime(
                freqs = doubleArrayOf(587.33, 739.99, 880.0, 1174.66),
                noteMs = 80,
                gain = gain
            )
        }
    }

    private fun synthesizePercussive(
        freqHz: Double,
        durationMs: Int,
        decay: Double,
        tb: ProfileTimbre,
        gain: Double
    ): ShortArray {
        val count = (sampleRate * durationMs) / 1000
        val out = ShortArray(count)
        val rng = Random(freqHz.toInt() + count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * decay)
            val transientEnv = exp(-t * (decay * 3.8))
            val fundamental = sin(2.0 * PI * freqHz * t)
            val harmonic = sin(2.0 * PI * (freqHz * tb.harmonicRatio) * t)
            val squareApprox = if (tb.digitalSquareMix > 0.0) {
                val sq = if (fundamental >= 0.0) 1.0 else -1.0
                tb.digitalSquareMix * sq * 0.55
            } else 0.0
            val clickNoise = (rng.nextDouble() * 2.0 - 1.0) * tb.noiseMix * transientEnv
            val sampleVal = ((fundamental + tb.harmonicBoost * harmonic + squareApprox) * env + clickNoise)
            val v = (sampleVal * 0.44 * gain * Short.MAX_VALUE).toInt()
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
            val durationMs = (samples.size * 1000L) / sampleRate + 40L
            Thread.sleep(durationMs)
        } finally {
            runCatching { track.stop() }
            runCatching { track.release() }
        }
    }
}

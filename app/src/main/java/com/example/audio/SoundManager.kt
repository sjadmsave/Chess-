package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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

class SoundManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sampleRate = 22050

    @Volatile
    var enabled: Boolean = true

    fun play(sound: ChessSound) {
        if (!enabled) return
        scope.launch {
            runCatching {
                val samples = when (sound) {
                    ChessSound.MOVE -> synthesizeWoodTap(freqHz = 210.0, durationMs = 55)
                    ChessSound.CAPTURE -> synthesizeWoodTap(freqHz = 145.0, durationMs = 80, sharp = true)
                    ChessSound.CASTLE -> combine(
                        synthesizeWoodTap(210.0, 50),
                        ShortArray(sampleRate / 20), // ~50ms pause
                        synthesizeWoodTap(250.0, 55)
                    )
                    ChessSound.CHECK -> synthesizeChime(doubleArrayOf(660.0, 880.0), noteMs = 75)
                    ChessSound.VICTORY -> synthesizeChime(doubleArrayOf(523.25, 659.25, 783.99, 1046.5), noteMs = 95)
                    ChessSound.DEFEAT -> synthesizeChime(doubleArrayOf(392.0, 311.13, 261.63), noteMs = 110)
                    ChessSound.PUZZLE_SOLVED -> synthesizeChime(doubleArrayOf(587.33, 739.99, 880.0, 1174.66), noteMs = 80)
                }
                playPcm(samples)
            }
        }
    }

    private fun synthesizeWoodTap(freqHz: Double, durationMs: Int, sharp: Boolean = false): ShortArray {
        val count = (sampleRate * durationMs) / 1000
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * if (sharp) 48.0 else 58.0)
            val body = sin(2.0 * PI * freqHz * t) +
                0.45 * sin(2.0 * PI * (freqHz * 2.3) * t) +
                if (sharp) 0.35 * sin(2.0 * PI * (freqHz * 4.1) * t) else 0.0
            val v = (body * env * 0.45 * Short.MAX_VALUE).toInt()
            out[i] = v.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun synthesizeChime(freqs: DoubleArray, noteMs: Int): ShortArray {
        val perNote = (sampleRate * noteMs) / 1000
        val out = ShortArray(perNote * freqs.size)
        freqs.forEachIndexed { idx, freq ->
            val offset = idx * perNote
            for (i in 0 until perNote) {
                val t = i.toDouble() / sampleRate
                val env = exp(-t * 14.0)
                val tone = sin(2.0 * PI * freq * t) + 0.3 * sin(2.0 * PI * freq * 2.0 * t)
                val v = (tone * env * 0.35 * Short.MAX_VALUE).toInt()
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

        track.write(samples, 0, samples.size)
        track.play()
        val durationMs = (samples.size * 1000L) / sampleRate + 60L
        Thread.sleep(durationMs)
        track.stop()
        track.release()
    }
}

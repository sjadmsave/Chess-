package com.example.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.chess.VibrationStyle

enum class ChessHapticEvent {
    MOVE,
    CAPTURE,
    CASTLE,
    CHECK,
    GAME_END
}

/**
 * Haptic feedback controller per Ruqaa Section 12 & 13:
 * - Default disabled (vibrate = false), style 0 (amplitude 65)
 * - 3 intensities: amplitude 65, 125, 200
 * - Distinct pulse patterns for move / capture / castle / check / game end
 */
class HapticsManager(context: Context) {

    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            mgr?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    @Volatile
    var enabled: Boolean = false

    @Volatile
    var style: VibrationStyle = VibrationStyle.LIGHT

    fun trigger(event: ChessHapticEvent) {
        if (!enabled) return
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        val baseAmp = style.amplitude
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (event) {
                    ChessHapticEvent.MOVE -> VibrationEffect.createOneShot(18L, baseAmp)
                    ChessHapticEvent.CAPTURE -> VibrationEffect.createWaveform(
                        longArrayOf(0L, 26L, 24L, 18L),
                        intArrayOf(0, baseAmp, 0, (baseAmp * 0.8f).toInt().coerceAtLeast(30)),
                        -1
                    )
                    ChessHapticEvent.CASTLE -> VibrationEffect.createWaveform(
                        longArrayOf(0L, 16L, 35L, 22L),
                        intArrayOf(0, baseAmp, 0, baseAmp),
                        -1
                    )
                    ChessHapticEvent.CHECK -> VibrationEffect.createWaveform(
                        longArrayOf(0L, 32L, 32L, 42L),
                        intArrayOf(0, baseAmp, 0, (baseAmp + 45).coerceAtMost(255)),
                        -1
                    )
                    ChessHapticEvent.GAME_END -> VibrationEffect.createWaveform(
                        longArrayOf(0L, 45L, 40L, 45L, 40L, 70L),
                        intArrayOf(0, baseAmp, 0, baseAmp, 0, (baseAmp + 50).coerceAtMost(255)),
                        -1
                    )
                }
                vib.vibrate(effect)
            }
        }
    }
}

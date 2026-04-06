package com.example.superiorcontroller.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object ButtonHaptics {

    const val INTENSITY_SOFT = "soft"
    const val INTENSITY_MEDIUM = "medium"
    const val INTENSITY_STRONG = "strong"

    @Volatile var enabled: Boolean = true
    @Volatile var intensity: String = INTENSITY_MEDIUM

    private var vibrator: Vibrator? = null
    private var hasAmplitudeControl = false

    fun init(context: Context) {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                mgr?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            hasAmplitudeControl = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.hasAmplitudeControl() == true
            } else false
        } catch (_: Exception) {
            vibrator = null
        }
    }

    fun performClick(context: Context, label: String = "") {
        if (!enabled) return
        val vib = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val (durationMs, amplitude) = resolveParams()
                val amp = if (hasAmplitudeControl) amplitude else VibrationEffect.DEFAULT_AMPLITUDE
                vib.vibrate(VibrationEffect.createOneShot(durationMs, amp))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(resolveDuration())
            }
        } catch (_: Exception) { /* silent fail */ }
    }

    private fun resolveParams(): Pair<Long, Int> = when (intensity) {
        INTENSITY_SOFT   -> 12L to 60
        INTENSITY_STRONG -> 30L to 255
        else             -> 20L to 140
    }

    private fun resolveDuration(): Long = when (intensity) {
        INTENSITY_SOFT   -> 10L
        INTENSITY_STRONG -> 35L
        else             -> 20L
    }
}

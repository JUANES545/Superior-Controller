package com.example.superiorcontroller.input

import kotlin.math.abs

/**
 * Quantizes analog stick and trigger values to discrete levels
 * for more reproducible HID recording/playback.
 *
 * Stick modes:
 * - 8DIR:      8 directions + neutral (3 levels per axis, independent)
 * - 4DIR:      4 cardinal directions + neutral at full intensity
 * - STABLE75:  4 cardinal directions at 75% intensity
 * - STABLE50:  4 cardinal directions at 50% intensity
 */
object InputQuantizer {

    const val MODE_8DIR = "8dir"
    const val MODE_4DIR = "4dir"
    const val MODE_STABLE75 = "stable75"
    const val MODE_STABLE50 = "stable50"

    @Deprecated("Use MODE_STABLE50", replaceWith = ReplaceWith("MODE_STABLE50"))
    const val MODE_PRECISION = "precision"

    private const val STICK_DEADZONE = 0.40f
    private const val TRIGGER_THRESHOLD = 0.25f

    fun quantizeStick(x: Float, y: Float, mode: String): Pair<Float, Float> = when (mode) {
        MODE_4DIR -> quantize4Dir(x, y, 1f)
        MODE_STABLE75 -> quantize4Dir(x, y, 0.75f)
        MODE_STABLE50, @Suppress("DEPRECATION") MODE_PRECISION -> quantize4Dir(x, y, 0.50f)
        else -> Pair(quantizeAxis(x), quantizeAxis(y))
    }

    fun quantizeTrigger(value: Float): Float =
        if (value >= TRIGGER_THRESHOLD) 1f else 0f

    private fun quantizeAxis(value: Float): Float = when {
        value < -STICK_DEADZONE -> -1f
        value > STICK_DEADZONE -> 1f
        else -> 0f
    }

    private fun quantize4Dir(x: Float, y: Float, intensity: Float): Pair<Float, Float> {
        val ax = abs(x)
        val ay = abs(y)
        if (ax < STICK_DEADZONE && ay < STICK_DEADZONE) return 0f to 0f
        return if (ax >= ay) {
            (if (x > 0) intensity else -intensity) to 0f
        } else {
            0f to (if (y > 0) intensity else -intensity)
        }
    }
}

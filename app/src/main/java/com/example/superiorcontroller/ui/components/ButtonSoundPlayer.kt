package com.example.superiorcontroller.ui.components

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * Generates and plays short synthetic click sounds.
 * No audio files required — all tones are produced programmatically.
 *
 * Styles:
 * - SOFT:      quiet sine click (800 Hz, 15ms)
 * - SHORT:     sharp sine click (1200 Hz, 8ms)
 * - ARCADE:    two-tone blip (600→900 Hz, 20ms)
 * - MECHANICAL: noise-like burst with fast decay (25ms)
 */
object ButtonSoundPlayer {

    const val STYLE_SOFT = "soft"
    const val STYLE_SHORT = "short"
    const val STYLE_ARCADE = "arcade"
    const val STYLE_MECHANICAL = "mechanical"

    @Volatile var enabled: Boolean = true
    @Volatile var style: String = STYLE_SOFT
    @Volatile var volume: Float = 0.7f

    private const val SAMPLE_RATE = 22050
    private var ready = false

    private val cache = mutableMapOf<String, ShortArray>()

    fun init() {
        try {
            cache[STYLE_SOFT] = generateSine(800.0, 15, 0.35f)
            cache[STYLE_SHORT] = generateSine(1200.0, 8, 0.50f)
            cache[STYLE_ARCADE] = generateChirp(600.0, 900.0, 20, 0.45f)
            cache[STYLE_MECHANICAL] = generateNoiseBurst(25, 0.40f)
            ready = true
        } catch (_: Exception) {
            ready = false
        }
    }

    fun playClick(label: String = "") {
        if (!enabled || !ready) return
        playInternal(style, volume)
    }

    fun playPreview(styleOverride: String = style, volumeOverride: Float = volume) {
        if (!ready) return
        playInternal(styleOverride, volumeOverride)
    }

    private fun playInternal(s: String, v: Float) {
        val samples = cache[s] ?: cache[STYLE_SOFT] ?: return
        val vol = v.coerceIn(0f, 1f)
        if (vol == 0f) return
        try {
            val bufSize = samples.size * 2
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setBufferSizeInBytes(bufSize.coerceAtLeast(AudioTrack.getMinBufferSize(
                    SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
                )))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            track.setVolume(vol)
            track.setNotificationMarkerPosition(samples.size)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack?) { t?.release() }
                override fun onPeriodicNotification(t: AudioTrack?) {}
            })
            track.play()
        } catch (_: Exception) { /* silent fail */ }
    }

    fun release() {
        cache.clear()
        ready = false
    }

    // ── Tone generators ──

    private fun generateSine(freqHz: Double, durationMs: Int, volume: Float): ShortArray {
        val numSamples = (SAMPLE_RATE * durationMs / 1000.0).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = fadeEnvelope(i, numSamples)
            val value = sin(2.0 * PI * freqHz * t) * volume * envelope
            samples[i] = (value * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    private fun generateChirp(startHz: Double, endHz: Double, durationMs: Int, volume: Float): ShortArray {
        val numSamples = (SAMPLE_RATE * durationMs / 1000.0).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val freq = startHz + (endHz - startHz) * progress
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = fadeEnvelope(i, numSamples)
            val value = sin(2.0 * PI * freq * t) * volume * envelope
            samples[i] = (value * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    private fun generateNoiseBurst(durationMs: Int, volume: Float): ShortArray {
        val numSamples = (SAMPLE_RATE * durationMs / 1000.0).toInt()
        val samples = ShortArray(numSamples)
        val random = java.util.Random(42)
        for (i in 0 until numSamples) {
            val envelope = fadeEnvelope(i, numSamples, attackRatio = 0.02f, releaseRatio = 0.5f)
            val noise = (random.nextFloat() * 2f - 1f) * volume * envelope
            samples[i] = (noise * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    private fun fadeEnvelope(i: Int, total: Int, attackRatio: Float = 0.05f, releaseRatio: Float = 0.3f): Float {
        val attackSamples = (total * attackRatio).toInt().coerceAtLeast(1)
        val releaseSamples = (total * releaseRatio).toInt().coerceAtLeast(1)
        return when {
            i < attackSamples -> i.toFloat() / attackSamples
            i > total - releaseSamples -> (total - i).toFloat() / releaseSamples
            else -> 1f
        }
    }
}

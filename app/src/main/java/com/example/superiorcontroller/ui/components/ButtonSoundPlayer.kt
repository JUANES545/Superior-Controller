package com.example.superiorcontroller.ui.components

import android.media.AudioAttributes
import android.media.SoundPool

object ButtonSoundPlayer {

    @Volatile
    var enabled: Boolean = true

    private var soundPool: SoundPool? = null
    private var clickSoundId: Int = 0
    private var loaded = false

    private fun ensurePool() {
        if (soundPool != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()
            .also { pool ->
                pool.setOnLoadCompleteListener { _, _, status ->
                    if (status == 0) loaded = true
                }
            }
    }

    fun playClick(label: String = "") {
        if (!enabled) return
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        loaded = false
    }
}

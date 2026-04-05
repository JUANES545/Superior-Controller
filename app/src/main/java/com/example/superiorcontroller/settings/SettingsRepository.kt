package com.example.superiorcontroller.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _hapticsEnabled = MutableStateFlow(prefs.getBoolean(Keys.HAPTICS, true))
    private val _soundEnabled = MutableStateFlow(prefs.getBoolean(Keys.SOUND, true))
    private val _triggerMode = MutableStateFlow(prefs.getString(Keys.TRIGGER_MODE, MODE_ANALOG) ?: MODE_ANALOG)
    private val _debugLogVisible = MutableStateFlow(prefs.getBoolean(Keys.DEBUG_LOG, false))
    private val _debugLogOverlay = MutableStateFlow(prefs.getBoolean(Keys.DEBUG_OVERLAY, false))

    val hapticsEnabled: Flow<Boolean> = _hapticsEnabled
    val soundEnabled: Flow<Boolean> = _soundEnabled
    val triggerMode: Flow<String> = _triggerMode
    val debugLogVisible: Flow<Boolean> = _debugLogVisible
    val debugLogOverlay: Flow<Boolean> = _debugLogOverlay

    suspend fun setHapticsEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) { prefs.edit().putBoolean(Keys.HAPTICS, enabled).apply() }
        _hapticsEnabled.value = enabled
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) { prefs.edit().putBoolean(Keys.SOUND, enabled).apply() }
        _soundEnabled.value = enabled
    }

    suspend fun setTriggerMode(mode: String) {
        withContext(Dispatchers.IO) { prefs.edit().putString(Keys.TRIGGER_MODE, mode).apply() }
        _triggerMode.value = mode
    }

    suspend fun setDebugLogVisible(visible: Boolean) {
        withContext(Dispatchers.IO) { prefs.edit().putBoolean(Keys.DEBUG_LOG, visible).apply() }
        _debugLogVisible.value = visible
    }

    suspend fun setDebugLogOverlay(overlay: Boolean) {
        withContext(Dispatchers.IO) { prefs.edit().putBoolean(Keys.DEBUG_OVERLAY, overlay).apply() }
        _debugLogOverlay.value = overlay
    }

    private object Keys {
        const val HAPTICS = "haptics_enabled"
        const val SOUND = "sound_enabled"
        const val TRIGGER_MODE = "trigger_mode"
        const val DEBUG_LOG = "debug_log_visible"
        const val DEBUG_OVERLAY = "debug_log_overlay"
    }

    companion object {
        const val MODE_ANALOG = "analog"
        const val MODE_BUTTON = "button"
    }
}

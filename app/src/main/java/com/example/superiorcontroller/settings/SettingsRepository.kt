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

    private val _controllerProfile = MutableStateFlow(prefs.getString(Keys.CONTROLLER_PROFILE, PROFILE_XBOX) ?: PROFILE_XBOX)
    private val _hapticsEnabled = MutableStateFlow(prefs.getBoolean(Keys.HAPTICS, true))
    private val _hapticsIntensity = MutableStateFlow(prefs.getString(Keys.HAPTICS_INTENSITY, HAPTICS_MEDIUM) ?: HAPTICS_MEDIUM)
    private val _soundEnabled = MutableStateFlow(prefs.getBoolean(Keys.SOUND, true))
    private val _soundStyle = MutableStateFlow(prefs.getString(Keys.SOUND_STYLE, SOUND_SOFT) ?: SOUND_SOFT)
    private val _soundVolume = MutableStateFlow(prefs.getFloat(Keys.SOUND_VOLUME, 0.7f))
    private val _triggerMode = MutableStateFlow(prefs.getString(Keys.TRIGGER_MODE, MODE_ANALOG) ?: MODE_ANALOG)
    private val _debugLogVisible = MutableStateFlow(prefs.getBoolean(Keys.DEBUG_LOG, false))
    private val _debugLogOverlay = MutableStateFlow(prefs.getBoolean(Keys.DEBUG_OVERLAY, false))
    private val _digitalRecording = MutableStateFlow(prefs.getBoolean(Keys.DIGITAL_RECORDING, false))
    private val _macroWarningSuppressed = MutableStateFlow(prefs.getBoolean(Keys.MACRO_WARNING_SUPPRESSED, false))
    private val _assistLeftMode = MutableStateFlow(prefs.getString(Keys.ASSIST_LEFT_MODE, ASSIST_8DIR) ?: ASSIST_8DIR)
    private val _assistRightMode = MutableStateFlow(
        migrateRightMode(prefs.getString(Keys.ASSIST_RIGHT_MODE, ASSIST_STABLE75) ?: ASSIST_STABLE75)
    )
    private val _assistLeftTempo = MutableStateFlow(prefs.getString(Keys.ASSIST_LEFT_TEMPO, TEMPO_GRID) ?: TEMPO_GRID)
    private val _assistRightTempo = MutableStateFlow(prefs.getString(Keys.ASSIST_RIGHT_TEMPO, TEMPO_PULSE) ?: TEMPO_PULSE)
    private val _profileWarningSuppressed = MutableStateFlow(prefs.getBoolean(Keys.PROFILE_WARNING_SUPPRESSED, false))
    private val _onboardingCompleted = MutableStateFlow(
        if (prefs.contains(Keys.ONBOARDING_COMPLETED)) prefs.getBoolean(Keys.ONBOARDING_COMPLETED, false)
        else prefs.all.isNotEmpty()
    )

    val controllerProfile: Flow<String> = _controllerProfile
    val hapticsEnabled: Flow<Boolean> = _hapticsEnabled
    val hapticsIntensity: Flow<String> = _hapticsIntensity
    val soundEnabled: Flow<Boolean> = _soundEnabled
    val soundStyle: Flow<String> = _soundStyle
    val soundVolume: Flow<Float> = _soundVolume
    val triggerMode: Flow<String> = _triggerMode
    val debugLogVisible: Flow<Boolean> = _debugLogVisible
    val debugLogOverlay: Flow<Boolean> = _debugLogOverlay
    val digitalRecording: Flow<Boolean> = _digitalRecording
    val macroWarningSuppressed: Flow<Boolean> = _macroWarningSuppressed
    val assistLeftMode: Flow<String> = _assistLeftMode
    val assistRightMode: Flow<String> = _assistRightMode
    val assistLeftTempo: Flow<String> = _assistLeftTempo
    val assistRightTempo: Flow<String> = _assistRightTempo
    val profileWarningSuppressed: Flow<Boolean> = _profileWarningSuppressed
    val onboardingCompleted: Flow<Boolean> = _onboardingCompleted

    suspend fun setControllerProfile(profile: String) {
        withContext(Dispatchers.IO) { prefs.edit().putString(Keys.CONTROLLER_PROFILE, profile).apply() }
        _controllerProfile.value = profile
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) { prefs.edit().putBoolean(Keys.HAPTICS, enabled).apply() }
        _hapticsEnabled.value = enabled
    }

    suspend fun setHapticsIntensity(intensity: String) {
        withContext(Dispatchers.IO) { prefs.edit().putString(Keys.HAPTICS_INTENSITY, intensity).apply() }
        _hapticsIntensity.value = intensity
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) { prefs.edit().putBoolean(Keys.SOUND, enabled).apply() }
        _soundEnabled.value = enabled
    }

    suspend fun setSoundStyle(style: String) {
        withContext(Dispatchers.IO) { prefs.edit().putString(Keys.SOUND_STYLE, style).apply() }
        _soundStyle.value = style
    }

    suspend fun setSoundVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        withContext(Dispatchers.IO) { prefs.edit().putFloat(Keys.SOUND_VOLUME, v).apply() }
        _soundVolume.value = v
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

    suspend fun setDigitalRecording(enabled: Boolean) {
        withContext(Dispatchers.IO) { prefs.edit().putBoolean(Keys.DIGITAL_RECORDING, enabled).apply() }
        _digitalRecording.value = enabled
    }

    suspend fun setMacroWarningSuppressed(suppressed: Boolean) {
        withContext(Dispatchers.IO) { prefs.edit().putBoolean(Keys.MACRO_WARNING_SUPPRESSED, suppressed).apply() }
        _macroWarningSuppressed.value = suppressed
    }

    suspend fun setAssistLeftMode(mode: String) {
        withContext(Dispatchers.IO) { prefs.edit().putString(Keys.ASSIST_LEFT_MODE, mode).apply() }
        _assistLeftMode.value = mode
    }

    suspend fun setAssistRightMode(mode: String) {
        withContext(Dispatchers.IO) { prefs.edit().putString(Keys.ASSIST_RIGHT_MODE, mode).apply() }
        _assistRightMode.value = mode
    }

    suspend fun setAssistLeftTempo(mode: String) {
        withContext(Dispatchers.IO) { prefs.edit().putString(Keys.ASSIST_LEFT_TEMPO, mode).apply() }
        _assistLeftTempo.value = mode
    }

    suspend fun setAssistRightTempo(mode: String) {
        withContext(Dispatchers.IO) { prefs.edit().putString(Keys.ASSIST_RIGHT_TEMPO, mode).apply() }
        _assistRightTempo.value = mode
    }

    suspend fun setProfileWarningSuppressed(suppressed: Boolean) {
        withContext(Dispatchers.IO) { prefs.edit().putBoolean(Keys.PROFILE_WARNING_SUPPRESSED, suppressed).apply() }
        _profileWarningSuppressed.value = suppressed
    }

    suspend fun setOnboardingCompleted() {
        withContext(Dispatchers.IO) { prefs.edit().putBoolean(Keys.ONBOARDING_COMPLETED, true).apply() }
        _onboardingCompleted.value = true
    }

    private object Keys {
        const val CONTROLLER_PROFILE = "controller_profile"
        const val HAPTICS = "haptics_enabled"
        const val HAPTICS_INTENSITY = "haptics_intensity"
        const val SOUND = "sound_enabled"
        const val SOUND_STYLE = "sound_style"
        const val SOUND_VOLUME = "sound_volume"
        const val TRIGGER_MODE = "trigger_mode"
        const val DEBUG_LOG = "debug_log_visible"
        const val DEBUG_OVERLAY = "debug_log_overlay"
        const val DIGITAL_RECORDING = "digital_recording"
        const val MACRO_WARNING_SUPPRESSED = "macro_warning_suppressed"
        const val ASSIST_LEFT_MODE = "assist_left_mode"
        const val ASSIST_RIGHT_MODE = "assist_right_mode"
        const val ASSIST_LEFT_TEMPO = "assist_left_tempo"
        const val ASSIST_RIGHT_TEMPO = "assist_right_tempo"
        const val PROFILE_WARNING_SUPPRESSED = "profile_warning_suppressed"
        const val ONBOARDING_COMPLETED = "onboarding_completed"
    }

    companion object {
        const val MODE_ANALOG = "analog"
        const val MODE_BUTTON = "button"
        const val PROFILE_XBOX = "xbox"
        const val PROFILE_PLAYSTATION = "playstation"
        const val ASSIST_8DIR = "8dir"
        const val ASSIST_4DIR = "4dir"
        const val ASSIST_STABLE75 = "stable75"
        const val ASSIST_STABLE50 = "stable50"
        const val TEMPO_FREE = "free"
        const val TEMPO_GRID = "grid"
        const val TEMPO_PULSE = "pulse"
        const val HAPTICS_SOFT = "soft"
        const val HAPTICS_MEDIUM = "medium"
        const val HAPTICS_STRONG = "strong"
        const val SOUND_SOFT = "soft"
        const val SOUND_SHORT = "short"
        const val SOUND_ARCADE = "arcade"
        const val SOUND_MECHANICAL = "mechanical"

        private fun migrateRightMode(stored: String): String = when (stored) {
            "precision" -> ASSIST_STABLE50
            else -> stored
        }
    }
}

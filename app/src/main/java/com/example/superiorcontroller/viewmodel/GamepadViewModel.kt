package com.example.superiorcontroller.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.superiorcontroller.bluetooth.BluetoothHidManager
import com.example.superiorcontroller.hid.AxisDefaults
import com.example.superiorcontroller.hid.GamepadButtons
import com.example.superiorcontroller.hid.GamepadReportBuilder
import com.example.superiorcontroller.hid.GamepadState
import com.example.superiorcontroller.hid.HidDescriptor
import com.example.superiorcontroller.hid.TriggerDefaults
import com.example.superiorcontroller.settings.SettingsRepository
import com.example.superiorcontroller.ui.components.ButtonHaptics
import com.example.superiorcontroller.ui.components.ButtonSoundPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GamepadViewModel(application: Application) : AndroidViewModel(application) {

    private val _gamepadState = MutableStateFlow(GamepadState())
    val gamepadState: StateFlow<GamepadState> = _gamepadState.asStateFlow()

    private val _bluetoothAvailable = MutableStateFlow(false)
    val bluetoothAvailable: StateFlow<Boolean> = _bluetoothAvailable.asStateFlow()

    private val _proxyReady = MutableStateFlow(false)
    val proxyReady: StateFlow<Boolean> = _proxyReady.asStateFlow()

    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _logMessages = MutableStateFlow<List<String>>(emptyList())
    val logMessages: StateFlow<List<String>> = _logMessages.asStateFlow()

    private val _reportsSent = MutableStateFlow(0L)
    val reportsSent: StateFlow<Long> = _reportsSent.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(true)
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _triggerMode = MutableStateFlow(SettingsRepository.MODE_ANALOG)
    val triggerMode: StateFlow<String> = _triggerMode.asStateFlow()

    private val _debugLogVisible = MutableStateFlow(true)
    val debugLogVisible: StateFlow<Boolean> = _debugLogVisible.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val settingsRepo = SettingsRepository(application.applicationContext)
    private val hidManager = BluetoothHidManager(application.applicationContext)

    private val managerListener = object : BluetoothHidManager.Listener {
        override fun onProxyReady() { _proxyReady.value = true }
        override fun onRegistrationChanged(registered: Boolean) { _isRegistered.value = registered }
        override fun onConnectionChanged(device: BluetoothDevice?, connected: Boolean) {
            _isConnected.value = connected
            _connectedDeviceName.value = device?.name ?: device?.address
        }
        override fun onLog(message: String) { addLog(message) }
    }

    init {
        hidManager.listener = managerListener
        addLog("Mode: 11btn+2trig | report=${HidDescriptor.REPORT_SIZE}B | throttle=${BluetoothHidManager.MIN_INTERVAL_MS}ms")

        viewModelScope.launch {
            settingsRepo.hapticsEnabled.collect { enabled ->
                _hapticsEnabled.value = enabled
                ButtonHaptics.enabled = enabled
                addLog("Settings: haptics=${if (enabled) "ON" else "OFF"}")
            }
        }
        viewModelScope.launch {
            settingsRepo.soundEnabled.collect { enabled ->
                _soundEnabled.value = enabled
                ButtonSoundPlayer.enabled = enabled
                addLog("Settings: sound=${if (enabled) "ON" else "OFF"}")
            }
        }
        viewModelScope.launch {
            settingsRepo.triggerMode.collect { mode ->
                _triggerMode.value = mode
                addLog("Settings: triggerMode=$mode")
            }
        }
        viewModelScope.launch {
            settingsRepo.debugLogVisible.collect { visible ->
                _debugLogVisible.value = visible
            }
        }
    }

    // ── Lifecycle ───────────────────────────────────────────────────────

    fun initializeBluetooth() {
        val result = hidManager.initialize()
        _bluetoothAvailable.value = hidManager.isBluetoothAvailable() && hidManager.isBluetoothEnabled()
        if (!result) addLog("Bluetooth initialization failed")
    }

    fun registerHidApp() {
        if (!hidManager.isProxyReady()) { addLog("Proxy not ready"); return }
        hidManager.registerApp()
    }

    fun unregisterHidApp() { hidManager.unregisterApp() }
    fun getBondedDevices(): List<BluetoothDevice> = hidManager.getBondedDevices()
    fun connectToDevice(device: BluetoothDevice) { hidManager.connectToHost(device) }

    // ── Button controls ─────────────────────────────────────────────────

    fun pressButton(button: Int) {
        val state = _gamepadState.value

        if (GamepadButtons.isDpad(button)) {
            if ((state.dpad and button) != 0) return
            _gamepadState.value = state.withDpadPressed(button)
        } else {
            if (state.isButtonPressed(button)) return
            _gamepadState.value = state.withButtonPressed(button)
        }

        val s = _gamepadState.value
        val name = buttonName(button)
        val report = GamepadReportBuilder.buildReport(s)
        addLog("▶ PRESS $name mask=0x${"%04X".format(s.buttons)} ${GamepadReportBuilder.describeBytes(s)} hex=[${GamepadReportBuilder.toHexString(report)}]")
        sendForced("PRESS $name")
    }

    fun releaseButton(button: Int) {
        val state = _gamepadState.value

        if (GamepadButtons.isDpad(button)) {
            if ((state.dpad and button) == 0) return
            _gamepadState.value = state.withDpadReleased(button)
        } else {
            if (!state.isButtonPressed(button)) return
            _gamepadState.value = state.withButtonReleased(button)
        }

        val s = _gamepadState.value
        val name = buttonName(button)
        addLog("■ RELEASE $name mask=0x${"%04X".format(s.buttons)} hex=[${GamepadReportBuilder.toHexString(GamepadReportBuilder.buildReport(s))}]")
        sendForced("RELEASE $name")
    }

    private fun buttonName(button: Int): String = when (button) {
        GamepadButtons.A -> "A"; GamepadButtons.B -> "B"
        GamepadButtons.X -> "X"; GamepadButtons.Y -> "Y"
        GamepadButtons.LB -> "LB"; GamepadButtons.RB -> "RB"
        GamepadButtons.BACK -> "BACK"; GamepadButtons.START -> "START"
        GamepadButtons.L3 -> "L3"; GamepadButtons.R3 -> "R3"
        GamepadButtons.HOME -> "HOME"
        GamepadButtons.DPAD_UP -> "D↑"; GamepadButtons.DPAD_DOWN -> "D↓"
        GamepadButtons.DPAD_LEFT -> "D←"; GamepadButtons.DPAD_RIGHT -> "D→"
        else -> "0x${"%04X".format(button)}"
    }

    // ── Trigger controls (analog) ────────────────────────────────────────

    fun setLeftTrigger(normalized: Float) {
        val value = (normalized * TriggerDefaults.MAX).toInt()
            .coerceIn(TriggerDefaults.REST, TriggerDefaults.MAX)
        val prev = _gamepadState.value.leftTrigger
        _gamepadState.value = _gamepadState.value.withLeftTrigger(value)
        if (value == TriggerDefaults.REST && prev != TriggerDefaults.REST) {
            sendForced("RELEASE LT")
        } else {
            sendThrottled()
        }
    }

    fun setRightTrigger(normalized: Float) {
        val value = (normalized * TriggerDefaults.MAX).toInt()
            .coerceIn(TriggerDefaults.REST, TriggerDefaults.MAX)
        val prev = _gamepadState.value.rightTrigger
        _gamepadState.value = _gamepadState.value.withRightTrigger(value)
        if (value == TriggerDefaults.REST && prev != TriggerDefaults.REST) {
            sendForced("RELEASE RT")
        } else {
            sendThrottled()
        }
    }

    // ── Axis controls ───────────────────────────────────────────────────

    fun setLeftAxis(normalizedX: Float, normalizedY: Float) {
        val x = ((normalizedX + 1f) / 2f * AxisDefaults.MAX).toInt().coerceIn(AxisDefaults.MIN, AxisDefaults.MAX)
        val y = ((normalizedY + 1f) / 2f * AxisDefaults.MAX).toInt().coerceIn(AxisDefaults.MIN, AxisDefaults.MAX)
        _gamepadState.value = _gamepadState.value.withLeftAxis(x, y)
        sendThrottled()
    }

    fun setRightAxis(normalizedX: Float, normalizedY: Float) {
        val x = ((normalizedX + 1f) / 2f * AxisDefaults.MAX).toInt().coerceIn(AxisDefaults.MIN, AxisDefaults.MAX)
        val y = ((normalizedY + 1f) / 2f * AxisDefaults.MAX).toInt().coerceIn(AxisDefaults.MIN, AxisDefaults.MAX)
        _gamepadState.value = _gamepadState.value.withRightAxis(x, y)
        sendThrottled()
    }

    // ── Report dispatch ─────────────────────────────────────────────────

    private fun sendThrottled() {
        if (!_isConnected.value) return
        val report = GamepadReportBuilder.buildReport(_gamepadState.value)
        val sent = hidManager.sendReport(report, force = false)
        if (sent) _reportsSent.value = hidManager.sendCount
    }

    private fun sendForced(context: String) {
        if (!_isConnected.value) {
            addLog("  ⚠ NOT CONNECTED ($context)")
            return
        }
        val report = GamepadReportBuilder.buildReport(_gamepadState.value)
        val sent = hidManager.sendReport(report, force = true)
        _reportsSent.value = hidManager.sendCount
        if (sent) {
            addLog("  ✓ SENT_FORCED #${hidManager.sendCount} ($context) [${hidManager.statsString()}]")
        } else {
            addLog("  ✗ SEND_FAILED ($context) hex=[${GamepadReportBuilder.toHexString(report)}]")
        }
    }

    fun sendNeutralReport() {
        _gamepadState.value = _gamepadState.value.neutral()
        val report = GamepadReportBuilder.neutralReport()
        addLog("NEUTRAL hex=[${GamepadReportBuilder.toHexString(report)}] | ${hidManager.statsString()}")
        if (!_isConnected.value) { addLog("Not connected"); return }
        val sent = hidManager.sendReport(report, force = true)
        addLog("Neutral sent=$sent")
        _reportsSent.value = hidManager.sendCount
    }

    fun resetState() {
        _gamepadState.value = GamepadState()
        addLog("State reset | ${hidManager.statsString()}")
    }

    // ── Logging ─────────────────────────────────────────────────────────

    private fun addLog(message: String) {
        val timestamp = timeFormat.format(Date())
        val formatted = "[$timestamp] $message"
        _logMessages.value = (_logMessages.value + formatted).takeLast(150)
        Log.d(TAG, formatted)
    }

    companion object {
        private const val TAG = "GamepadDebug"
    }

    fun clearLog() { _logMessages.value = emptyList() }

    fun toggleHaptics(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setHapticsEnabled(enabled) }
    }

    fun toggleSound(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setSoundEnabled(enabled) }
    }

    fun setTriggerMode(mode: String) {
        viewModelScope.launch { settingsRepo.setTriggerMode(mode) }
    }

    fun toggleDebugLog(visible: Boolean) {
        viewModelScope.launch { settingsRepo.setDebugLogVisible(visible) }
    }

    override fun onCleared() {
        super.onCleared()
        ButtonSoundPlayer.release()
        hidManager.cleanup()
    }
}

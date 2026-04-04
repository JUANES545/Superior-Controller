package com.example.superiorcontroller.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import com.example.superiorcontroller.bluetooth.BluetoothHidManager
import com.example.superiorcontroller.hid.AxisDefaults
import com.example.superiorcontroller.hid.GamepadButtons
import com.example.superiorcontroller.hid.GamepadReportBuilder
import com.example.superiorcontroller.hid.GamepadState
import com.example.superiorcontroller.hid.HidDescriptor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        addLog("Descriptor mode: ${if (HidDescriptor.EXTENDED_BUTTONS) "12-button" else "8-button"}, report=${HidDescriptor.REPORT_SIZE}B")
    }

    // ── Lifecycle ───────────────────────────────────────────────────────

    fun initializeBluetooth() {
        val result = hidManager.initialize()
        _bluetoothAvailable.value = hidManager.isBluetoothAvailable() && hidManager.isBluetoothEnabled()
        if (!result) addLog("Bluetooth initialization failed")
    }

    fun registerHidApp() {
        if (!hidManager.isProxyReady()) {
            addLog("Proxy not ready yet — wait or re-initialize")
            return
        }
        hidManager.registerApp()
    }

    fun unregisterHidApp() { hidManager.unregisterApp() }

    fun getBondedDevices(): List<BluetoothDevice> = hidManager.getBondedDevices()

    fun connectToDevice(device: BluetoothDevice) { hidManager.connectToHost(device) }

    // ── Button controls ─────────────────────────────────────────────────

    fun pressButton(button: Int) {
        val prev = _gamepadState.value
        _gamepadState.value = if (GamepadButtons.isDpad(button)) {
            prev.withDpadPressed(button)
        } else {
            prev.withButtonPressed(button)
        }
        val state = _gamepadState.value
        val name = buttonName(button)
        val report = GamepadReportBuilder.buildReport(state)
        addLog("▶ PRESS $name mask=0x${"%04X".format(state.buttons)} ${GamepadReportBuilder.describeBytes(state)} hex=[${GamepadReportBuilder.toHexString(report)}]")
        sendReportWithLog("PRESS $name")
    }

    fun releaseButton(button: Int) {
        val prev = _gamepadState.value
        _gamepadState.value = if (GamepadButtons.isDpad(button)) {
            prev.withDpadReleased(button)
        } else {
            prev.withButtonReleased(button)
        }
        val state = _gamepadState.value
        val name = buttonName(button)
        val report = GamepadReportBuilder.buildReport(state)
        addLog("■ RELEASE $name mask=0x${"%04X".format(state.buttons)} hex=[${GamepadReportBuilder.toHexString(report)}]")
        sendReportWithLog("RELEASE $name")
    }

    private fun buttonName(button: Int): String = when (button) {
        GamepadButtons.A -> "A"
        GamepadButtons.B -> "B"
        GamepadButtons.X -> "X"
        GamepadButtons.Y -> "Y"
        GamepadButtons.LB -> "LB"
        GamepadButtons.RB -> "RB"
        GamepadButtons.BACK -> "BACK"
        GamepadButtons.START -> "START"
        GamepadButtons.LT -> "LT"
        GamepadButtons.RT -> "RT"
        GamepadButtons.L3 -> "L3"
        GamepadButtons.R3 -> "R3"
        GamepadButtons.DPAD_UP -> "D↑"
        GamepadButtons.DPAD_DOWN -> "D↓"
        GamepadButtons.DPAD_LEFT -> "D←"
        GamepadButtons.DPAD_RIGHT -> "D→"
        else -> "0x${"%04X".format(button)}"
    }

    // ── Axis controls ───────────────────────────────────────────────────

    fun setLeftAxis(normalizedX: Float, normalizedY: Float) {
        val x = ((normalizedX + 1f) / 2f * AxisDefaults.MAX).toInt()
            .coerceIn(AxisDefaults.MIN, AxisDefaults.MAX)
        val y = ((normalizedY + 1f) / 2f * AxisDefaults.MAX).toInt()
            .coerceIn(AxisDefaults.MIN, AxisDefaults.MAX)
        _gamepadState.value = _gamepadState.value.withLeftAxis(x, y)
        sendReport()
    }

    fun setRightAxis(normalizedX: Float, normalizedY: Float) {
        val x = ((normalizedX + 1f) / 2f * AxisDefaults.MAX).toInt()
            .coerceIn(AxisDefaults.MIN, AxisDefaults.MAX)
        val y = ((normalizedY + 1f) / 2f * AxisDefaults.MAX).toInt()
            .coerceIn(AxisDefaults.MIN, AxisDefaults.MAX)
        _gamepadState.value = _gamepadState.value.withRightAxis(x, y)
        sendReport()
    }

    // ── Report management ───────────────────────────────────────────────

    fun sendReport() {
        if (!_isConnected.value) return
        val report = GamepadReportBuilder.buildReport(_gamepadState.value)
        val sent = hidManager.sendReport(report)
        if (sent) _reportsSent.value++
    }

    private fun sendReportWithLog(context: String) {
        if (!_isConnected.value) {
            addLog("  ⚠ NOT CONNECTED — report not sent ($context)")
            return
        }
        val report = GamepadReportBuilder.buildReport(_gamepadState.value)
        val sent = hidManager.sendReport(report)
        if (sent) {
            _reportsSent.value++
            addLog("  ✓ SENT #${_reportsSent.value} ($context)")
        } else {
            addLog("  ✗ SEND FAILED ($context) hex=[${GamepadReportBuilder.toHexString(report)}]")
        }
    }

    fun sendNeutralReport() {
        _gamepadState.value = _gamepadState.value.neutral()
        val report = GamepadReportBuilder.neutralReport()
        addLog("NEUTRAL → hex=[${GamepadReportBuilder.toHexString(report)}]")
        if (!_isConnected.value) {
            addLog("Not connected — neutral state set locally only")
            return
        }
        val sent = hidManager.sendReport(report)
        addLog("Neutral report sent: $sent")
        if (sent) _reportsSent.value++
    }

    fun resetState() {
        _gamepadState.value = GamepadState()
        addLog("Gamepad state reset to neutral")
    }

    // ── Logging ─────────────────────────────────────────────────────────

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private fun addLog(message: String) {
        val timestamp = timeFormat.format(Date())
        _logMessages.value = (_logMessages.value + "[$timestamp] $message").takeLast(150)
    }

    fun clearLog() { _logMessages.value = emptyList() }

    override fun onCleared() {
        super.onCleared()
        hidManager.cleanup()
    }
}

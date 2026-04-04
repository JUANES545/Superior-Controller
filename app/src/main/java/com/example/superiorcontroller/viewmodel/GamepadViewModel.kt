package com.example.superiorcontroller.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import com.example.superiorcontroller.bluetooth.BluetoothHidManager
import com.example.superiorcontroller.hid.AxisDefaults
import com.example.superiorcontroller.hid.GamepadButtons
import com.example.superiorcontroller.hid.GamepadReportBuilder
import com.example.superiorcontroller.hid.GamepadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GamepadViewModel(application: Application) : AndroidViewModel(application) {

    // ── Observable state ────────────────────────────────────────────────
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

    // ── Bluetooth manager ───────────────────────────────────────────────
    private val hidManager = BluetoothHidManager(application.applicationContext)

    private val managerListener = object : BluetoothHidManager.Listener {
        override fun onProxyReady() {
            _proxyReady.value = true
        }

        override fun onRegistrationChanged(registered: Boolean) {
            _isRegistered.value = registered
        }

        override fun onConnectionChanged(device: BluetoothDevice?, connected: Boolean) {
            _isConnected.value = connected
            _connectedDeviceName.value = device?.name ?: device?.address
        }

        override fun onLog(message: String) {
            addLog(message)
        }
    }

    init {
        hidManager.listener = managerListener
    }

    // ── Lifecycle ───────────────────────────────────────────────────────

    fun initializeBluetooth() {
        val result = hidManager.initialize()
        _bluetoothAvailable.value = hidManager.isBluetoothAvailable() && hidManager.isBluetoothEnabled()
        if (!result) {
            addLog("Bluetooth initialization failed")
        }
    }

    fun registerHidApp() {
        if (!hidManager.isProxyReady()) {
            addLog("Proxy not ready yet — wait for callback or re-initialize")
            return
        }
        hidManager.registerApp()
    }

    fun unregisterHidApp() {
        hidManager.unregisterApp()
    }

    fun getBondedDevices(): List<BluetoothDevice> = hidManager.getBondedDevices()

    fun connectToDevice(device: BluetoothDevice) {
        hidManager.connectToHost(device)
    }

    // ── Button controls ─────────────────────────────────────────────────

    fun pressButton(button: Int) {
        _gamepadState.value = if (GamepadButtons.isDpad(button)) {
            _gamepadState.value.withDpadPressed(button)
        } else {
            _gamepadState.value.withButtonPressed(button)
        }
        addLog("PRESS 0x${"%04X".format(button)} → ${GamepadReportBuilder.describe(_gamepadState.value)}")
        sendReport()
    }

    fun releaseButton(button: Int) {
        _gamepadState.value = if (GamepadButtons.isDpad(button)) {
            _gamepadState.value.withDpadReleased(button)
        } else {
            _gamepadState.value.withButtonReleased(button)
        }
        sendReport()
    }

    // ── Axis controls ───────────────────────────────────────────────────

    fun setAxis(normalizedX: Float, normalizedY: Float) {
        val x = ((normalizedX + 1f) / 2f * AxisDefaults.MAX).toInt()
            .coerceIn(AxisDefaults.MIN, AxisDefaults.MAX)
        val y = ((normalizedY + 1f) / 2f * AxisDefaults.MAX).toInt()
            .coerceIn(AxisDefaults.MIN, AxisDefaults.MAX)
        _gamepadState.value = _gamepadState.value.withAxes(x, y)
        sendReport()
    }

    // ── Report management ───────────────────────────────────────────────

    fun sendReport() {
        if (!_isConnected.value) return
        val report = GamepadReportBuilder.buildReport(_gamepadState.value)
        val sent = hidManager.sendReport(report)
        if (sent) {
            _reportsSent.value++
        }
    }

    fun sendNeutralReport() {
        _gamepadState.value = _gamepadState.value.neutral()
        val report = GamepadReportBuilder.neutralReport()
        addLog("NEUTRAL → ${GamepadReportBuilder.toHexString(report)}")
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
        val entry = "[$timestamp] $message"
        _logMessages.value = (_logMessages.value + entry).takeLast(100)
    }

    fun clearLog() {
        _logMessages.value = emptyList()
    }

    // ── Cleanup ─────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        hidManager.cleanup()
    }
}

package com.example.superiorcontroller.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.superiorcontroller.bluetooth.BluetoothHidManager
import com.example.superiorcontroller.bluetooth.KnownDevice
import com.example.superiorcontroller.bluetooth.KnownDevicesRepository
import com.example.superiorcontroller.hid.AxisDefaults
import com.example.superiorcontroller.hid.GamepadButtons
import com.example.superiorcontroller.hid.GamepadReportBuilder
import com.example.superiorcontroller.hid.GamepadState
import com.example.superiorcontroller.hid.HidDescriptor
import com.example.superiorcontroller.hid.TriggerDefaults
import com.example.superiorcontroller.input.HardwareGamepadManager
import com.example.superiorcontroller.recording.GamepadSnapshot
import com.example.superiorcontroller.recording.InputRecorder
import com.example.superiorcontroller.recording.PlaybackEngine
import com.example.superiorcontroller.recording.PlaybackProgress
import com.example.superiorcontroller.recording.RecordingData
import com.example.superiorcontroller.recording.RecordingMeta
import com.example.superiorcontroller.recording.RecordingRepository
import com.example.superiorcontroller.settings.SettingsRepository
import com.example.superiorcontroller.ui.components.ButtonHaptics
import com.example.superiorcontroller.ui.components.ButtonSoundPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.SystemClock
import java.util.UUID

class GamepadViewModel(application: Application) : AndroidViewModel(application) {

    // ── Gamepad state ───────────────────────────────────────────────────

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

    // ── Settings ────────────────────────────────────────────────────────

    private val _hapticsEnabled = MutableStateFlow(true)
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _triggerMode = MutableStateFlow(SettingsRepository.MODE_ANALOG)
    val triggerMode: StateFlow<String> = _triggerMode.asStateFlow()

    private val _debugLogVisible = MutableStateFlow(true)
    val debugLogVisible: StateFlow<Boolean> = _debugLogVisible.asStateFlow()

    // ── Known devices / connection management ──────────────────────────

    private val _knownDevices = MutableStateFlow<List<KnownDevice>>(emptyList())
    val knownDevices: StateFlow<List<KnownDevice>> = _knownDevices.asStateFlow()

    private val _bondedDeviceInfo = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val bondedDeviceInfo: StateFlow<List<Pair<String, String>>> = _bondedDeviceInfo.asStateFlow()

    private var userDisconnected = false
    private var pendingRecovery = false

    // ── Hardware controller ─────────────────────────────────────────────

    private val _hwConnected = MutableStateFlow(false)
    val hwConnected: StateFlow<Boolean> = _hwConnected.asStateFlow()

    private val _hwDeviceName = MutableStateFlow<String?>(null)
    val hwDeviceName: StateFlow<String?> = _hwDeviceName.asStateFlow()

    // ── Recording / Playback ────────────────────────────────────────────

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingElapsedMs = MutableStateFlow(0L)
    val recordingElapsedMs: StateFlow<Long> = _recordingElapsedMs.asStateFlow()

    private val _recordings = MutableStateFlow<List<RecordingMeta>>(emptyList())
    val recordings: StateFlow<List<RecordingMeta>> = _recordings.asStateFlow()

    val playbackProgress: StateFlow<PlaybackProgress>
        get() = playbackEngine.progress

    // ── Infrastructure ──────────────────────────────────────────────────

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val settingsRepo = SettingsRepository(application.applicationContext)
    private val knownDevicesRepo = KnownDevicesRepository(application.applicationContext)
    private val hidManager = BluetoothHidManager(application.applicationContext)
    private val hwManager = HardwareGamepadManager(application.applicationContext)

    private val recorder = InputRecorder()
    private val playbackEngine = PlaybackEngine()
    private val recordingRepo = RecordingRepository(application.applicationContext)

    private var recordingTimerJob: Job? = null

    // ── Listeners ───────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private val managerListener = object : BluetoothHidManager.Listener {
        override fun onProxyReady() {
            _proxyReady.value = true
            if (pendingRecovery) {
                pendingRecovery = false
                recoverConnection()
            }
        }
        override fun onRegistrationChanged(registered: Boolean) {
            _isRegistered.value = registered
            if (registered && pendingRecovery) {
                pendingRecovery = false
                recoverConnection()
            }
        }
        override fun onConnectionChanged(device: BluetoothDevice?, connected: Boolean) {
            _isConnected.value = connected
            _connectedDeviceName.value = device?.name ?: device?.address
            if (connected && device != null) {
                userDisconnected = false
                val kd = KnownDevice(
                    name = device.name ?: device.address ?: "Unknown",
                    address = device.address ?: "",
                    lastUsedAt = System.currentTimeMillis()
                )
                viewModelScope.launch(Dispatchers.IO) {
                    knownDevicesRepo.save(kd)
                    _knownDevices.value = knownDevicesRepo.loadAll()
                }
            }
        }
        override fun onLog(message: String) { addLog(message) }
    }

    private val hwListener = object : HardwareGamepadManager.Listener {
        override fun onHwButtonDown(button: Int, name: String, eventTimeMs: Long) {
            pressButton(button, "[HW] ", hwEventTimeMs = eventTimeMs)
        }
        override fun onHwButtonUp(button: Int, name: String, eventTimeMs: Long) {
            releaseButton(button, "[HW] ", hwEventTimeMs = eventTimeMs)
        }
        override fun onHwLeftAxis(x: Float, y: Float, eventTimeMs: Long) {
            setLeftAxis(x, y, hwEventTimeMs = eventTimeMs)
        }
        override fun onHwRightAxis(x: Float, y: Float, eventTimeMs: Long) {
            setRightAxis(x, y, hwEventTimeMs = eventTimeMs)
        }
        override fun onHwLeftTrigger(value: Float, eventTimeMs: Long) {
            setLeftTrigger(value, hwEventTimeMs = eventTimeMs)
        }
        override fun onHwRightTrigger(value: Float, eventTimeMs: Long) {
            setRightTrigger(value, hwEventTimeMs = eventTimeMs)
        }
        override fun onHwDeviceConnected(name: String, vendorId: Int, productId: Int) {
            _hwConnected.value = true
            _hwDeviceName.value = name
            addLog("GAMEPAD_CONNECTED: $name (VID:${"%04X".format(vendorId)} PID:${"%04X".format(productId)})")
        }
        override fun onHwDeviceDisconnected(name: String) {
            _hwConnected.value = false
            _hwDeviceName.value = null
            addLog("GAMEPAD_DISCONNECTED: $name")
        }
    }

    private val playbackSink = object : PlaybackEngine.EventSink {
        override fun onPlaybackReset() {
            _gamepadState.value = GamepadState()
            if (_isConnected.value) {
                hidManager.sendReport(GamepadReportBuilder.neutralReport(), force = true)
            }
            addLog("PB_RESET: neutral")
        }
        override fun onPlaybackApplySnapshot(snapshot: GamepadSnapshot) {
            val lx = ((snapshot.leftX + 1f) / 2f * AxisDefaults.MAX).toInt()
                .coerceIn(AxisDefaults.MIN, AxisDefaults.MAX)
            val ly = ((snapshot.leftY + 1f) / 2f * AxisDefaults.MAX).toInt()
                .coerceIn(AxisDefaults.MIN, AxisDefaults.MAX)
            val rx = ((snapshot.rightX + 1f) / 2f * AxisDefaults.MAX).toInt()
                .coerceIn(AxisDefaults.MIN, AxisDefaults.MAX)
            val ry = ((snapshot.rightY + 1f) / 2f * AxisDefaults.MAX).toInt()
                .coerceIn(AxisDefaults.MIN, AxisDefaults.MAX)
            val lt = (snapshot.leftTrigger * TriggerDefaults.MAX).toInt()
                .coerceIn(TriggerDefaults.REST, TriggerDefaults.MAX)
            val rt = (snapshot.rightTrigger * TriggerDefaults.MAX).toInt()
                .coerceIn(TriggerDefaults.REST, TriggerDefaults.MAX)
            _gamepadState.value = GamepadState(
                buttons = snapshot.buttons,
                dpad = snapshot.dpad,
                leftX = lx, leftY = ly,
                rightX = rx, rightY = ry,
                leftTrigger = lt, rightTrigger = rt
            )
            if (_isConnected.value) {
                hidManager.sendReport(
                    GamepadReportBuilder.buildReport(_gamepadState.value), force = true
                )
            }
            addLog("PB_SNAPSHOT: applied")
        }
        override fun onPlaybackButtonPress(button: Int) { pressButton(button, "[PB] ") }
        override fun onPlaybackButtonRelease(button: Int) { releaseButton(button, "[PB] ") }
        override fun onPlaybackLeftAxis(x: Float, y: Float) { setLeftAxis(x, y, fromPlayback = true) }
        override fun onPlaybackRightAxis(x: Float, y: Float) { setRightAxis(x, y, fromPlayback = true) }
        override fun onPlaybackLeftTrigger(value: Float) { setLeftTrigger(value, fromPlayback = true) }
        override fun onPlaybackRightTrigger(value: Float) { setRightTrigger(value, fromPlayback = true) }
        override fun onPlaybackComplete() { addLog("PLAYBACK_COMPLETE") }
        override fun onPlaybackLog(message: String) { addLog(message) }
    }

    // ── Init ────────────────────────────────────────────────────────────

    init {
        hidManager.listener = managerListener
        hwManager.listener = hwListener
        hwManager.register()
        playbackEngine.sink = playbackSink

        _knownDevices.value = knownDevicesRepo.loadAll()

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
        viewModelScope.launch(Dispatchers.IO) {
            _recordings.value = recordingRepo.loadAll()
        }
    }

    // ── Lifecycle / Connection management ───────────────────────────────

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

    fun disconnectFromHost() {
        userDisconnected = true
        hidManager.disconnect()
        addLog("BT_DISCONNECT: user requested")
    }

    fun connectToKnownDevice(address: String) {
        userDisconnected = false
        if (!hidManager.isProxyReady()) { addLog("Proxy not ready for connect"); return }
        if (!hidManager.isRegistered) { addLog("Not registered, cannot connect"); return }
        if (hidManager.isConnected) { addLog("Already connected"); return }
        hidManager.connectToAddress(address)
    }

    fun removeKnownDevice(address: String) {
        viewModelScope.launch(Dispatchers.IO) {
            knownDevicesRepo.remove(address)
            _knownDevices.value = knownDevicesRepo.loadAll()
        }
    }

    fun refreshBondedDevices() {
        _bondedDeviceInfo.value = hidManager.getBondedDeviceInfo()
    }

    /**
     * Called from Activity.onResume. Syncs actual BT state with ViewModel
     * and auto-recovers the connection chain if it was lost while in background.
     */
    fun recoverConnection() {
        val btOk = hidManager.isBluetoothAvailable() && hidManager.isBluetoothEnabled()
        _bluetoothAvailable.value = btOk

        if (!btOk) {
            addLog("BT_RESUME: Bluetooth not available — reinitializing")
            pendingRecovery = !userDisconnected
            hidManager.initialize()
            return
        }

        if (!hidManager.isProxyReady()) {
            addLog("BT_RESUME: proxy lost — reinitializing")
            pendingRecovery = !userDisconnected
            hidManager.initialize()
            return
        }
        _proxyReady.value = true

        if (!hidManager.isRegistered) {
            addLog("BT_RESUME: registration lost — re-registering")
            pendingRecovery = !userDisconnected
            registerHidApp()
            return
        }
        _isRegistered.value = true

        _isConnected.value = hidManager.isConnected
        if (hidManager.isConnected) {
            addLog("BT_RESUME: still connected")
            return
        }

        if (!userDisconnected) {
            val lastDevice = knownDevicesRepo.getLastUsed()
            if (lastDevice != null) {
                addLog("BT_RESUME: reconnecting to ${lastDevice.name}")
                connectToKnownDevice(lastDevice.address)
            } else {
                addLog("BT_RESUME: registered, no known device to reconnect")
            }
        } else {
            addLog("BT_RESUME: user-disconnected, not reconnecting")
        }
    }

    // ── Hardware gamepad event forwarding ─────────────────────────────

    fun processHardwareKeyEvent(event: KeyEvent): Boolean = hwManager.processKeyEvent(event)
    fun processHardwareMotionEvent(event: MotionEvent): Boolean = hwManager.processMotionEvent(event)

    // ── Button controls ─────────────────────────────────────────────────

    fun pressButton(button: Int, tag: String = "", hwEventTimeMs: Long = 0L) {
        if (playbackEngine.isRunning && !tag.startsWith("[PB]")) return
        if (recorder.isRecording && !tag.startsWith("[PB]")) recorder.recordButtonPress(button, hwEventTimeMs)

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
        addLog("${tag}▶ PRESS $name mask=0x${"%04X".format(s.buttons)} ${GamepadReportBuilder.describeBytes(s)} hex=[${GamepadReportBuilder.toHexString(report)}]")
        sendForced("${tag}PRESS $name")
    }

    fun releaseButton(button: Int, tag: String = "", hwEventTimeMs: Long = 0L) {
        if (playbackEngine.isRunning && !tag.startsWith("[PB]")) return
        if (recorder.isRecording && !tag.startsWith("[PB]")) recorder.recordButtonRelease(button, hwEventTimeMs)

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
        addLog("${tag}■ RELEASE $name mask=0x${"%04X".format(s.buttons)} hex=[${GamepadReportBuilder.toHexString(GamepadReportBuilder.buildReport(s))}]")
        sendForced("${tag}RELEASE $name")
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

    // ── Trigger controls ────────────────────────────────────────────────

    fun setLeftTrigger(normalized: Float, hwEventTimeMs: Long = 0L, fromPlayback: Boolean = false) {
        if (playbackEngine.isRunning && !fromPlayback) return
        if (recorder.isRecording && !playbackEngine.isRunning) recorder.recordLeftTrigger(normalized, hwEventTimeMs)

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

    fun setRightTrigger(normalized: Float, hwEventTimeMs: Long = 0L, fromPlayback: Boolean = false) {
        if (playbackEngine.isRunning && !fromPlayback) return
        if (recorder.isRecording && !playbackEngine.isRunning) recorder.recordRightTrigger(normalized, hwEventTimeMs)

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

    fun setLeftAxis(normalizedX: Float, normalizedY: Float, hwEventTimeMs: Long = 0L, fromPlayback: Boolean = false) {
        if (playbackEngine.isRunning && !fromPlayback) return
        if (recorder.isRecording && !playbackEngine.isRunning) recorder.recordLeftAxis(normalizedX, normalizedY, hwEventTimeMs)

        val x = ((normalizedX + 1f) / 2f * AxisDefaults.MAX).toInt().coerceIn(AxisDefaults.MIN, AxisDefaults.MAX)
        val y = ((normalizedY + 1f) / 2f * AxisDefaults.MAX).toInt().coerceIn(AxisDefaults.MIN, AxisDefaults.MAX)
        _gamepadState.value = _gamepadState.value.withLeftAxis(x, y)
        sendThrottled()
    }

    fun setRightAxis(normalizedX: Float, normalizedY: Float, hwEventTimeMs: Long = 0L, fromPlayback: Boolean = false) {
        if (playbackEngine.isRunning && !fromPlayback) return
        if (recorder.isRecording && !playbackEngine.isRunning) recorder.recordRightAxis(normalizedX, normalizedY, hwEventTimeMs)

        val x = ((normalizedX + 1f) / 2f * AxisDefaults.MAX).toInt().coerceIn(AxisDefaults.MIN, AxisDefaults.MAX)
        val y = ((normalizedY + 1f) / 2f * AxisDefaults.MAX).toInt().coerceIn(AxisDefaults.MIN, AxisDefaults.MAX)
        _gamepadState.value = _gamepadState.value.withRightAxis(x, y)
        sendThrottled()
    }

    // ── Recording controls ──────────────────────────────────────────────

    fun startRecording() {
        if (recorder.isRecording || playbackEngine.isRunning) return
        val snap = captureSnapshot()
        recorder.start(snap, hwBaseMs = SystemClock.uptimeMillis())
        _isRecording.value = true
        _recordingElapsedMs.value = 0L
        recordingTimerJob = viewModelScope.launch {
            while (recorder.isRecording) {
                _recordingElapsedMs.value = recorder.elapsedMs()
                delay(100)
            }
        }
        addLog("REC_START snap=YES btn=0x${"%04X".format(snap.buttons)}")
    }

    private fun captureSnapshot(): GamepadSnapshot {
        val s = _gamepadState.value
        return GamepadSnapshot(
            buttons = s.buttons,
            dpad = s.dpad,
            leftX = s.leftX.toFloat() / AxisDefaults.MAX.toFloat() * 2f - 1f,
            leftY = s.leftY.toFloat() / AxisDefaults.MAX.toFloat() * 2f - 1f,
            rightX = s.rightX.toFloat() / AxisDefaults.MAX.toFloat() * 2f - 1f,
            rightY = s.rightY.toFloat() / AxisDefaults.MAX.toFloat() * 2f - 1f,
            leftTrigger = s.leftTrigger.toFloat() / TriggerDefaults.MAX.toFloat(),
            rightTrigger = s.rightTrigger.toFloat() / TriggerDefaults.MAX.toFloat()
        )
    }

    fun stopRecording() {
        if (!recorder.isRecording) return
        val stats = recorder.statsString()
        val snapshot = recorder.initialSnapshot
        val events = recorder.stop()
        _isRecording.value = false
        _recordingElapsedMs.value = 0L
        recordingTimerJob?.cancel()

        if (events.isEmpty()) {
            addLog("REC_STOP (empty, discarded) | $stats")
            return
        }

        val duration = events.last().t
        val count = _recordings.value.size + 1
        val id = UUID.randomUUID().toString()
        val data = RecordingData(
            id = id,
            name = "Recording $count",
            createdAt = System.currentTimeMillis(),
            durationMs = duration,
            events = events,
            initialSnapshot = snapshot
        )
        viewModelScope.launch(Dispatchers.IO) {
            recordingRepo.save(data)
            _recordings.value = recordingRepo.loadAll()
        }
        addLog("REC_STOP ${events.size} events, ${duration}ms | $stats")
    }

    // ── Playback controls ───────────────────────────────────────────────

    fun playRecording(id: String) {
        if (recorder.isRecording || playbackEngine.isRunning) return
        viewModelScope.launch {
            val data = withContext(Dispatchers.IO) { recordingRepo.load(id) }
            if (data == null) {
                addLog("PLAYBACK_FAIL: not found")
                return@launch
            }
            playbackEngine.play(data, viewModelScope)
            addLog("PLAYBACK_START: ${data.name} (${data.events.size} events, ${data.durationMs}ms)")
        }
    }

    fun pausePlayback() {
        playbackEngine.pause()
        addLog("PLAYBACK_PAUSE")
    }

    fun resumePlayback() {
        playbackEngine.resume()
        addLog("PLAYBACK_RESUME")
    }

    fun stopPlayback() {
        playbackEngine.stop()
        addLog("PLAYBACK_STOP")
    }

    // ── Recording management ────────────────────────────────────────────

    fun deleteRecording(id: String) {
        if (playbackEngine.progress.value.recordingId == id) playbackEngine.stop()
        viewModelScope.launch(Dispatchers.IO) {
            recordingRepo.delete(id)
            _recordings.value = recordingRepo.loadAll()
        }
    }

    fun renameRecording(id: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            recordingRepo.rename(id, newName)
            _recordings.value = recordingRepo.loadAll()
        }
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
            addLog("  -- NOT CONNECTED ($context)")
            return
        }
        val report = GamepadReportBuilder.buildReport(_gamepadState.value)
        val sent = hidManager.sendReport(report, force = true)
        _reportsSent.value = hidManager.sendCount
        if (sent) {
            addLog("  + SENT_FORCED #${hidManager.sendCount} ($context) [${hidManager.statsString()}]")
        } else {
            addLog("  x SEND_FAILED ($context) hex=[${GamepadReportBuilder.toHexString(report)}]")
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
        playbackEngine.stop()
        hwManager.unregister()
        ButtonSoundPlayer.release()
        hidManager.cleanup()
    }
}

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
import com.example.superiorcontroller.input.InputQuantizer
import com.example.superiorcontroller.recording.GamepadSnapshot
import com.example.superiorcontroller.recording.HidRecordingData
import com.example.superiorcontroller.recording.HidRecordingMeta
import com.example.superiorcontroller.recording.HidReportPlaybackEngine
import com.example.superiorcontroller.recording.HidReportRecorder
import com.example.superiorcontroller.recording.InputRecorder
import com.example.superiorcontroller.recording.PlaybackEngine
import com.example.superiorcontroller.recording.PlaybackProgress
import com.example.superiorcontroller.recording.PlaybackStatus
import com.example.superiorcontroller.recording.RecordingData
import com.example.superiorcontroller.recording.RecordingMeta
import com.example.superiorcontroller.recording.RecordingRepository
import com.example.superiorcontroller.service.HidForegroundService
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

enum class HwConnectionType { NONE, USB, BLUETOOTH }

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

    private val _connectedHostAddress = MutableStateFlow<String?>(null)
    val connectedHostAddress: StateFlow<String?> = _connectedHostAddress.asStateFlow()

    private val _logMessages = MutableStateFlow<List<String>>(emptyList())
    val logMessages: StateFlow<List<String>> = _logMessages.asStateFlow()

    private val _reportsSent = MutableStateFlow(0L)
    val reportsSent: StateFlow<Long> = _reportsSent.asStateFlow()

    // ── Settings ────────────────────────────────────────────────────────

    private val _controllerProfile = MutableStateFlow(SettingsRepository.PROFILE_XBOX)
    val controllerProfile: StateFlow<String> = _controllerProfile.asStateFlow()
    private val isPsProfile: Boolean get() = _controllerProfile.value == SettingsRepository.PROFILE_PLAYSTATION

    private val _hapticsEnabled = MutableStateFlow(true)
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()
    private val _hapticsIntensity = MutableStateFlow(SettingsRepository.HAPTICS_MEDIUM)
    val hapticsIntensity: StateFlow<String> = _hapticsIntensity.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()
    private val _soundStyle = MutableStateFlow(SettingsRepository.SOUND_SOFT)
    val soundStyle: StateFlow<String> = _soundStyle.asStateFlow()
    private val _soundVolume = MutableStateFlow(0.7f)
    val soundVolume: StateFlow<Float> = _soundVolume.asStateFlow()

    private val _triggerMode = MutableStateFlow(SettingsRepository.MODE_ANALOG)
    val triggerMode: StateFlow<String> = _triggerMode.asStateFlow()

    private val _debugLogVisible = MutableStateFlow(false)
    val debugLogVisible: StateFlow<Boolean> = _debugLogVisible.asStateFlow()

    private val _debugLogOverlay = MutableStateFlow(false)
    val debugLogOverlay: StateFlow<Boolean> = _debugLogOverlay.asStateFlow()

    private val _digitalRecording = MutableStateFlow(false)
    val digitalRecording: StateFlow<Boolean> = _digitalRecording.asStateFlow()

    private val _macroWarningSuppressed = MutableStateFlow(false)

    private val _assistLeftMode = MutableStateFlow(SettingsRepository.ASSIST_8DIR)
    val assistLeftMode: StateFlow<String> = _assistLeftMode.asStateFlow()
    private val _assistRightMode = MutableStateFlow(SettingsRepository.ASSIST_STABLE75)
    val assistRightMode: StateFlow<String> = _assistRightMode.asStateFlow()
    private val _assistLeftTempo = MutableStateFlow(SettingsRepository.TEMPO_GRID)
    val assistLeftTempo: StateFlow<String> = _assistLeftTempo.asStateFlow()
    private val _assistRightTempo = MutableStateFlow(SettingsRepository.TEMPO_PULSE)
    val assistRightTempo: StateFlow<String> = _assistRightTempo.asStateFlow()

    private val temporalQuantizer = com.example.superiorcontroller.input.TemporalQuantizer(viewModelScope).also { tq ->
        tq.onCommitLeft = { x, y -> applyLeftAxis(x, y) }
        tq.onCommitRight = { x, y -> applyRightAxis(x, y) }
    }

    private val _profileWarningSuppressed = MutableStateFlow(false)
    val profileWarningSuppressed: StateFlow<Boolean> = _profileWarningSuppressed.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(true)
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _showMacroWarning = MutableStateFlow(false)
    val showMacroWarning: StateFlow<Boolean> = _showMacroWarning.asStateFlow()

    data class ProfileSuggestion(
        val address: String,
        val deviceName: String,
        val suggestedProfile: String
    )
    private val _profileSuggestion = MutableStateFlow<ProfileSuggestion?>(null)
    val profileSuggestion: StateFlow<ProfileSuggestion?> = _profileSuggestion.asStateFlow()

    // ── Known devices / connection management ──────────────────────────

    private val _knownDevices = MutableStateFlow<List<KnownDevice>>(emptyList())
    val knownDevices: StateFlow<List<KnownDevice>> = _knownDevices.asStateFlow()

    private val _bondedDeviceInfo = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val bondedDeviceInfo: StateFlow<List<Pair<String, String>>> = _bondedDeviceInfo.asStateFlow()

    private var userDisconnected = false
    private var pendingConnectAfterRegister: String? = null

    // ── Hardware controller ─────────────────────────────────────────────

    private val _hwConnected = MutableStateFlow(false)
    val hwConnected: StateFlow<Boolean> = _hwConnected.asStateFlow()

    private val _hwDeviceName = MutableStateFlow<String?>(null)
    val hwDeviceName: StateFlow<String?> = _hwDeviceName.asStateFlow()

    private val _hwConnectionType = MutableStateFlow(HwConnectionType.NONE)
    val hwConnectionType: StateFlow<HwConnectionType> = _hwConnectionType.asStateFlow()

    // ── BT warning dialog state ────────────────────────────────────────

    private val _showBtWarning = MutableStateFlow(false)
    val showBtWarning: StateFlow<Boolean> = _showBtWarning.asStateFlow()
    private var pendingDeviceAfterWarning: String? = null

    // ── Foreground Service state ───────────────────────────────────────

    private var hidServiceRunning = false

    // ── Recording / Playback ────────────────────────────────────────────

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingElapsedMs = MutableStateFlow(0L)
    val recordingElapsedMs: StateFlow<Long> = _recordingElapsedMs.asStateFlow()

    private val _recordings = MutableStateFlow<List<RecordingMeta>>(emptyList())
    val recordings: StateFlow<List<RecordingMeta>> = _recordings.asStateFlow()

    val playbackProgress: StateFlow<PlaybackProgress>
        get() {
            val hidProg = hidPlaybackEngine.progress.value
            return if (hidProg.status != PlaybackStatus.IDLE) hidPlaybackEngine.progress
            else playbackEngine.progress
        }

    private val _hidRecordings = MutableStateFlow<List<HidRecordingMeta>>(emptyList())
    val hidRecordings: StateFlow<List<HidRecordingMeta>> = _hidRecordings.asStateFlow()

    // ── Infrastructure ──────────────────────────────────────────────────

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val settingsRepo = SettingsRepository(application.applicationContext)
    private val knownDevicesRepo = KnownDevicesRepository(application.applicationContext)
    val hidManager = BluetoothHidManager(application.applicationContext)
    private val hwManager = HardwareGamepadManager(application.applicationContext)

    private val recorder = InputRecorder()
    private val playbackEngine = PlaybackEngine()
    private val hidReportRecorder = HidReportRecorder()
    private val hidPlaybackEngine = HidReportPlaybackEngine()
    private val recordingRepo = RecordingRepository(application.applicationContext)

    private var recordingTimerJob: Job? = null

    // ── Lifecycle state (for diagnostic logging) ──────────────────────
    var currentLifecycleState: String = "INIT"

    fun diagnosticState(): String =
        "lifecycle=$currentLifecycleState bt=${_bluetoothAvailable.value} " +
        "proxy=${_proxyReady.value} reg=${_isRegistered.value} " +
        "conn=${_isConnected.value} host=${_connectedDeviceName.value} " +
        "hostAddr=${_connectedHostAddress.value} " +
        "hw=${_hwConnected.value} hwName=${_hwDeviceName.value} hwType=${_hwConnectionType.value} " +
        "svc=$hidServiceRunning | mgr: ${hidManager.stateSnapshot()}"

    // ── Listeners ───────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private val managerListener = object : BluetoothHidManager.Listener {
        override fun onProxyReady() {
            _proxyReady.value = true
            addLog(
                "PROXY_READY: acquired (NO auto-register — lazy mode). " +
                "reg=${hidManager.isRegistered} | ${diagnosticState()}"
            )
        }
        override fun onRegistrationChanged(registered: Boolean) {
            val prev = _isRegistered.value
            _isRegistered.value = registered
            addLog("VM_REG_CHANGED: registered=$registered prev=$prev lifecycle=$currentLifecycleState hw=${_hwConnected.value}")

            if (registered) {
                startHidService()
                val pending = pendingConnectAfterRegister
                if (pending != null) {
                    pendingConnectAfterRegister = null
                    addLog("LAZY_CONNECT: registration confirmed → connecting to $pending")
                    hidManager.switchToHost(pending)
                }
            } else {
                if (pendingConnectAfterRegister != null) {
                    addLog("LAZY_CONNECT_CANCELLED: registration lost, clearing pending=$pendingConnectAfterRegister")
                    pendingConnectAfterRegister = null
                }
                if (!_isConnected.value) {
                    stopHidService()
                }
            }
        }
        override fun onConnectionChanged(device: BluetoothDevice?, connected: Boolean) {
            val prevConn = _isConnected.value
            _isConnected.value = connected
            _connectedHostAddress.value = if (connected) device?.address else null
            addLog(
                "VM_CONN_CHANGED: connected=$connected prev=$prevConn " +
                "device=${device?.name}/${device?.address} lifecycle=$currentLifecycleState"
            )
            if (connected && device != null) {
                userDisconnected = false
                val address = device.address ?: ""
                val btName = device.name ?: address
                val kd = KnownDevice(
                    name = btName,
                    address = address,
                    lastUsedAt = System.currentTimeMillis(),
                    lastProfile = _controllerProfile.value
                )
                viewModelScope.launch(Dispatchers.IO) {
                    knownDevicesRepo.save(kd)
                    val refreshed = knownDevicesRepo.loadAll()
                    _knownDevices.value = refreshed
                    val saved = refreshed.find { it.address == address }
                    _connectedDeviceName.value = saved?.displayName ?: btName
                }
                updateHidServiceNotification()
            } else {
                _connectedDeviceName.value = null
                if (!_isRegistered.value) {
                    stopHidService()
                } else {
                    updateHidServiceNotification()
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
            _hwConnectionType.value = detectConnectionType(name)
            addLog(
                "GAMEPAD_CONNECTED: $name (VID:${"%04X".format(vendorId)} PID:${"%04X".format(productId)}) " +
                "type=${_hwConnectionType.value} " +
                "lifecycle=$currentLifecycleState reg=${_isRegistered.value} conn=${_isConnected.value}"
            )
        }
        override fun onHwDeviceDisconnected(name: String) {
            val prevName = _hwDeviceName.value
            _hwConnected.value = false
            _hwDeviceName.value = null
            _hwConnectionType.value = HwConnectionType.NONE
            addLog(
                "GAMEPAD_DISCONNECTED: $name (was=$prevName) " +
                "lifecycle=$currentLifecycleState reg=${_isRegistered.value} conn=${_isConnected.value} " +
                "| mgr: ${hidManager.stateSnapshot()}"
            )
        }
    }

    private val playbackSink = object : PlaybackEngine.EventSink {
        override fun onPlaybackReset() {
            _gamepadState.value = GamepadState()
            if (_isConnected.value) {
                hidManager.sendReport(GamepadReportBuilder.neutralReport(isPsProfile), force = true)
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
                    GamepadReportBuilder.buildReport(_gamepadState.value, isPsProfile), force = true
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

        hidPlaybackEngine.sender = HidReportPlaybackEngine.ReportSender { report ->
            hidManager.sendRawReport(report)
        }
        hidPlaybackEngine.completionListener = HidReportPlaybackEngine.CompletionListener {
            viewModelScope.launch(Dispatchers.Main) {
                val s = hidPlaybackEngine.stats.value
                addLog("HID_PLAYBACK_COMPLETE: ${s.summary()}")
                _gamepadState.value = GamepadState()
            }
        }
        hidPlaybackEngine.uiUpdateListener = HidReportPlaybackEngine.UiUpdateListener { report, _ ->
            updateGamepadStateFromReport(report)
        }

        _knownDevices.value = knownDevicesRepo.loadAll()

        addLog("Mode: 11btn+2trig | report=${HidDescriptor.REPORT_SIZE}B | throttle=${BluetoothHidManager.MIN_INTERVAL_MS}ms")

        viewModelScope.launch {
            settingsRepo.controllerProfile.collect { profile ->
                val prev = _controllerProfile.value
                _controllerProfile.value = profile
                if (prev != profile && _isRegistered.value) {
                    addLog("PROFILE_CHANGE: $prev → $profile, re-registering HID")
                    hidManager.unregisterApp()
                    hidManager.registerApp(reason = "PROFILE_CHANGE", profile = profile)
                } else {
                    addLog("Settings: controllerProfile=$profile")
                }
            }
        }
        ButtonHaptics.init(getApplication())
        ButtonSoundPlayer.init()
        viewModelScope.launch {
            settingsRepo.hapticsEnabled.collect { enabled ->
                _hapticsEnabled.value = enabled
                ButtonHaptics.enabled = enabled
                addLog("Settings: haptics=${if (enabled) "ON" else "OFF"}")
            }
        }
        viewModelScope.launch {
            settingsRepo.hapticsIntensity.collect { intensity ->
                _hapticsIntensity.value = intensity
                ButtonHaptics.intensity = intensity
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
            settingsRepo.soundStyle.collect { style ->
                _soundStyle.value = style
                ButtonSoundPlayer.style = style
            }
        }
        viewModelScope.launch {
            settingsRepo.soundVolume.collect { vol ->
                _soundVolume.value = vol
                ButtonSoundPlayer.volume = vol
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
        viewModelScope.launch {
            settingsRepo.debugLogOverlay.collect { overlay ->
                _debugLogOverlay.value = overlay
            }
        }
        viewModelScope.launch {
            settingsRepo.digitalRecording.collect { enabled ->
                _digitalRecording.value = enabled
                addLog("Settings: digitalRecording=${if (enabled) "ON" else "OFF"}")
            }
        }
        viewModelScope.launch {
            settingsRepo.macroWarningSuppressed.collect { suppressed ->
                _macroWarningSuppressed.value = suppressed
            }
        }
        viewModelScope.launch {
            settingsRepo.assistLeftMode.collect { mode -> _assistLeftMode.value = mode }
        }
        viewModelScope.launch {
            settingsRepo.assistRightMode.collect { mode -> _assistRightMode.value = mode }
        }
        viewModelScope.launch {
            settingsRepo.assistLeftTempo.collect { mode -> _assistLeftTempo.value = mode }
        }
        viewModelScope.launch {
            settingsRepo.assistRightTempo.collect { mode -> _assistRightTempo.value = mode }
        }
        viewModelScope.launch {
            settingsRepo.profileWarningSuppressed.collect { _profileWarningSuppressed.value = it }
        }
        viewModelScope.launch {
            settingsRepo.onboardingCompleted.collect { _onboardingCompleted.value = it }
        }
        viewModelScope.launch(Dispatchers.IO) {
            _recordings.value = recordingRepo.loadAll()
            _hidRecordings.value = recordingRepo.loadAllHid()
        }
    }

    // ── Foreground Service management ───────────────────────────────────

    private fun startHidService() {
        if (hidServiceRunning) {
            updateHidServiceNotification()
            return
        }
        hidServiceRunning = true
        val hostName = _connectedDeviceName.value
        val registered = _isRegistered.value
        addLog("SERVICE_START: host=$hostName reg=$registered")
        HidForegroundService.start(getApplication(), hostName, registered)
    }

    private fun updateHidServiceNotification() {
        if (!hidServiceRunning) return
        val hostName = _connectedDeviceName.value
        val registered = _isRegistered.value
        HidForegroundService.update(getApplication(), hostName, registered)
    }

    private fun stopHidService() {
        if (!hidServiceRunning) return
        hidServiceRunning = false
        addLog("SERVICE_STOP")
        HidForegroundService.stop(getApplication())
    }

    // ── HW connection type heuristic ───────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun detectConnectionType(hwName: String): HwConnectionType {
        val bondedNames = try {
            hidManager.getBondedDevices().mapNotNull { it.name }
        } catch (_: SecurityException) {
            emptyList()
        }
        return if (bondedNames.any { it.equals(hwName, ignoreCase = true) }) {
            HwConnectionType.BLUETOOTH
        } else {
            HwConnectionType.USB
        }
    }

    // ── BT warning dialog flow ─────────────────────────────────────────

    fun confirmBtWarning() {
        _showBtWarning.value = false
        val address = pendingDeviceAfterWarning ?: return
        pendingDeviceAfterWarning = null
        proceedWithConnection(address)
    }

    fun dismissBtWarning() {
        _showBtWarning.value = false
        pendingDeviceAfterWarning = null
    }

    private fun proceedWithConnection(address: String) {
        userDisconnected = false
        if (!hidManager.isRegistered) {
            addLog("PROCEED_LAZY: registering first, then connecting to $address")
            pendingConnectAfterRegister = address
            hidManager.registerApp(reason = "LAZY_REGISTER_FOR_CONNECT", profile = _controllerProfile.value)
            return
        }
        addLog("PROCEED_SWITCH: switching to $address | ${diagnosticState()}")
        hidManager.switchToHost(address)
    }

    // ── Lifecycle / Connection management ───────────────────────────────

    fun initializeBluetooth() {
        addLog("INIT_BT: called lifecycle=$currentLifecycleState")
        val result = hidManager.initialize(reason = "APP_INIT")
        _bluetoothAvailable.value = hidManager.isBluetoothAvailable() && hidManager.isBluetoothEnabled()
        if (!result) addLog("INIT_BT_FAIL: initialization failed | ${diagnosticState()}")
    }

    fun registerHidApp() {
        if (!hidManager.isProxyReady()) { addLog("MANUAL_REGISTER_FAIL: proxy not ready"); return }
        addLog("MANUAL_REGISTER: user requested (hw=${_hwConnected.value}) | ${diagnosticState()}")
        hidManager.registerApp(reason = "USER_MANUAL_REGISTER", profile = _controllerProfile.value)
    }

    fun unregisterHidApp() {
        addLog("MANUAL_UNREGISTER: user requested | ${diagnosticState()}")
        hidManager.unregisterApp(reason = "USER_MANUAL_UNREGISTER")
        stopHidService()
    }

    fun getBondedDevices(): List<BluetoothDevice> = hidManager.getBondedDevices()
    fun connectToDevice(device: BluetoothDevice) {
        hidManager.connectToHost(device, reason = "USER_CONNECT_DEVICE")
    }

    fun disconnectFromHost() {
        userDisconnected = true
        addLog("USER_DISCONNECT: requested | ${diagnosticState()}")
        hidManager.disconnect(reason = "USER_DISCONNECT")
    }

    fun connectToKnownDevice(address: String) {
        userDisconnected = false
        if (!hidManager.isProxyReady()) { addLog("KNOWN_CONNECT_FAIL: proxy not ready"); return }
        if (hidManager.isConnected) { addLog("KNOWN_CONNECT_SKIP: already connected"); return }
        if (!hidManager.isRegistered) {
            addLog("KNOWN_CONNECT_LAZY: not registered — registering first for $address")
            pendingConnectAfterRegister = address
            hidManager.registerApp(reason = "LAZY_REGISTER_FOR_KNOWN_CONNECT", profile = _controllerProfile.value)
            return
        }
        hidManager.connectToAddress(address, reason = "USER_CONNECT_KNOWN")
    }

    fun switchToDevice(address: String) {
        userDisconnected = false
        if (!hidManager.isProxyReady()) { addLog("SWITCH_FAIL: proxy not ready"); return }

        if (!hidManager.isRegistered && _hwConnectionType.value == HwConnectionType.BLUETOOTH) {
            addLog("BT_WARNING: hw controller is Bluetooth, showing warning before HID registration")
            pendingDeviceAfterWarning = address
            _showBtWarning.value = true
            return
        }

        val known = _knownDevices.value.find { it.address == address }
        val savedProfile = known?.lastProfile.orEmpty()
        if (savedProfile.isNotBlank() && savedProfile != _controllerProfile.value) {
            _profileSuggestion.value = ProfileSuggestion(
                address = address,
                deviceName = known?.displayName ?: address,
                suggestedProfile = savedProfile
            )
            return
        }

        proceedWithConnection(address)
    }

    fun acceptProfileSuggestion() {
        val suggestion = _profileSuggestion.value ?: return
        _profileSuggestion.value = null
        viewModelScope.launch {
            settingsRepo.setControllerProfile(suggestion.suggestedProfile)
            if (hidManager.isRegistered) {
                addLog("PROFILE_SWITCH: re-registering with profile=${suggestion.suggestedProfile}")
                hidManager.unregisterApp(reason = "PROFILE_SWITCH")
            }
            proceedWithConnection(suggestion.address)
        }
    }

    fun declineProfileSuggestion() {
        val suggestion = _profileSuggestion.value ?: return
        _profileSuggestion.value = null
        proceedWithConnection(suggestion.address)
    }

    fun removeKnownDevice(address: String) {
        viewModelScope.launch(Dispatchers.IO) {
            knownDevicesRepo.remove(address)
            _knownDevices.value = knownDevicesRepo.loadAll()
        }
    }

    fun renameKnownDevice(address: String, alias: String) {
        viewModelScope.launch(Dispatchers.IO) {
            knownDevicesRepo.rename(address, alias)
            _knownDevices.value = knownDevicesRepo.loadAll()
            if (_connectedHostAddress.value == address) {
                val device = _knownDevices.value.find { it.address == address }
                _connectedDeviceName.value = device?.displayName
            }
        }
    }

    fun refreshBondedDevices() {
        _bondedDeviceInfo.value = hidManager.getHostCandidates()
    }

    fun recoverConnection() {
        val btAvailable = hidManager.isBluetoothAvailable() && hidManager.isBluetoothEnabled()
        val prevBt = _bluetoothAvailable.value
        val prevProxy = _proxyReady.value
        val prevReg = _isRegistered.value
        val prevConn = _isConnected.value

        _bluetoothAvailable.value = btAvailable
        _proxyReady.value = hidManager.isProxyReady()
        _isRegistered.value = hidManager.isRegistered
        _isConnected.value = hidManager.isConnected

        addLog(
            "RECOVER: prevBt=$prevBt→$btAvailable prevProxy=$prevProxy→${hidManager.isProxyReady()} " +
            "prevReg=$prevReg→${hidManager.isRegistered} prevConn=$prevConn→${hidManager.isConnected} " +
            "hw=${_hwConnected.value} svc=$hidServiceRunning | mgr: ${hidManager.stateSnapshot()}"
        )

        if (!btAvailable || !hidManager.isProxyReady()) {
            addLog("RECOVER_ACTION: reinitializing proxy (reason=ACTIVITY_RESUME_NO_PROXY)")
            hidManager.initialize(reason = "ACTIVITY_RESUME_NO_PROXY")
        } else if (!hidManager.isRegistered && prevReg) {
            addLog(
                "RECOVER_INFO: registration was lost during background. " +
                "NOT re-registering to preserve external BT connections."
            )
            _connectedDeviceName.value = null
            _connectedHostAddress.value = null
            stopHidService()
        } else {
            addLog("RECOVER_ACTION: state synced — no action needed")
        }
    }

    // ── Hardware gamepad event forwarding ─────────────────────────────

    fun processHardwareKeyEvent(event: KeyEvent): Boolean = hwManager.processKeyEvent(event)
    fun processHardwareMotionEvent(event: MotionEvent): Boolean = hwManager.processMotionEvent(event)

    // ── Button controls ─────────────────────────────────────────────────

    fun pressButton(button: Int, tag: String = "", hwEventTimeMs: Long = 0L) {
        if (isAnyPlaybackPlaying && !tag.startsWith("[PB]")) return
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
        val report = GamepadReportBuilder.buildReport(s, isPsProfile)
        addLog("${tag}▶ PRESS $name mask=0x${"%04X".format(s.buttons)} ${GamepadReportBuilder.describeBytes(s)} hex=[${GamepadReportBuilder.toHexString(report)}]")
        sendForced("${tag}PRESS $name")
    }

    fun releaseButton(button: Int, tag: String = "", hwEventTimeMs: Long = 0L) {
        if (isAnyPlaybackPlaying && !tag.startsWith("[PB]")) return
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
        addLog("${tag}■ RELEASE $name mask=0x${"%04X".format(s.buttons)} hex=[${GamepadReportBuilder.toHexString(GamepadReportBuilder.buildReport(s, isPsProfile))}]")
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
        if (isAnyPlaybackPlaying && !fromPlayback) return
        val q = if (shouldQuantize && !fromPlayback) InputQuantizer.quantizeTrigger(normalized) else normalized
        if (recorder.isRecording && !playbackEngine.isRunning) recorder.recordLeftTrigger(q, hwEventTimeMs)

        val value = (q * TriggerDefaults.MAX).toInt()
            .coerceIn(TriggerDefaults.REST, TriggerDefaults.MAX)
        val prev = _gamepadState.value.leftTrigger
        _gamepadState.value = _gamepadState.value.withLeftTrigger(value)
        if (fromPlayback || (value == TriggerDefaults.REST && prev != TriggerDefaults.REST)) {
            sendForced(if (fromPlayback) "[PB] LT" else "RELEASE LT")
        } else {
            sendThrottled()
        }
    }

    fun setRightTrigger(normalized: Float, hwEventTimeMs: Long = 0L, fromPlayback: Boolean = false) {
        if (isAnyPlaybackPlaying && !fromPlayback) return
        val q = if (shouldQuantize && !fromPlayback) InputQuantizer.quantizeTrigger(normalized) else normalized
        if (recorder.isRecording && !playbackEngine.isRunning) recorder.recordRightTrigger(q, hwEventTimeMs)

        val value = (q * TriggerDefaults.MAX).toInt()
            .coerceIn(TriggerDefaults.REST, TriggerDefaults.MAX)
        val prev = _gamepadState.value.rightTrigger
        _gamepadState.value = _gamepadState.value.withRightTrigger(value)
        if (fromPlayback || (value == TriggerDefaults.REST && prev != TriggerDefaults.REST)) {
            sendForced(if (fromPlayback) "[PB] RT" else "RELEASE RT")
        } else {
            sendThrottled()
        }
    }

    // ── Axis controls ───────────────────────────────────────────────────

    fun setLeftAxis(normalizedX: Float, normalizedY: Float, hwEventTimeMs: Long = 0L, fromPlayback: Boolean = false) {
        if (isAnyPlaybackPlaying && !fromPlayback) return
        val (qx, qy) = if (shouldQuantize && !fromPlayback)
            InputQuantizer.quantizeStick(normalizedX, normalizedY, _assistLeftMode.value)
        else normalizedX to normalizedY

        if (shouldQuantize && !fromPlayback && temporalQuantizer.isActive
            && temporalQuantizer.leftMode != com.example.superiorcontroller.input.TemporalQuantizer.MODE_FREE) {
            temporalQuantizer.feedLeft(qx, qy)
            return
        }
        applyLeftAxis(qx, qy, fromPlayback)
    }

    private fun applyLeftAxis(qx: Float, qy: Float, fromPlayback: Boolean = false) {
        if (recorder.isRecording && !playbackEngine.isRunning) recorder.recordLeftAxis(qx, qy, 0L)
        val x = ((qx + 1f) / 2f * AxisDefaults.MAX).toInt().coerceIn(AxisDefaults.MIN, AxisDefaults.MAX)
        val y = ((qy + 1f) / 2f * AxisDefaults.MAX).toInt().coerceIn(AxisDefaults.MIN, AxisDefaults.MAX)
        _gamepadState.value = _gamepadState.value.withLeftAxis(x, y)
        if (fromPlayback) sendForced("[PB] L-Axis") else sendThrottled()
    }

    fun setRightAxis(normalizedX: Float, normalizedY: Float, hwEventTimeMs: Long = 0L, fromPlayback: Boolean = false) {
        if (isAnyPlaybackPlaying && !fromPlayback) return
        val (qx, qy) = if (shouldQuantize && !fromPlayback)
            InputQuantizer.quantizeStick(normalizedX, normalizedY, _assistRightMode.value)
        else normalizedX to normalizedY

        if (shouldQuantize && !fromPlayback && temporalQuantizer.isActive
            && temporalQuantizer.rightMode != com.example.superiorcontroller.input.TemporalQuantizer.MODE_FREE) {
            temporalQuantizer.feedRight(qx, qy)
            return
        }
        applyRightAxis(qx, qy, fromPlayback)
    }

    private fun applyRightAxis(qx: Float, qy: Float, fromPlayback: Boolean = false) {
        if (recorder.isRecording && !playbackEngine.isRunning) recorder.recordRightAxis(qx, qy, 0L)
        val x = ((qx + 1f) / 2f * AxisDefaults.MAX).toInt().coerceIn(AxisDefaults.MIN, AxisDefaults.MAX)
        val y = ((qy + 1f) / 2f * AxisDefaults.MAX).toInt().coerceIn(AxisDefaults.MIN, AxisDefaults.MAX)
        _gamepadState.value = _gamepadState.value.withRightAxis(x, y)
        if (fromPlayback) sendForced("[PB] R-Axis") else sendThrottled()
    }

    // ── Recording controls (HID raw report capture) ────────────────────

    fun startRecording() {
        if (hidReportRecorder.isRecording || isAnyPlaybackRunning()) return
        if (!_isConnected.value) {
            addLog("REC_FAIL: not connected — HID recording requires active connection")
            return
        }
        if (_digitalRecording.value && !_macroWarningSuppressed.value) {
            _showMacroWarning.value = true
            return
        }
        doStartRecording()
    }

    fun confirmMacroWarning(dontShowAgain: Boolean) {
        _showMacroWarning.value = false
        if (dontShowAgain) {
            viewModelScope.launch { settingsRepo.setMacroWarningSuppressed(true) }
        }
        doStartRecording()
    }

    fun dismissMacroWarning() {
        _showMacroWarning.value = false
    }

    private fun doStartRecording() {
        if (hidReportRecorder.isRecording || !_isConnected.value) return
        hidReportRecorder.start()
        hidManager.reportCaptureListener = { report, sendTimeNs ->
            hidReportRecorder.captureFrame(report, sendTimeNs)
        }
        if (_digitalRecording.value) {
            temporalQuantizer.leftMode = _assistLeftTempo.value
            temporalQuantizer.rightMode = _assistRightTempo.value
            val needsTemporal = temporalQuantizer.leftMode != com.example.superiorcontroller.input.TemporalQuantizer.MODE_FREE
                    || temporalQuantizer.rightMode != com.example.superiorcontroller.input.TemporalQuantizer.MODE_FREE
            if (needsTemporal) temporalQuantizer.start()
        }
        _isRecording.value = true
        _recordingElapsedMs.value = 0L
        recordingTimerJob = viewModelScope.launch {
            while (hidReportRecorder.isRecording) {
                _recordingElapsedMs.value = hidReportRecorder.elapsedMs()
                delay(100)
            }
        }
        val digi = if (_digitalRecording.value) " DIGITAL_MODE" else ""
        val tempo = if (temporalQuantizer.isActive) " tempo:L=${temporalQuantizer.leftMode},R=${temporalQuantizer.rightMode}" else ""
        addLog("HID_REC_START: capturing raw HID reports (profile=${_controllerProfile.value}$digi$tempo)")
    }

    fun stopRecording() {
        if (!hidReportRecorder.isRecording) return
        temporalQuantizer.stop()
        hidManager.reportCaptureListener = null
        val frames = hidReportRecorder.stop()
        _isRecording.value = false
        _recordingElapsedMs.value = 0L
        recordingTimerJob?.cancel()

        if (frames.isEmpty()) {
            addLog("HID_REC_STOP: (empty, discarded)")
            return
        }

        val durationMs = frames.last().relativeNs / 1_000_000L
        val count = _hidRecordings.value.size + 1
        val id = UUID.randomUUID().toString()
        val data = HidRecordingData(
            id = id,
            name = "HID Recording $count",
            createdAt = System.currentTimeMillis(),
            durationMs = durationMs,
            frameCount = frames.size,
            frames = frames,
            profileUsed = _controllerProfile.value
        )
        viewModelScope.launch(Dispatchers.IO) {
            recordingRepo.saveHid(data)
            _hidRecordings.value = recordingRepo.loadAllHid()
        }
        addLog("HID_REC_STOP: ${frames.size} frames, ${durationMs}ms")
    }

    private fun isAnyPlaybackRunning(): Boolean =
        playbackEngine.isRunning || hidPlaybackEngine.isRunning

    private val isAnyPlaybackPlaying: Boolean
        get() = playbackEngine.isPlaying || hidPlaybackEngine.isPlaying

    private val shouldQuantize: Boolean
        get() = _digitalRecording.value && _isRecording.value

    // ── Playback controls ───────────────────────────────────────────────

    fun playRecording(id: String) {
        if (hidReportRecorder.isRecording || isAnyPlaybackRunning()) return
        viewModelScope.launch {
            val hidData = withContext(Dispatchers.IO) { recordingRepo.loadHid(id) }
            if (hidData != null) {
                hidPlaybackEngine.play(hidData)
                val expectedSlots = hidData.durationMs / HidReportPlaybackEngine.SLOT_INTERVAL_MS + 1
                addLog("HID_PLAYBACK_START: ${hidData.name} (${hidData.frameCount} raw → ${expectedSlots} slots@${HidReportPlaybackEngine.SLOT_INTERVAL_MS}ms, ${hidData.durationMs}ms)")
                return@launch
            }
            val data = withContext(Dispatchers.IO) { recordingRepo.load(id) }
            if (data != null) {
                playbackEngine.play(data, viewModelScope)
                addLog("PLAYBACK_START: ${data.name} (${data.events.size} events, ${data.durationMs}ms)")
                return@launch
            }
            addLog("PLAYBACK_FAIL: recording $id not found")
        }
    }

    fun pausePlayback() {
        if (hidPlaybackEngine.isRunning) {
            hidPlaybackEngine.pause()
            addLog("HID_PLAYBACK_PAUSE")
        } else {
            playbackEngine.pause()
            addLog("PLAYBACK_PAUSE")
        }
    }

    fun resumePlayback() {
        if (hidPlaybackEngine.isRunning) {
            hidPlaybackEngine.resume()
            addLog("HID_PLAYBACK_RESUME")
        } else {
            playbackEngine.resume()
            addLog("PLAYBACK_RESUME")
        }
    }

    fun stopPlayback() {
        if (hidPlaybackEngine.isRunning) {
            hidPlaybackEngine.stop()
            addLog("HID_PLAYBACK_STOP")
        } else {
            playbackEngine.stop()
            addLog("PLAYBACK_STOP")
        }
    }

    // ── Recording management ────────────────────────────────────────────

    fun deleteRecording(id: String) {
        if (playbackEngine.progress.value.recordingId == id) playbackEngine.stop()
        if (hidPlaybackEngine.progress.value.recordingId == id) hidPlaybackEngine.stop()
        viewModelScope.launch(Dispatchers.IO) {
            recordingRepo.delete(id)
            recordingRepo.deleteHid(id)
            _recordings.value = recordingRepo.loadAll()
            _hidRecordings.value = recordingRepo.loadAllHid()
        }
    }

    fun renameRecording(id: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            recordingRepo.rename(id, newName)
            recordingRepo.renameHid(id, newName)
            _recordings.value = recordingRepo.loadAll()
            _hidRecordings.value = recordingRepo.loadAllHid()
        }
    }

    /**
     * Reverse-parse a raw HID report to update UI state during playback.
     * This provides visual feedback without affecting the HID pipeline.
     */
    private fun updateGamepadStateFromReport(report: ByteArray) {
        if (report.size < HidDescriptor.REPORT_SIZE) return
        val b0 = report[0].toInt() and 0xFF
        val b1 = report[1].toInt() and 0xFF
        val hat = report[2].toInt() and 0x0F
        val lx = report[3].toInt() and 0xFF
        val ly = report[4].toInt() and 0xFF
        val rx = report[5].toInt() and 0xFF
        val ry = report[6].toInt() and 0xFF
        val lt = report[7].toInt() and 0xFF
        val rt = report[8].toInt() and 0xFF

        val buttons = if (isPsProfile) {
            var xb = 0
            if (b0 and (1 shl 0) != 0) xb = xb or GamepadButtons.X      // Square → X
            if (b0 and (1 shl 1) != 0) xb = xb or GamepadButtons.A      // Cross → A
            if (b0 and (1 shl 2) != 0) xb = xb or GamepadButtons.B      // Circle → B
            if (b0 and (1 shl 3) != 0) xb = xb or GamepadButtons.Y      // Triangle → Y
            if (b0 and (1 shl 4) != 0) xb = xb or GamepadButtons.LB     // L1 → LB
            if (b0 and (1 shl 5) != 0) xb = xb or GamepadButtons.RB     // R1 → RB
            // bits 6,7 = L2/R2 digital — represented by analog triggers
            val b1ps = b1
            if (b1ps and (1 shl 0) != 0) xb = xb or GamepadButtons.BACK  // Create → Back
            if (b1ps and (1 shl 1) != 0) xb = xb or GamepadButtons.START // Options → Start
            if (b1ps and (1 shl 2) != 0) xb = xb or GamepadButtons.L3
            if (b1ps and (1 shl 3) != 0) xb = xb or GamepadButtons.R3
            if (b1ps and (1 shl 4) != 0) xb = xb or GamepadButtons.HOME  // PS → Home
            xb
        } else {
            b0 or (b1 shl 8)
        }

        val dpad = when (hat) {
            1 -> GamepadButtons.DPAD_UP
            2 -> GamepadButtons.DPAD_UP or GamepadButtons.DPAD_RIGHT
            3 -> GamepadButtons.DPAD_RIGHT
            4 -> GamepadButtons.DPAD_DOWN or GamepadButtons.DPAD_RIGHT
            5 -> GamepadButtons.DPAD_DOWN
            6 -> GamepadButtons.DPAD_DOWN or GamepadButtons.DPAD_LEFT
            7 -> GamepadButtons.DPAD_LEFT
            8 -> GamepadButtons.DPAD_UP or GamepadButtons.DPAD_LEFT
            else -> 0
        }

        _gamepadState.value = GamepadState(
            buttons = buttons, dpad = dpad,
            leftX = lx, leftY = ly,
            rightX = rx, rightY = ry,
            leftTrigger = lt, rightTrigger = rt
        )
    }

    // ── Report dispatch ─────────────────────────────────────────────────

    private fun sendThrottled() {
        if (!_isConnected.value) return
        val report = GamepadReportBuilder.buildReport(_gamepadState.value, isPsProfile)
        val sent = hidManager.sendReport(report, force = false)
        if (sent) _reportsSent.value = hidManager.sendCount
    }

    private fun sendForced(context: String) {
        if (!_isConnected.value) {
            addLog("  -- NOT CONNECTED ($context)")
            return
        }
        val report = GamepadReportBuilder.buildReport(_gamepadState.value, isPsProfile)
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
        val report = GamepadReportBuilder.neutralReport(isPsProfile)
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

    fun setControllerProfile(profile: String) {
        viewModelScope.launch {
            settingsRepo.setControllerProfile(profile)
            _connectedHostAddress.value?.let { addr ->
                withContext(Dispatchers.IO) { knownDevicesRepo.updateProfile(addr, profile) }
                _knownDevices.value = withContext(Dispatchers.IO) { knownDevicesRepo.loadAll() }
            }
        }
    }

    fun toggleHaptics(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setHapticsEnabled(enabled) }
    }

    fun setHapticsIntensity(intensity: String) {
        viewModelScope.launch { settingsRepo.setHapticsIntensity(intensity) }
    }

    fun toggleSound(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setSoundEnabled(enabled) }
    }

    fun setSoundStyle(style: String) {
        viewModelScope.launch { settingsRepo.setSoundStyle(style) }
    }

    fun setSoundVolume(volume: Float) {
        viewModelScope.launch { settingsRepo.setSoundVolume(volume) }
    }

    fun setTriggerMode(mode: String) {
        viewModelScope.launch { settingsRepo.setTriggerMode(mode) }
    }

    fun toggleDebugLog(visible: Boolean) {
        viewModelScope.launch { settingsRepo.setDebugLogVisible(visible) }
    }

    fun toggleDebugLogOverlay(overlay: Boolean) {
        viewModelScope.launch { settingsRepo.setDebugLogOverlay(overlay) }
    }

    fun toggleDigitalRecording(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setDigitalRecording(enabled) }
    }

    fun setAssistLeftMode(mode: String) {
        viewModelScope.launch { settingsRepo.setAssistLeftMode(mode) }
    }

    fun setAssistRightMode(mode: String) {
        viewModelScope.launch { settingsRepo.setAssistRightMode(mode) }
    }

    fun setAssistLeftTempo(mode: String) {
        viewModelScope.launch { settingsRepo.setAssistLeftTempo(mode) }
    }

    fun setAssistRightTempo(mode: String) {
        viewModelScope.launch { settingsRepo.setAssistRightTempo(mode) }
    }

    fun setProfileWarningSuppressed(suppressed: Boolean) {
        viewModelScope.launch { settingsRepo.setProfileWarningSuppressed(suppressed) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { settingsRepo.setOnboardingCompleted() }
    }

    override fun onCleared() {
        addLog("VM_ON_CLEARED: | ${diagnosticState()}")
        super.onCleared()
        playbackEngine.stop()
        hwManager.unregister()
        ButtonSoundPlayer.release()
        stopHidService()
        hidManager.cleanup(reason = "VIEWMODEL_CLEARED")
    }
}

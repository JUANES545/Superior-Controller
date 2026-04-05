package com.example.superiorcontroller.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.superiorcontroller.hid.GamepadReportBuilder
import com.example.superiorcontroller.hid.HidDescriptor

@SuppressLint("MissingPermission")
class BluetoothHidManager(private val context: Context) {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var hidDevice: BluetoothHidDevice? = null
    private var hostDevice: BluetoothDevice? = null

    private var _isRegistered = false
    val isRegistered: Boolean get() = _isRegistered

    private var _isConnected = false
    val isConnected: Boolean get() = _isConnected

    // ── Rate limiting ────────────────────────────────────────────────────
    private var lastSentReport: ByteArray? = null
    private var lastSendTimeNs: Long = 0
    private var pendingReport: ByteArray? = null
    private val handler = Handler(Looper.getMainLooper())

    // ── Host switching ───────────────────────────────────────────────────
    private var pendingSwitchAddress: String? = null

    // ── Diagnostic tracking ──────────────────────────────────────────────
    private var lastOperation: String = "NONE"
    private var lastOperationMs: Long = 0L
    private var registerCount: Int = 0
    private var unregisterCallbackCount: Int = 0

    var sendCount: Long = 0; private set
    var skipDedupCount: Long = 0; private set
    var skipThrottleCount: Long = 0; private set
    var failCount: Long = 0; private set

    companion object {
        const val MIN_INTERVAL_MS = 8L
    }

    private fun markOp(name: String) {
        lastOperation = name
        lastOperationMs = System.currentTimeMillis()
    }

    fun stateSnapshot(): String {
        val adapter = bluetoothAdapter
        return "adapter=${adapter?.isEnabled} scanMode=${adapter?.scanMode} " +
            "proxy=${hidDevice != null} reg=$_isRegistered conn=$_isConnected " +
            "host=${hostDevice?.name ?: hostDevice?.address ?: "null"} " +
            "pending=$pendingSwitchAddress " +
            "bonded=${adapter?.bondedDevices?.size ?: -1} " +
            "regCount=$registerCount unregCbCount=$unregisterCallbackCount " +
            "lastOp=$lastOperation ${System.currentTimeMillis() - lastOperationMs}ms_ago"
    }

    private val deferredSendRunnable = Runnable {
        val report = pendingReport ?: return@Runnable
        pendingReport = null
        doSend(report)
    }

    // ── Listener ─────────────────────────────────────────────────────────
    var listener: Listener? = null

    interface Listener {
        fun onProxyReady()
        fun onRegistrationChanged(registered: Boolean)
        fun onConnectionChanged(device: BluetoothDevice?, connected: Boolean)
        fun onLog(message: String)
    }

    // ── HID callback ─────────────────────────────────────────────────────
    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            val thread = Thread.currentThread().name
            val prev = _isRegistered
            val msSinceLast = System.currentTimeMillis() - lastOperationMs
            _isRegistered = registered
            if (registered) registerCount++
            if (!registered) unregisterCallbackCount++
            listener?.onRegistrationChanged(registered)
            listener?.onLog(
                "CB_APP_STATUS: registered=$registered prev=$prev " +
                "plugged=${pluggedDevice?.name}/${pluggedDevice?.address} " +
                "thread=$thread lastOp=$lastOperation ${msSinceLast}ms_ago | ${stateSnapshot()}"
            )
            if (!registered && prev) {
                val trace = Throwable("DIAG_UNREGISTER_TRACE").stackTraceToString()
                    .lineSequence().take(8).joinToString(" << ")
                listener?.onLog("UNREGISTER_TRACE: $trace")
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            val stateName = when (state) {
                BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
                BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"
                BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
                BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
                else -> "UNKNOWN($state)"
            }
            listener?.onLog(
                "CB_CONN_STATE: state=$stateName device=${device?.name}/${device?.address} " +
                "lastOp=$lastOperation | ${stateSnapshot()}"
            )

            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    hostDevice = device
                    _isConnected = true
                    listener?.onConnectionChanged(device, true)
                    listener?.onLog("Host connected: ${device?.name ?: device?.address}")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    val prevHost = hostDevice?.name ?: hostDevice?.address
                    hostDevice = null
                    _isConnected = false
                    lastSentReport = null
                    listener?.onConnectionChanged(null, false)
                    listener?.onLog("Host disconnected (was=$prevHost)")

                    val switchAddr = pendingSwitchAddress
                    if (switchAddr != null) {
                        pendingSwitchAddress = null
                        handler.postDelayed({
                            listener?.onLog("BT_SWITCH: auto-connecting to $switchAddr | ${stateSnapshot()}")
                            connectToAddress(switchAddr)
                        }, 200)
                    }
                }
                BluetoothProfile.STATE_CONNECTING ->
                    listener?.onLog("Connecting to host ${device?.name}...")
                BluetoothProfile.STATE_DISCONNECTING ->
                    listener?.onLog("Disconnecting from host ${device?.name}...")
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            listener?.onLog("CB_GET_REPORT: type=$type id=$id size=$bufferSize device=${device?.name}")
            val report = lastSentReport?.copyOf() ?: GamepadReportBuilder.neutralReport()
            hidDevice?.replyReport(device, type, id, report)
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            listener?.onLog("CB_SET_REPORT: type=$type id=$id dataLen=${data?.size} device=${device?.name}")
        }

        override fun onInterruptData(device: BluetoothDevice?, reportId: Byte, data: ByteArray?) {
            listener?.onLog("CB_INTERRUPT: reportId=$reportId device=${device?.name}")
        }
    }

    // ── Profile listener ─────────────────────────────────────────────────
    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                val prevProxy = hidDevice != null
                hidDevice = proxy as? BluetoothHidDevice
                listener?.onLog(
                    "CB_SERVICE_CONNECTED: profile=HID_DEVICE prevProxy=$prevProxy " +
                    "thread=${Thread.currentThread().name} | ${stateSnapshot()}"
                )
                listener?.onProxyReady()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                val prevState = "proxy=${hidDevice != null} reg=$_isRegistered conn=$_isConnected"
                hidDevice = null
                _isRegistered = false
                _isConnected = false
                listener?.onLog(
                    "CB_SERVICE_DISCONNECTED: profile=HID_DEVICE prev=[$prevState] " +
                    "thread=${Thread.currentThread().name} lastOp=$lastOperation"
                )
            }
        }
    }

    // ── Public API ───────────────────────────────────────────────────────

    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true
    fun isProxyReady(): Boolean = hidDevice != null

    fun initialize(reason: String = "UNKNOWN"): Boolean {
        markOp("initialize($reason)")
        listener?.onLog("INIT_PRE: reason=$reason | ${stateSnapshot()}")

        if (hidDevice != null) {
            listener?.onLog("INIT_SKIP: proxy already available reason=$reason")
            return true
        }
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = manager?.adapter
        if (bluetoothAdapter == null) {
            listener?.onLog("INIT_FAIL: Bluetooth not available reason=$reason")
            return false
        }
        if (!bluetoothAdapter!!.isEnabled) {
            listener?.onLog("INIT_FAIL: Bluetooth disabled reason=$reason")
            return false
        }
        val result = bluetoothAdapter!!.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
        listener?.onLog(
            "INIT_POST: getProfileProxy=$result reason=$reason | ${stateSnapshot()}"
        )
        return result
    }

    fun registerApp(reason: String = "UNKNOWN", profile: String = "xbox"): Boolean {
        markOp("registerApp($reason, profile=$profile)")
        if (_isRegistered) {
            listener?.onLog("REGISTER_GUARD: already registered — skipping reason=$reason | ${stateSnapshot()}")
            return true
        }
        val hid = hidDevice ?: run {
            listener?.onLog("REGISTER_FAIL: proxy not ready reason=$reason | ${stateSnapshot()}")
            return false
        }

        listener?.onLog(
            "REGISTER_PRE: reason=$reason profile=$profile thread=${Thread.currentThread().name} | ${stateSnapshot()}"
        )

        val descriptor = HidDescriptor.descriptorForProfile(profile)
        val sdpSettings = BluetoothHidDeviceAppSdpSettings(
            "Superior Controller",
            "Android Bluetooth HID Gamepad",
            "SuperiorController",
            BluetoothHidDevice.SUBCLASS1_NONE.toInt()
                .or(BluetoothHidDevice.SUBCLASS2_GAMEPAD.toInt()).toByte(),
            descriptor
        )

        val success = hid.registerApp(sdpSettings, null, null, context.mainExecutor, hidCallback)
        listener?.onLog(
            "REGISTER_POST: result=$success reason=$reason thread=${Thread.currentThread().name} | ${stateSnapshot()}"
        )
        return success
    }

    fun unregisterApp(reason: String = "UNKNOWN") {
        markOp("unregisterApp($reason)")
        listener?.onLog("UNREGISTER_PRE: reason=$reason | ${stateSnapshot()}")
        val result = hidDevice?.unregisterApp()
        listener?.onLog("UNREGISTER_POST: result=$result reason=$reason | ${stateSnapshot()}")
    }

    /**
     * Send a HID report with deduplication and optional throttling.
     *
     * @param force If true, bypasses the time throttle (but still deduplicates).
     *              Use for discrete state changes (button press/release).
     *              Non-forced sends are rate-limited to [MIN_INTERVAL_MS] for
     *              continuous updates (joystick axes).
     */
    fun sendReport(report: ByteArray, force: Boolean = false): Boolean {
        if (hostDevice == null || hidDevice == null) return false
        if (report.size != HidDescriptor.REPORT_SIZE) {
            listener?.onLog("ERROR: report size ${report.size} ≠ ${HidDescriptor.REPORT_SIZE}")
            return false
        }

        handler.removeCallbacks(deferredSendRunnable)

        val last = lastSentReport
        if (last != null && last.contentEquals(report)) {
            skipDedupCount++
            return true
        }

        if (!force) {
            val elapsedMs = (System.nanoTime() - lastSendTimeNs) / 1_000_000
            if (elapsedMs < MIN_INTERVAL_MS) {
                pendingReport = report.copyOf()
                handler.postDelayed(deferredSendRunnable, MIN_INTERVAL_MS - elapsedMs)
                skipThrottleCount++
                return true
            }
        }

        return doSend(report)
    }

    private fun doSend(report: ByteArray): Boolean {
        val device = hostDevice ?: return false
        val hid = hidDevice ?: return false
        val sent = hid.sendReport(device, 0, report)
        if (sent) {
            lastSentReport = report.copyOf()
            lastSendTimeNs = System.nanoTime()
            sendCount++
        } else {
            failCount++
        }
        return sent
    }

    fun statsString(): String = "sent=$sendCount dedup=$skipDedupCount throttle=$skipThrottleCount fail=$failCount"

    fun getBondedDevices(): List<BluetoothDevice> = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()

    fun getBondedDeviceInfo(): List<Pair<String, String>> =
        bluetoothAdapter?.bondedDevices?.map { (it.name ?: it.address) to it.address } ?: emptyList()

    /**
     * Returns bonded devices filtered to exclude PERIPHERAL class (gamepads,
     * keyboards, mice). These are input sources, not valid HID host targets.
     * This prevents the user from accidentally trying to connect to their own
     * Xbox controller as an HID output target.
     */
    fun getHostCandidates(): List<Pair<String, String>> =
        bluetoothAdapter?.bondedDevices
            ?.filter {
                val major = it.bluetoothClass?.majorDeviceClass ?: 0
                major != BluetoothClass.Device.Major.PERIPHERAL
            }
            ?.map { (it.name ?: it.address) to it.address }
            ?: emptyList()

    fun connectToHost(device: BluetoothDevice, reason: String = "UNKNOWN"): Boolean {
        markOp("connectToHost($reason)")
        val hid = hidDevice ?: run {
            listener?.onLog("CONNECT_FAIL: proxy null reason=$reason | ${stateSnapshot()}")
            return false
        }
        listener?.onLog("CONNECT_PRE: device=${device.name}/${device.address} reason=$reason | ${stateSnapshot()}")
        val result = hid.connect(device)
        listener?.onLog("CONNECT_POST: result=$result device=${device.name} reason=$reason")
        return result
    }

    fun connectToAddress(address: String, reason: String = "UNKNOWN"): Boolean {
        markOp("connectToAddress($reason)")
        val adapter = bluetoothAdapter ?: run {
            listener?.onLog("CONNECT_FAIL: adapter null reason=$reason | ${stateSnapshot()}")
            return false
        }
        val hid = hidDevice ?: run {
            listener?.onLog("CONNECT_FAIL: proxy null reason=$reason | ${stateSnapshot()}")
            return false
        }
        listener?.onLog("CONNECT_PRE: address=$address reason=$reason | ${stateSnapshot()}")
        return try {
            val device = adapter.getRemoteDevice(address)
            val result = hid.connect(device)
            listener?.onLog("CONNECT_POST: result=$result addr=$address name=${device.name} reason=$reason")
            result
        } catch (e: IllegalArgumentException) {
            listener?.onLog("CONNECT_FAIL: invalid address=$address reason=$reason")
            false
        }
    }

    fun disconnect(reason: String = "UNKNOWN") {
        markOp("disconnect($reason)")
        listener?.onLog("DISCONNECT_PRE: reason=$reason | ${stateSnapshot()}")
        pendingSwitchAddress = null
        val host = hostDevice
        if (host != null) {
            val result = hidDevice?.disconnect(host)
            listener?.onLog("DISCONNECT_POST: result=$result host=${host.name} reason=$reason")
        } else {
            listener?.onLog("DISCONNECT_SKIP: no host connected reason=$reason")
        }
    }

    fun switchToHost(address: String) {
        markOp("switchToHost")
        listener?.onLog("SWITCH_PRE: target=$address | ${stateSnapshot()}")

        if (!_isConnected) {
            listener?.onLog("SWITCH: not connected, connecting directly to $address")
            connectToAddress(address, reason = "HOST_SWITCH_DIRECT")
            return
        }
        if (hostDevice?.address == address) {
            listener?.onLog("SWITCH_SKIP: already connected to $address")
            return
        }
        listener?.onLog("SWITCH: scheduling switch to $address after disconnect from ${hostDevice?.name}")
        pendingSwitchAddress = address
        hostDevice?.let { hidDevice?.disconnect(it) }
    }

    fun connectedHostAddress(): String? = if (_isConnected) hostDevice?.address else null
    fun connectedHostName(): String? = if (_isConnected) (hostDevice?.name ?: hostDevice?.address) else null

    fun cleanup(reason: String = "UNKNOWN") {
        markOp("cleanup($reason)")
        listener?.onLog("CLEANUP_PRE: reason=$reason | ${stateSnapshot()}")
        handler.removeCallbacks(deferredSendRunnable)
        pendingReport = null
        pendingSwitchAddress = null
        if (_isConnected) disconnect(reason = "CLEANUP")
        val hadProxy = hidDevice != null
        hidDevice?.let {
            listener?.onLog("CLEANUP: calling closeProfileProxy reason=$reason")
            bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, it)
        }
        hidDevice = null; hostDevice = null; lastSentReport = null
        _isRegistered = false; _isConnected = false
        listener?.onLog("CLEANUP_POST: hadProxy=$hadProxy reason=$reason | ${stateSnapshot()}")
    }
}

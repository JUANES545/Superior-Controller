package com.example.superiorcontroller.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
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

    var sendCount: Long = 0; private set
    var skipDedupCount: Long = 0; private set
    var skipThrottleCount: Long = 0; private set
    var failCount: Long = 0; private set

    companion object {
        const val MIN_INTERVAL_MS = 25L
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
            _isRegistered = registered
            listener?.onRegistrationChanged(registered)
            listener?.onLog(if (registered) "HID app registered" else "HID app unregistered")
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    hostDevice = device
                    _isConnected = true
                    listener?.onConnectionChanged(device, true)
                    listener?.onLog("Host connected: ${device?.name ?: device?.address}")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    hostDevice = null
                    _isConnected = false
                    lastSentReport = null
                    listener?.onConnectionChanged(null, false)
                    listener?.onLog("Host disconnected")
                }
                BluetoothProfile.STATE_CONNECTING -> listener?.onLog("Connecting to host...")
                BluetoothProfile.STATE_DISCONNECTING -> listener?.onLog("Disconnecting from host...")
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            listener?.onLog("onGetReport type=$type id=$id size=$bufferSize")
            val report = lastSentReport?.copyOf() ?: GamepadReportBuilder.neutralReport()
            hidDevice?.replyReport(device, type, id, report)
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            listener?.onLog("onSetReport type=$type id=$id dataLen=${data?.size}")
        }

        override fun onInterruptData(device: BluetoothDevice?, reportId: Byte, data: ByteArray?) {
            listener?.onLog("onInterruptData reportId=$reportId")
        }
    }

    // ── Profile listener ─────────────────────────────────────────────────
    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = proxy as? BluetoothHidDevice
                listener?.onLog("HID Device profile proxy acquired")
                listener?.onProxyReady()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
                _isRegistered = false
                _isConnected = false
                listener?.onLog("HID Device profile proxy lost")
            }
        }
    }

    // ── Public API ───────────────────────────────────────────────────────

    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true
    fun isProxyReady(): Boolean = hidDevice != null

    fun initialize(): Boolean {
        if (hidDevice != null) {
            listener?.onLog("Proxy already available — skipping init")
            return true
        }
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = manager?.adapter
        if (bluetoothAdapter == null) {
            listener?.onLog("ERROR: Bluetooth not available on this device")
            return false
        }
        if (!bluetoothAdapter!!.isEnabled) {
            listener?.onLog("ERROR: Bluetooth is disabled — enable it in Settings")
            return false
        }
        val result = bluetoothAdapter!!.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
        listener?.onLog(if (result) "Requesting HID Device profile proxy..." else "ERROR: HID Device profile not supported")
        return result
    }

    fun registerApp(): Boolean {
        val hid = hidDevice ?: run {
            listener?.onLog("ERROR: HID proxy not ready")
            return false
        }
        val sdpSettings = BluetoothHidDeviceAppSdpSettings(
            "Superior Controller",
            "Android Bluetooth HID Gamepad",
            "SuperiorController",
            BluetoothHidDevice.SUBCLASS1_NONE.toInt()
                .or(BluetoothHidDevice.SUBCLASS2_GAMEPAD.toInt()).toByte(),
            HidDescriptor.GAMEPAD_DESCRIPTOR
        )
        val qosOut = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT, 800, 9, 0, 11250,
            BluetoothHidDeviceAppQosSettings.MAX
        )
        listener?.onLog("Descriptor ${HidDescriptor.GAMEPAD_DESCRIPTOR.size}B, report ${HidDescriptor.REPORT_SIZE}B")
        val success = hid.registerApp(sdpSettings, null, qosOut, context.mainExecutor, hidCallback)
        listener?.onLog(if (success) "registerApp() — waiting for callback" else "ERROR: registerApp() returned false")
        return success
    }

    fun unregisterApp() {
        listener?.onLog("unregisterApp() result=${hidDevice?.unregisterApp()}")
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

    fun connectToHost(device: BluetoothDevice): Boolean {
        val hid = hidDevice ?: return false
        val result = hid.connect(device)
        listener?.onLog("connect(${device.name ?: device.address}) = $result")
        return result
    }

    fun connectToAddress(address: String): Boolean {
        val adapter = bluetoothAdapter ?: run {
            listener?.onLog("ERROR: adapter null for connectToAddress")
            return false
        }
        val hid = hidDevice ?: run {
            listener?.onLog("ERROR: proxy null for connectToAddress")
            return false
        }
        return try {
            val device = adapter.getRemoteDevice(address)
            val result = hid.connect(device)
            listener?.onLog("connectToAddress($address) = $result")
            result
        } catch (e: IllegalArgumentException) {
            listener?.onLog("ERROR: invalid address $address")
            false
        }
    }

    fun disconnect() {
        hostDevice?.let { hidDevice?.disconnect(it); listener?.onLog("disconnect() called") }
    }

    fun connectedHostAddress(): String? = if (_isConnected) hostDevice?.address else null

    fun cleanup() {
        handler.removeCallbacks(deferredSendRunnable)
        pendingReport = null
        if (_isConnected) disconnect()
        if (_isRegistered) unregisterApp()
        hidDevice?.let { bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, it) }
        hidDevice = null; hostDevice = null; lastSentReport = null
        _isRegistered = false; _isConnected = false
        listener?.onLog("BluetoothHidManager cleaned up")
    }
}

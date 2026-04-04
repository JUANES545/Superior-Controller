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
import com.example.superiorcontroller.hid.GamepadReportBuilder
import com.example.superiorcontroller.hid.HidDescriptor

/**
 * Manages the Bluetooth HID Device profile lifecycle:
 * proxy acquisition → app registration → report sending → cleanup.
 *
 * All callbacks fire on the main thread (via context.mainExecutor).
 */
@SuppressLint("MissingPermission")
class BluetoothHidManager(private val context: Context) {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var hidDevice: BluetoothHidDevice? = null
    private var hostDevice: BluetoothDevice? = null

    private var _isRegistered = false
    val isRegistered: Boolean get() = _isRegistered

    private var _isConnected = false
    val isConnected: Boolean get() = _isConnected

    // ── Listener interface ──────────────────────────────────────────────
    var listener: Listener? = null

    interface Listener {
        fun onProxyReady()
        fun onRegistrationChanged(registered: Boolean)
        fun onConnectionChanged(device: BluetoothDevice?, connected: Boolean)
        fun onLog(message: String)
    }

    // ── HID Device callback ─────────────────────────────────────────────
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
                    listener?.onConnectionChanged(null, false)
                    listener?.onLog("Host disconnected")
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    listener?.onLog("Connecting to host...")
                }
                BluetoothProfile.STATE_DISCONNECTING -> {
                    listener?.onLog("Disconnecting from host...")
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            listener?.onLog("onGetReport type=$type id=$id size=$bufferSize")
            val report = GamepadReportBuilder.neutralReport()
            hidDevice?.replyReport(device, type, id, report)
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            listener?.onLog("onSetReport type=$type id=$id dataLen=${data?.size}")
        }

        override fun onInterruptData(device: BluetoothDevice?, reportId: Byte, data: ByteArray?) {
            listener?.onLog("onInterruptData reportId=$reportId")
        }
    }

    // ── Profile service listener ────────────────────────────────────────
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

    // ── Public API ──────────────────────────────────────────────────────

    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    fun isProxyReady(): Boolean = hidDevice != null

    fun initialize(): Boolean {
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
        val result = bluetoothAdapter!!.getProfileProxy(
            context, profileListener, BluetoothProfile.HID_DEVICE
        )
        listener?.onLog(
            if (result) "Requesting HID Device profile proxy..."
            else "ERROR: HID Device profile not supported by this device"
        )
        return result
    }

    fun registerApp(): Boolean {
        val hid = hidDevice ?: run {
            listener?.onLog("ERROR: HID proxy not ready — call initialize() first")
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
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800,
            9,
            0,
            11250,
            BluetoothHidDeviceAppQosSettings.MAX
        )

        listener?.onLog("Descriptor size: ${HidDescriptor.GAMEPAD_DESCRIPTOR.size} bytes")
        listener?.onLog("Report size: ${HidDescriptor.REPORT_SIZE} bytes")

        val success = hid.registerApp(sdpSettings, null, qosOut, context.mainExecutor, hidCallback)
        listener?.onLog(if (success) "registerApp() called — waiting for callback" else "ERROR: registerApp() returned false")
        return success
    }

    fun unregisterApp() {
        val result = hidDevice?.unregisterApp()
        listener?.onLog("unregisterApp() result=$result")
    }

    fun sendReport(report: ByteArray): Boolean {
        val device = hostDevice ?: return false
        val hid = hidDevice ?: return false
        if (report.size != HidDescriptor.REPORT_SIZE) {
            listener?.onLog("ERROR: report size ${report.size} ≠ expected ${HidDescriptor.REPORT_SIZE}")
            return false
        }
        return hid.sendReport(device, 0, report)
    }

    fun getBondedDevices(): List<BluetoothDevice> {
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    fun connectToHost(device: BluetoothDevice): Boolean {
        val hid = hidDevice ?: return false
        val result = hid.connect(device)
        listener?.onLog("connect(${device.name ?: device.address}) = $result")
        return result
    }

    fun disconnect() {
        hostDevice?.let { device ->
            hidDevice?.disconnect(device)
            listener?.onLog("disconnect() called")
        }
    }

    fun cleanup() {
        if (_isConnected) disconnect()
        if (_isRegistered) unregisterApp()
        hidDevice?.let {
            bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, it)
        }
        hidDevice = null
        hostDevice = null
        _isRegistered = false
        _isConnected = false
        listener?.onLog("BluetoothHidManager cleaned up")
    }
}

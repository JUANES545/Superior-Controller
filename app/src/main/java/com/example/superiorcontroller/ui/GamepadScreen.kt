package com.example.superiorcontroller.ui

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.superiorcontroller.R
import com.example.superiorcontroller.hid.AxisDefaults
import com.example.superiorcontroller.hid.GamepadButtons
import com.example.superiorcontroller.hid.TriggerDefaults
import com.example.superiorcontroller.recording.PlaybackStatus
import com.example.superiorcontroller.ui.components.ActionButtons
import com.example.superiorcontroller.ui.components.BumperRow
import com.example.superiorcontroller.ui.components.ControlButton
import com.example.superiorcontroller.ui.components.DPad
import com.example.superiorcontroller.ui.components.DebugLog
import com.example.superiorcontroller.ui.components.DeviceSelectorSheet
import com.example.superiorcontroller.ui.components.GamepadTrigger
import com.example.superiorcontroller.ui.components.MenuButtons
import com.example.superiorcontroller.ui.components.PlaybackBar
import com.example.superiorcontroller.ui.components.RecordControls
import com.example.superiorcontroller.ui.components.RecordingsSheet
import com.example.superiorcontroller.ui.components.SettingsSheet
import com.example.superiorcontroller.ui.components.StickClickRow
import com.example.superiorcontroller.ui.components.TriggerRow
import com.example.superiorcontroller.ui.components.VirtualJoystick
import com.example.superiorcontroller.viewmodel.GamepadViewModel

@Composable
fun GamepadScreen(
    viewModel: GamepadViewModel,
    permissionsGranted: Boolean,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logMessages by viewModel.logMessages.collectAsState()

    val discoverableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    val onMakeDiscoverable: () -> Unit = {
        discoverableLauncher.launch(
            Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }
        )
    }

    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val triggerMode by viewModel.triggerMode.collectAsState()
    val triggerButtonMode = triggerMode == "button"
    val debugLogVisible by viewModel.debugLogVisible.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var showRecordings by remember { mutableStateOf(false) }

    val onPress: (Int) -> Unit = { viewModel.pressButton(it) }
    val onRelease: (Int) -> Unit = { viewModel.releaseButton(it) }
    val onLeftTrigger: (Float) -> Unit = { viewModel.setLeftTrigger(it) }
    val onRightTrigger: (Float) -> Unit = { viewModel.setRightTrigger(it) }

    val gs by viewModel.gamepadState.collectAsState()
    val gsButtons = gs.buttons or gs.dpad
    val gsLeftX = (gs.leftX - AxisDefaults.CENTER.toFloat()) / AxisDefaults.CENTER
    val gsLeftY = (gs.leftY - AxisDefaults.CENTER.toFloat()) / AxisDefaults.CENTER
    val gsRightX = (gs.rightX - AxisDefaults.CENTER.toFloat()) / AxisDefaults.CENTER
    val gsRightY = (gs.rightY - AxisDefaults.CENTER.toFloat()) / AxisDefaults.CENTER
    val gsLT = gs.leftTrigger.toFloat() / TriggerDefaults.MAX
    val gsRT = gs.rightTrigger.toFloat() / TriggerDefaults.MAX

    val isRecording by viewModel.isRecording.collectAsState()
    val recordingElapsedMs by viewModel.recordingElapsedMs.collectAsState()
    val recordings by viewModel.recordings.collectAsState()
    val playbackProgress by viewModel.playbackProgress.collectAsState()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            LandscapeLayout(
                viewModel = viewModel,
                permissionsGranted = permissionsGranted,
                onRequestPermissions = onRequestPermissions,
                onMakeDiscoverable = onMakeDiscoverable,
                logMessages = logMessages,
                onPress = onPress,
                onRelease = onRelease,
                onLeftTrigger = onLeftTrigger,
                onRightTrigger = onRightTrigger,
                triggerButtonMode = triggerButtonMode,
                debugLogVisible = debugLogVisible,
                gsButtons = gsButtons,
                gsLeftX = gsLeftX, gsLeftY = gsLeftY,
                gsRightX = gsRightX, gsRightY = gsRightY,
                gsLT = gsLT, gsRT = gsRT
            )
        } else {
            PortraitLayout(
                viewModel = viewModel,
                permissionsGranted = permissionsGranted,
                onRequestPermissions = onRequestPermissions,
                onMakeDiscoverable = onMakeDiscoverable,
                logMessages = logMessages,
                onPress = onPress,
                onRelease = onRelease,
                onLeftTrigger = onLeftTrigger,
                onRightTrigger = onRightTrigger,
                triggerButtonMode = triggerButtonMode,
                debugLogVisible = debugLogVisible,
                gsButtons = gsButtons,
                gsLeftX = gsLeftX, gsLeftY = gsLeftY,
                gsRightX = gsRightX, gsRightY = gsRightY,
                gsLT = gsLT, gsRT = gsRT
            )
        }

        if (playbackProgress.status != PlaybackStatus.IDLE) {
            PlaybackBar(
                progress = playbackProgress,
                onPause = { viewModel.pausePlayback() },
                onResume = { viewModel.resumePlayback() },
                onStop = { viewModel.stopPlayback() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 72.dp)
            )
        }

        RecordControls(
            isRecording = isRecording,
            elapsedMs = recordingElapsedMs,
            hasRecordings = recordings.isNotEmpty(),
            onToggleRecord = { if (isRecording) viewModel.stopRecording() else viewModel.startRecording() },
            onOpenRecordings = { showRecordings = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        )

        SmallFloatingActionButton(
            onClick = { showSettings = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings_title)
            )
        }
    }

    if (showSettings) {
        SettingsSheet(
            hapticsEnabled = hapticsEnabled,
            soundEnabled = soundEnabled,
            triggerMode = triggerMode,
            debugLogVisible = debugLogVisible,
            onToggleHaptics = { viewModel.toggleHaptics(it) },
            onToggleSound = { viewModel.toggleSound(it) },
            onTriggerModeChange = { viewModel.setTriggerMode(it) },
            onToggleDebugLog = { viewModel.toggleDebugLog(it) },
            onDismiss = { showSettings = false }
        )
    }

    if (showRecordings) {
        RecordingsSheet(
            recordings = recordings,
            playbackProgress = playbackProgress,
            onPlay = { viewModel.playRecording(it) },
            onPause = { viewModel.pausePlayback() },
            onResume = { viewModel.resumePlayback() },
            onStop = { viewModel.stopPlayback() },
            onDelete = { viewModel.deleteRecording(it) },
            onRename = { id, name -> viewModel.renameRecording(id, name) },
            onDismiss = { showRecordings = false }
        )
    }
}

// ── Portrait ────────────────────────────────────────────────────────────────

@Composable
private fun PortraitLayout(
    viewModel: GamepadViewModel,
    permissionsGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    logMessages: List<String>,
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    onLeftTrigger: (Float) -> Unit,
    onRightTrigger: (Float) -> Unit,
    triggerButtonMode: Boolean,
    debugLogVisible: Boolean,
    gsButtons: Int,
    gsLeftX: Float, gsLeftY: Float,
    gsRightX: Float, gsRightY: Float,
    gsLT: Float, gsRT: Float
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BluetoothBar(
            viewModel = viewModel,
            permissionsGranted = permissionsGranted,
            onRequestPermissions = onRequestPermissions,
            onMakeDiscoverable = onMakeDiscoverable,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        TriggerRow(onLeftTrigger = onLeftTrigger, onRightTrigger = onRightTrigger,
            buttonMode = triggerButtonMode, hwLeftTrigger = gsLT, hwRightTrigger = gsRT)
        Spacer(Modifier.height(4.dp))
        BumperRow(onPress = onPress, onRelease = onRelease, hwButtons = gsButtons)

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DPad(onPress = onPress, onRelease = onRelease, hwButtons = gsButtons)
            ActionButtons(onPress = onPress, onRelease = onRelease, hwButtons = gsButtons)
        }

        Spacer(Modifier.height(4.dp))

        MenuButtons(onPress = onPress, onRelease = onRelease, hwButtons = gsButtons)

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            VirtualJoystick(
                onAxisChanged = { x, y -> viewModel.setLeftAxis(x, y) },
                size = 130.dp,
                label = stringResource(R.string.label_left_stick),
                hwX = gsLeftX, hwY = gsLeftY
            )
            VirtualJoystick(
                onAxisChanged = { x, y -> viewModel.setRightAxis(x, y) },
                size = 130.dp,
                label = stringResource(R.string.label_right_stick),
                thumbColor = Color(0xFFFF9800),
                hwX = gsRightX, hwY = gsRightY
            )
        }

        Spacer(Modifier.height(2.dp))
        StickClickRow(onPress = onPress, onRelease = onRelease, hwButtons = gsButtons)

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.sendNeutralReport() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F))
            ) { Text(stringResource(R.string.btn_send_neutral), fontSize = 12.sp) }
            OutlinedButton(
                onClick = { viewModel.clearLog() },
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.btn_clear_log), fontSize = 12.sp) }
        }

        if (debugLogVisible) {
            Spacer(Modifier.height(6.dp))
            DebugLog(messages = logMessages)
        }

        Spacer(Modifier.height(80.dp))
    }
}

// ── Landscape ───────────────────────────────────────────────────────────────

@Composable
private fun LandscapeLayout(
    viewModel: GamepadViewModel,
    permissionsGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    logMessages: List<String>,
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    onLeftTrigger: (Float) -> Unit,
    onRightTrigger: (Float) -> Unit,
    triggerButtonMode: Boolean,
    debugLogVisible: Boolean,
    gsButtons: Int,
    gsLeftX: Float, gsLeftY: Float,
    gsRightX: Float, gsRightY: Float,
    gsLT: Float, gsRT: Float
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BluetoothBar(
                viewModel = viewModel,
                permissionsGranted = permissionsGranted,
                onRequestPermissions = onRequestPermissions,
                onMakeDiscoverable = onMakeDiscoverable,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = { viewModel.sendNeutralReport() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F))
                ) { Text(stringResource(R.string.btn_send_neutral), fontSize = 10.sp) }
                OutlinedButton(onClick = { viewModel.clearLog() }) {
                    Text(stringResource(R.string.btn_clear_log), fontSize = 10.sp)
                }
            }
        }

        Spacer(Modifier.height(2.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(0.22f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                GamepadTrigger("LT", onValueChanged = onLeftTrigger, buttonMode = triggerButtonMode,
                    hwValue = gsLT, width = 78.dp, height = 40.dp)
                ControlButton("LB", GamepadButtons.LB, Color(0xFFFF9800), onPress, onRelease,
                    hwPressed = (gsButtons and GamepadButtons.LB) != 0, width = 78.dp, height = 32.dp, fontSize = 12)
                VirtualJoystick(
                    onAxisChanged = { x, y -> viewModel.setLeftAxis(x, y) },
                    size = 100.dp,
                    label = stringResource(R.string.label_left_stick),
                    hwX = gsLeftX, hwY = gsLeftY
                )
                ControlButton("L3", GamepadButtons.L3, Color(0xFF546E7A), onPress, onRelease,
                    hwPressed = (gsButtons and GamepadButtons.L3) != 0, width = 50.dp, height = 26.dp, fontSize = 10)
            }

            Column(
                modifier = Modifier.weight(0.56f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DPad(onPress = onPress, onRelease = onRelease, hwButtons = gsButtons,
                        btnWidth = 42.dp, btnHeight = 36.dp, offset = 38.dp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MenuButtons(onPress = onPress, onRelease = onRelease, hwButtons = gsButtons)
                    }
                    ActionButtons(onPress = onPress, onRelease = onRelease, hwButtons = gsButtons,
                        buttonSize = 46.dp, spacing = 32.dp)
                }
            }

            Column(
                modifier = Modifier.weight(0.22f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                GamepadTrigger("RT", onValueChanged = onRightTrigger, buttonMode = triggerButtonMode,
                    hwValue = gsRT, width = 78.dp, height = 40.dp)
                ControlButton("RB", GamepadButtons.RB, Color(0xFFFF9800), onPress, onRelease,
                    hwPressed = (gsButtons and GamepadButtons.RB) != 0, width = 78.dp, height = 32.dp, fontSize = 12)
                VirtualJoystick(
                    onAxisChanged = { x, y -> viewModel.setRightAxis(x, y) },
                    size = 100.dp,
                    label = stringResource(R.string.label_right_stick),
                    thumbColor = Color(0xFFFF9800),
                    hwX = gsRightX, hwY = gsRightY
                )
                ControlButton("R3", GamepadButtons.R3, Color(0xFF546E7A), onPress, onRelease,
                    hwPressed = (gsButtons and GamepadButtons.R3) != 0, width = 50.dp, height = 26.dp, fontSize = 10)
            }
        }

        if (debugLogVisible) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                DebugLog(
                    messages = logMessages,
                    maxHeight = 72.dp,
                    minHeight = 44.dp,
                    modifier = Modifier.fillMaxWidth(0.65f)
                )
            }
        }
    }
}

// ── Bluetooth status bar ────────────────────────────────────────────────────

@Composable
private fun BluetoothBar(
    viewModel: GamepadViewModel,
    permissionsGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bluetoothAvailable by viewModel.bluetoothAvailable.collectAsState()
    val proxyReady by viewModel.proxyReady.collectAsState()
    val isRegistered by viewModel.isRegistered.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val connectedDevice by viewModel.connectedDeviceName.collectAsState()
    val connectedAddress by viewModel.connectedHostAddress.collectAsState()
    val reportsSent by viewModel.reportsSent.collectAsState()
    val knownDevices by viewModel.knownDevices.collectAsState()
    val bondedDevices by viewModel.bondedDeviceInfo.collectAsState()
    val hwConnected by viewModel.hwConnected.collectAsState()
    val hwDeviceName by viewModel.hwDeviceName.collectAsState()

    var showDeviceSelector by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Pre-registration: show setup buttons ─────────────────
        if (!permissionsGranted) {
            Button(onClick = onRequestPermissions) {
                Text(stringResource(R.string.btn_grant_permissions), fontSize = 11.sp)
            }
            return@Row
        }
        if (!bluetoothAvailable) {
            Button(onClick = { viewModel.initializeBluetooth() }) {
                Text(stringResource(R.string.btn_initialize_bt), fontSize = 11.sp)
            }
            return@Row
        }
        if (bluetoothAvailable && proxyReady && !isRegistered) {
            StatusDots(bt = true, hid = false)
            Button(
                onClick = { viewModel.registerHidApp() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) { Text(stringResource(R.string.btn_register_hid), fontSize = 11.sp) }
            return@Row
        }

        // ── Post-registration: status dots + chips ───────────────
        if (isRegistered) {
            StatusDots(bt = true, hid = true)

            HostChip(
                isConnected = isConnected,
                deviceName = connectedDevice,
                onClick = {
                    viewModel.refreshBondedDevices()
                    showDeviceSelector = true
                }
            )

            if (!isConnected) {
                val lastKnown = knownDevices.firstOrNull()
                if (lastKnown != null) {
                    Button(
                        onClick = { viewModel.switchToDevice(lastKnown.address) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Text(
                            "↻ ${lastKnown.displayName}",
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HwControllerChip(connected = hwConnected, deviceName = hwDeviceName)

            Text(
                text = "#$reportsSent",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showDeviceSelector) {
        DeviceSelectorSheet(
            isConnected = isConnected,
            isRegistered = isRegistered,
            connectedAddress = connectedAddress,
            connectedDeviceName = connectedDevice,
            knownDevices = knownDevices,
            bondedDevices = bondedDevices,
            onConnect = { address ->
                viewModel.switchToDevice(address)
                showDeviceSelector = false
            },
            onDisconnect = {
                viewModel.disconnectFromHost()
            },
            onRemoveKnown = { viewModel.removeKnownDevice(it) },
            onRenameKnown = { address, alias -> viewModel.renameKnownDevice(address, alias) },
            onMakeDiscoverable = {
                onMakeDiscoverable()
                showDeviceSelector = false
            },
            onUnregister = {
                viewModel.unregisterHidApp()
                showDeviceSelector = false
            },
            onDismiss = { showDeviceSelector = false }
        )
    }
}

// ── Sub-components ──────────────────────────────────────────────────────────

@Composable
private fun StatusDots(bt: Boolean, hid: Boolean) {
    val green = Color(0xFF4CAF50)
    val red = Color(0xFFB71C1C)
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusDot(active = bt, activeColor = green, inactiveColor = red)
        Spacer(Modifier.width(3.dp))
        StatusDot(active = hid, activeColor = green, inactiveColor = red)
    }
}

@Composable
private fun StatusDot(active: Boolean, activeColor: Color, inactiveColor: Color) {
    val color by animateColorAsState(if (active) activeColor else inactiveColor, label = "dot")
    Box(Modifier.size(8.dp).background(color, CircleShape))
}

@Composable
private fun HostChip(
    isConnected: Boolean,
    deviceName: String?,
    onClick: () -> Unit
) {
    val bgColor = if (isConnected)
        Color(0xFF4CAF50).copy(alpha = 0.15f)
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val dotColor = if (isConnected) Color(0xFF4CAF50) else Color(0xFFB71C1C)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(7.dp).background(dotColor, CircleShape))
            Spacer(Modifier.width(5.dp))
            Text(
                text = if (isConnected && deviceName != null) deviceName
                       else stringResource(R.string.status_no_host),
                fontSize = 11.sp,
                fontWeight = if (isConnected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(3.dp))
            Text("▾", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HwControllerChip(connected: Boolean, deviceName: String?) {
    if (!connected) return

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF2196F3).copy(alpha = 0.12f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🎮", fontSize = 10.sp)
            Spacer(Modifier.width(4.dp))
            Text(
                text = deviceName?.take(16) ?: stringResource(R.string.status_hw_controller),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color(0xFF1565C0)
            )
        }
    }
}

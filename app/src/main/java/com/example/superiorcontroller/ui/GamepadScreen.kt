package com.example.superiorcontroller.ui

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextAlign
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
import com.example.superiorcontroller.viewmodel.HwConnectionType

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

    val showBtWarning by viewModel.showBtWarning.collectAsState()

    if (showBtWarning) {
        BtWarningDialog(
            onConfirm = { viewModel.confirmBtWarning() },
            onDismiss = { viewModel.dismissBtWarning() }
        )
    }

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

// ── BT Warning Dialog ──────────────────────────────────────────────────

@Composable
private fun BtWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text("⚠️", fontSize = 28.sp) },
        title = {
            Text(
                stringResource(R.string.bt_warning_title),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Text(
                stringResource(R.string.bt_warning_body),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.bt_warning_continue))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

// ── Portrait ────────────────────────────────────────────────────────────

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
    val hwConnected by viewModel.hwConnected.collectAsState()
    val hwDeviceName by viewModel.hwDeviceName.collectAsState()
    val hwType by viewModel.hwConnectionType.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatusCard(
            viewModel = viewModel,
            permissionsGranted = permissionsGranted,
            onRequestPermissions = onRequestPermissions,
            onMakeDiscoverable = onMakeDiscoverable,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        TriggerRow(
            onLeftTrigger = onLeftTrigger, onRightTrigger = onRightTrigger,
            buttonMode = triggerButtonMode, hwLeftTrigger = gsLT, hwRightTrigger = gsRT
        )
        Spacer(Modifier.height(4.dp))

        InputBumperRow(
            onPress = onPress,
            onRelease = onRelease,
            hwButtons = gsButtons,
            hwConnected = hwConnected,
            hwDeviceName = hwDeviceName,
            hwType = hwType
        )

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

// ── Landscape ───────────────────────────────────────────────────────────

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
            StatusCard(
                viewModel = viewModel,
                permissionsGranted = permissionsGranted,
                onRequestPermissions = onRequestPermissions,
                onMakeDiscoverable = onMakeDiscoverable,
                modifier = Modifier.weight(1f),
                compact = true
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

// ── Input + Bumper Row (LB · InputIndicator · RB) ──────────────────────

@Composable
private fun InputBumperRow(
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    hwButtons: Int,
    hwConnected: Boolean,
    hwDeviceName: String?,
    hwType: HwConnectionType
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ControlButton(
            "LB", GamepadButtons.LB, Color(0xFFFF9800), onPress, onRelease,
            hwPressed = (hwButtons and GamepadButtons.LB) != 0
        )

        InputIndicator(
            hwConnected = hwConnected,
            hwDeviceName = hwDeviceName,
            hwType = hwType,
            modifier = Modifier.weight(1f, fill = false).padding(horizontal = 8.dp)
        )

        ControlButton(
            "RB", GamepadButtons.RB, Color(0xFFFF9800), onPress, onRelease,
            hwPressed = (hwButtons and GamepadButtons.RB) != 0
        )
    }
}

// ── Input Indicator (2 lines, centered) ────────────────────────────────

@Composable
private fun InputIndicator(
    hwConnected: Boolean,
    hwDeviceName: String?,
    hwType: HwConnectionType,
    modifier: Modifier = Modifier
) {
    if (hwConnected && hwDeviceName != null) {
        val typeLine = when (hwType) {
            HwConnectionType.BLUETOOTH -> stringResource(R.string.input_type_bluetooth)
            HwConnectionType.USB -> stringResource(R.string.input_type_usb)
            HwConnectionType.NONE -> stringResource(R.string.card_input_virtual)
        }
        val accentColor = when (hwType) {
            HwConnectionType.BLUETOOTH -> Color(0xFF2196F3)
            HwConnectionType.USB -> Color(0xFF4CAF50)
            HwConnectionType.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = typeLine,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = accentColor,
                letterSpacing = 0.5.sp
            )
            Text(
                text = hwDeviceName,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    } else {
        Text(
            text = stringResource(R.string.card_input_virtual),
            modifier = modifier,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ── Status Card ────────────────────────────────────────────────────────

@Composable
private fun StatusCard(
    viewModel: GamepadViewModel,
    permissionsGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
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

    var showDeviceSelector by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        when {
            !permissionsGranted -> PermissionsContent(onRequestPermissions, compact)
            !bluetoothAvailable -> InitBtContent(viewModel, compact)
            else -> MainStatusContent(
                viewModel = viewModel,
                isRegistered = isRegistered,
                isConnected = isConnected,
                connectedDevice = connectedDevice,
                proxyReady = proxyReady,
                reportsSent = reportsSent,
                bluetoothAvailable = bluetoothAvailable,
                compact = compact,
                onOpenDevices = {
                    viewModel.refreshBondedDevices()
                    showDeviceSelector = true
                }
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

// ── Card content states ────────────────────────────────────────────────

@Composable
private fun PermissionsContent(onRequestPermissions: () -> Unit, compact: Boolean) {
    Column(modifier = Modifier.padding(if (compact) 10.dp else 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepDot(StepState.BLOCKED)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.card_permissions_needed),
                style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        if (!compact) Spacer(Modifier.height(12.dp))
        Button(
            onClick = onRequestPermissions,
            modifier = if (compact) Modifier.padding(top = 4.dp) else Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text(
                stringResource(R.string.btn_grant_permissions),
                fontSize = if (compact) 11.sp else 14.sp
            )
        }
    }
}

@Composable
private fun InitBtContent(viewModel: GamepadViewModel, compact: Boolean) {
    Column(modifier = Modifier.padding(if (compact) 10.dp else 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepDot(StepState.BLOCKED)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.card_bt_disabled),
                style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        if (!compact) Spacer(Modifier.height(12.dp))
        Button(
            onClick = { viewModel.initializeBluetooth() },
            modifier = if (compact) Modifier.padding(top = 4.dp) else Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text(
                stringResource(R.string.btn_initialize_bt),
                fontSize = if (compact) 11.sp else 14.sp
            )
        }
    }
}

@Composable
private fun MainStatusContent(
    viewModel: GamepadViewModel,
    isRegistered: Boolean,
    isConnected: Boolean,
    connectedDevice: String?,
    proxyReady: Boolean,
    reportsSent: Long,
    bluetoothAvailable: Boolean,
    compact: Boolean,
    onOpenDevices: () -> Unit
) {
    Column(modifier = Modifier.padding(if (compact) 10.dp else 16.dp)) {
        if (!compact) {
            ConnectionSteps(
                bluetoothOk = bluetoothAvailable && proxyReady,
                hidRegistered = isRegistered,
                hostConnected = isConnected,
                modifier = Modifier.fillMaxWidth()
            )
            if (isConnected && connectedDevice != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = connectedDevice,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "#$reportsSent",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val statusColor = when {
                isConnected -> Color(0xFF4CAF50)
                isRegistered -> Color(0xFF2196F3)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val statusText = when {
                isConnected -> stringResource(R.string.card_connected_to, connectedDevice ?: "")
                isRegistered -> stringResource(R.string.card_hid_ready)
                proxyReady -> stringResource(R.string.card_ready)
                else -> stringResource(R.string.card_initializing)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepDot(
                    when {
                        isConnected -> StepState.DONE
                        isRegistered -> StepState.ACTIVE
                        else -> StepState.PENDING
                    }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isConnected) {
                    Spacer(Modifier.width(6.dp))
                    Text("#$reportsSent", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(if (compact) 6.dp else 12.dp))

        if (compact) {
            CompactActions(
                isConnected = isConnected,
                proxyReady = proxyReady,
                viewModel = viewModel,
                onOpenDevices = onOpenDevices
            )
        } else {
            FullActions(
                isConnected = isConnected,
                proxyReady = proxyReady,
                viewModel = viewModel,
                onOpenDevices = onOpenDevices
            )
        }
    }
}

@Composable
private fun CompactActions(
    isConnected: Boolean,
    proxyReady: Boolean,
    viewModel: GamepadViewModel,
    onOpenDevices: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (isConnected) {
            OutlinedButton(onClick = onOpenDevices) {
                Text(stringResource(R.string.card_manage_devices), fontSize = 10.sp)
            }
            Button(
                onClick = { viewModel.disconnectFromHost() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
            ) {
                Text(stringResource(R.string.btn_disconnect), fontSize = 10.sp, color = Color.White)
            }
        } else if (proxyReady) {
            Button(
                onClick = onOpenDevices,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Text(stringResource(R.string.card_connect_device), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun FullActions(
    isConnected: Boolean,
    proxyReady: Boolean,
    viewModel: GamepadViewModel,
    onOpenDevices: () -> Unit
) {
    val knownDevices by viewModel.knownDevices.collectAsState()
    val isRegistered by viewModel.isRegistered.collectAsState()

    if (isConnected) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onOpenDevices,
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.card_manage_devices), fontSize = 13.sp) }
            Button(
                onClick = { viewModel.disconnectFromHost() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
            ) { Text(stringResource(R.string.btn_disconnect), fontSize = 13.sp, color = Color.White) }
        }
    } else if (proxyReady) {
        Button(
            onClick = onOpenDevices,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
        ) {
            Text(stringResource(R.string.card_connect_device), fontSize = 14.sp)
        }

        if (!isRegistered && knownDevices.isNotEmpty()) {
            val last = knownDevices.first()
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = { viewModel.switchToDevice(last.address) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.card_reconnect, last.displayName),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── Connection Steps ───────────────────────────────────────────────────

private enum class StepState { PENDING, ACTIVE, DONE, BLOCKED }

@Composable
private fun ConnectionSteps(
    bluetoothOk: Boolean,
    hidRegistered: Boolean,
    hostConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val btState = when {
        bluetoothOk -> StepState.DONE
        else -> StepState.ACTIVE
    }
    val hidState = when {
        hidRegistered -> StepState.DONE
        bluetoothOk -> StepState.ACTIVE
        else -> StepState.PENDING
    }
    val deviceState = when {
        hostConnected -> StepState.DONE
        hidRegistered -> StepState.ACTIVE
        else -> StepState.PENDING
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepItem(stringResource(R.string.step_bluetooth), btState)
        StepLine(btState == StepState.DONE)
        StepItem(stringResource(R.string.step_hid), hidState)
        StepLine(hidState == StepState.DONE)
        StepItem(stringResource(R.string.step_device), deviceState)
    }
}

@Composable
private fun StepItem(label: String, state: StepState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        StepDot(state)
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (state == StepState.ACTIVE || state == StepState.DONE) FontWeight.SemiBold else FontWeight.Normal,
            color = when (state) {
                StepState.DONE -> Color(0xFF4CAF50)
                StepState.ACTIVE -> Color(0xFF2196F3)
                StepState.BLOCKED -> Color(0xFFFF9800)
                StepState.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
private fun StepDot(state: StepState) {
    val color by animateColorAsState(
        when (state) {
            StepState.DONE -> Color(0xFF4CAF50)
            StepState.ACTIVE -> Color(0xFF2196F3)
            StepState.BLOCKED -> Color(0xFFFF9800)
            StepState.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
        },
        label = "step"
    )
    Box(Modifier.size(10.dp).background(color, CircleShape))
}

@Composable
private fun StepLine(completed: Boolean) {
    val color by animateColorAsState(
        if (completed) Color(0xFF4CAF50).copy(alpha = 0.6f)
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
        label = "line"
    )
    Box(
        Modifier
            .width(28.dp)
            .height(2.dp)
            .background(color, RoundedCornerShape(1.dp))
    )
}

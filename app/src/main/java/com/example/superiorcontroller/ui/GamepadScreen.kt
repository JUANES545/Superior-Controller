package com.example.superiorcontroller.ui

import android.app.Activity
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.superiorcontroller.R
import com.example.superiorcontroller.ui.components.ActionButtons
import com.example.superiorcontroller.ui.components.BumperRow
import com.example.superiorcontroller.ui.components.ControlButton
import com.example.superiorcontroller.ui.components.GamepadTrigger
import com.example.superiorcontroller.ui.components.DPad
import com.example.superiorcontroller.ui.components.DebugLog
import com.example.superiorcontroller.ui.components.MenuButtons
import com.example.superiorcontroller.ui.components.SettingsSheet
import com.example.superiorcontroller.ui.components.StatusPanel
import com.example.superiorcontroller.ui.components.StickClickRow
import com.example.superiorcontroller.ui.components.TriggerRow
import com.example.superiorcontroller.ui.components.VirtualJoystick
import com.example.superiorcontroller.hid.GamepadButtons
import com.example.superiorcontroller.viewmodel.GamepadViewModel

@Composable
fun GamepadScreen(
    viewModel: GamepadViewModel,
    permissionsGranted: Boolean,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bluetoothAvailable by viewModel.bluetoothAvailable.collectAsState()
    val proxyReady by viewModel.proxyReady.collectAsState()
    val isRegistered by viewModel.isRegistered.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val connectedDevice by viewModel.connectedDeviceName.collectAsState()
    val reportsSent by viewModel.reportsSent.collectAsState()
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

    val onPress: (Int) -> Unit = { viewModel.pressButton(it) }
    val onRelease: (Int) -> Unit = { viewModel.releaseButton(it) }
    val onLeftTrigger: (Float) -> Unit = { viewModel.setLeftTrigger(it) }
    val onRightTrigger: (Float) -> Unit = { viewModel.setRightTrigger(it) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            LandscapeLayout(
                viewModel = viewModel,
                permissionsGranted = permissionsGranted,
                onRequestPermissions = onRequestPermissions,
                onMakeDiscoverable = onMakeDiscoverable,
                bluetoothAvailable = bluetoothAvailable,
                proxyReady = proxyReady,
                isRegistered = isRegistered,
                isConnected = isConnected,
                connectedDevice = connectedDevice,
                reportsSent = reportsSent,
                logMessages = logMessages,
                onPress = onPress,
                onRelease = onRelease,
                onLeftTrigger = onLeftTrigger,
                onRightTrigger = onRightTrigger,
                triggerButtonMode = triggerButtonMode,
                debugLogVisible = debugLogVisible
            )
        } else {
            PortraitLayout(
                viewModel = viewModel,
                permissionsGranted = permissionsGranted,
                onRequestPermissions = onRequestPermissions,
                onMakeDiscoverable = onMakeDiscoverable,
                bluetoothAvailable = bluetoothAvailable,
                proxyReady = proxyReady,
                isRegistered = isRegistered,
                isConnected = isConnected,
                connectedDevice = connectedDevice,
                reportsSent = reportsSent,
                logMessages = logMessages,
                onPress = onPress,
                onRelease = onRelease,
                onLeftTrigger = onLeftTrigger,
                onRightTrigger = onRightTrigger,
                triggerButtonMode = triggerButtonMode,
                debugLogVisible = debugLogVisible
            )
        }

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
}

// ── Portrait ────────────────────────────────────────────────────────────────

@Composable
private fun PortraitLayout(
    viewModel: GamepadViewModel,
    permissionsGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    bluetoothAvailable: Boolean,
    proxyReady: Boolean,
    isRegistered: Boolean,
    isConnected: Boolean,
    connectedDevice: String?,
    reportsSent: Long,
    logMessages: List<String>,
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    onLeftTrigger: (Float) -> Unit,
    onRightTrigger: (Float) -> Unit,
    triggerButtonMode: Boolean,
    debugLogVisible: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatusPanel(
            bluetoothAvailable = bluetoothAvailable,
            isRegistered = isRegistered,
            isConnected = isConnected,
            connectedDeviceName = connectedDevice,
            reportsSent = reportsSent
        )

        Spacer(Modifier.height(6.dp))

        ConnectionButtons(
            viewModel = viewModel,
            permissionsGranted = permissionsGranted,
            onRequestPermissions = onRequestPermissions,
            bluetoothAvailable = bluetoothAvailable,
            proxyReady = proxyReady,
            isRegistered = isRegistered,
            isConnected = isConnected,
            onMakeDiscoverable = onMakeDiscoverable,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        TriggerRow(onLeftTrigger = onLeftTrigger, onRightTrigger = onRightTrigger, buttonMode = triggerButtonMode)
        Spacer(Modifier.height(4.dp))
        BumperRow(onPress = onPress, onRelease = onRelease)

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DPad(onPress = onPress, onRelease = onRelease)
            ActionButtons(onPress = onPress, onRelease = onRelease)
        }

        Spacer(Modifier.height(4.dp))

        MenuButtons(onPress = onPress, onRelease = onRelease)

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            VirtualJoystick(
                onAxisChanged = { x, y -> viewModel.setLeftAxis(x, y) },
                size = 130.dp,
                label = stringResource(R.string.label_left_stick)
            )
            VirtualJoystick(
                onAxisChanged = { x, y -> viewModel.setRightAxis(x, y) },
                size = 130.dp,
                label = stringResource(R.string.label_right_stick),
                thumbColor = Color(0xFFFF9800)
            )
        }

        Spacer(Modifier.height(2.dp))
        StickClickRow(onPress = onPress, onRelease = onRelease)

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

        Spacer(Modifier.height(12.dp))
    }
}

// ── Landscape ───────────────────────────────────────────────────────────────

@Composable
private fun LandscapeLayout(
    viewModel: GamepadViewModel,
    permissionsGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    bluetoothAvailable: Boolean,
    proxyReady: Boolean,
    isRegistered: Boolean,
    isConnected: Boolean,
    connectedDevice: String?,
    reportsSent: Long,
    logMessages: List<String>,
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    onLeftTrigger: (Float) -> Unit,
    onRightTrigger: (Float) -> Unit,
    triggerButtonMode: Boolean,
    debugLogVisible: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // ── Top bar ─────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CompactStatusDots(bluetoothAvailable, isRegistered, isConnected, connectedDevice, reportsSent)

            ConnectionButtons(
                viewModel = viewModel,
                permissionsGranted = permissionsGranted,
                onRequestPermissions = onRequestPermissions,
                bluetoothAvailable = bluetoothAvailable,
                proxyReady = proxyReady,
                isRegistered = isRegistered,
                isConnected = isConnected,
                onMakeDiscoverable = onMakeDiscoverable
            )

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

        // ── Main controls ───────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left column: LT, LB, Left stick, L3
            Column(
                modifier = Modifier.weight(0.22f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                GamepadTrigger("LT", onValueChanged = onLeftTrigger, buttonMode = triggerButtonMode, width = 78.dp, height = 40.dp)
                ControlButton("LB", GamepadButtons.LB, Color(0xFFFF9800), onPress, onRelease, width = 78.dp, height = 32.dp, fontSize = 12)
                VirtualJoystick(
                    onAxisChanged = { x, y -> viewModel.setLeftAxis(x, y) },
                    size = 100.dp,
                    label = stringResource(R.string.label_left_stick)
                )
                ControlButton("L3", GamepadButtons.L3, Color(0xFF546E7A), onPress, onRelease, width = 50.dp, height = 26.dp, fontSize = 10)
            }

            // Center: DPad + Menu + ABXY
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
                    DPad(onPress = onPress, onRelease = onRelease, btnWidth = 42.dp, btnHeight = 36.dp, offset = 38.dp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MenuButtons(onPress = onPress, onRelease = onRelease)
                    }
                    ActionButtons(onPress = onPress, onRelease = onRelease, buttonSize = 46.dp, spacing = 32.dp)
                }
            }

            // Right column: RT, RB, Right stick, R3
            Column(
                modifier = Modifier.weight(0.22f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                GamepadTrigger("RT", onValueChanged = onRightTrigger, buttonMode = triggerButtonMode, width = 78.dp, height = 40.dp)
                ControlButton("RB", GamepadButtons.RB, Color(0xFFFF9800), onPress, onRelease, width = 78.dp, height = 32.dp, fontSize = 12)
                VirtualJoystick(
                    onAxisChanged = { x, y -> viewModel.setRightAxis(x, y) },
                    size = 100.dp,
                    label = stringResource(R.string.label_right_stick),
                    thumbColor = Color(0xFFFF9800)
                )
                ControlButton("R3", GamepadButtons.R3, Color(0xFF546E7A), onPress, onRelease, width = 50.dp, height = 26.dp, fontSize = 10)
            }
        }

        // ── Bottom centered debug console ────────────────────────
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

// ── Shared composables ──────────────────────────────────────────────────────

@Composable
private fun CompactStatusDots(
    bt: Boolean,
    reg: Boolean,
    conn: Boolean,
    deviceName: String?,
    reports: Long
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val green = Color(0xFF4CAF50)
        val red = Color(0xFFB71C1C)
        StatusDot(bt, green, red)
        Spacer(Modifier.width(4.dp))
        StatusDot(reg, green, red)
        Spacer(Modifier.width(4.dp))
        StatusDot(conn, green, red)
        Spacer(Modifier.width(6.dp))
        val info = buildString {
            if (conn && deviceName != null) append(deviceName).append(" | ")
            append(reports)
        }
        Text(info, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatusDot(active: Boolean, activeColor: Color, inactiveColor: Color) {
    val color by animateColorAsState(if (active) activeColor else inactiveColor, label = "dot")
    Box(Modifier.size(8.dp).background(color, CircleShape))
}

@Composable
private fun ConnectionButtons(
    viewModel: GamepadViewModel,
    permissionsGranted: Boolean,
    onRequestPermissions: () -> Unit,
    bluetoothAvailable: Boolean,
    proxyReady: Boolean,
    isRegistered: Boolean,
    isConnected: Boolean,
    onMakeDiscoverable: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
        }
        if (bluetoothAvailable && proxyReady && !isRegistered) {
            Button(
                onClick = { viewModel.registerHidApp() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) { Text(stringResource(R.string.btn_register_hid), fontSize = 11.sp) }
        }
        if (isRegistered && !isConnected) {
            Button(
                onClick = onMakeDiscoverable,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
            ) { Text(stringResource(R.string.btn_discoverable), fontSize = 11.sp) }
        }
        if (isRegistered) {
            OutlinedButton(onClick = { viewModel.unregisterHidApp() }) {
                Text(stringResource(R.string.btn_unregister), fontSize = 11.sp)
            }
        }
    }
}

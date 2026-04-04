package com.example.superiorcontroller.ui

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.superiorcontroller.ui.components.ActionButtons
import com.example.superiorcontroller.ui.components.DPad
import com.example.superiorcontroller.ui.components.DebugLog
import com.example.superiorcontroller.ui.components.MenuButtons
import com.example.superiorcontroller.ui.components.ShoulderButtons
import com.example.superiorcontroller.ui.components.StatusPanel
import com.example.superiorcontroller.ui.components.VirtualJoystick
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

    val context = LocalContext.current
    val discoverableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_CANCELED) {
            // Discoverable enabled
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Title ───────────────────────────────────────────────────
        Text(
            text = "Superior Controller",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── Status ──────────────────────────────────────────────────
        StatusPanel(
            bluetoothAvailable = bluetoothAvailable,
            isRegistered = isRegistered,
            isConnected = isConnected,
            connectedDeviceName = connectedDevice,
            reportsSent = reportsSent
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── Control buttons ─────────────────────────────────────────
        if (!permissionsGranted) {
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Bluetooth Permissions")
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!bluetoothAvailable) {
                    Button(
                        onClick = { viewModel.initializeBluetooth() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Initialize BT", fontSize = 12.sp)
                    }
                }

                if (bluetoothAvailable && proxyReady && !isRegistered) {
                    Button(
                        onClick = { viewModel.registerHidApp() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        )
                    ) {
                        Text("Register HID", fontSize = 12.sp)
                    }
                }

                if (isRegistered && !isConnected) {
                    Button(
                        onClick = {
                            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                            }
                            discoverableLauncher.launch(intent)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800)
                        )
                    ) {
                        Text("Discoverable", fontSize = 12.sp)
                    }
                }

                if (isRegistered) {
                    OutlinedButton(
                        onClick = { viewModel.unregisterHidApp() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Unregister", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Shoulder buttons (L1 / R1) ──────────────────────────────
        ShoulderButtons(
            onPress = { viewModel.pressButton(it) },
            onRelease = { viewModel.releaseButton(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── DPad + Action buttons ───────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DPad(
                onPress = { viewModel.pressButton(it) },
                onRelease = { viewModel.releaseButton(it) }
            )
            ActionButtons(
                onPress = { viewModel.pressButton(it) },
                onRelease = { viewModel.releaseButton(it) }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Start / Select ──────────────────────────────────────────
        MenuButtons(
            onPress = { viewModel.pressButton(it) },
            onRelease = { viewModel.releaseButton(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── Virtual joystick ────────────────────────────────────────
        VirtualJoystick(
            onAxisChanged = { x, y -> viewModel.setAxis(x, y) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── Neutral / Clear ─────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.sendNeutralReport() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF455A64))
            ) {
                Text("Send Neutral", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { viewModel.clearLog() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear Log", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Debug log ───────────────────────────────────────────────
        DebugLog(messages = logMessages)

        Spacer(modifier = Modifier.height(16.dp))
    }
}

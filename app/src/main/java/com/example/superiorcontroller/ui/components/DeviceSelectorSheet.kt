package com.example.superiorcontroller.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.superiorcontroller.R
import com.example.superiorcontroller.bluetooth.KnownDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSelectorSheet(
    isConnected: Boolean,
    isRegistered: Boolean,
    connectedAddress: String?,
    connectedDeviceName: String?,
    knownDevices: List<KnownDevice>,
    bondedDevices: List<Pair<String, String>>,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onRemoveKnown: (String) -> Unit,
    onRenameKnown: (String, String) -> Unit,
    onMakeDiscoverable: () -> Unit,
    onUnregister: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(
                text = stringResource(R.string.devices_title),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(12.dp))

            if (isConnected && connectedDeviceName != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(connectedDeviceName, fontWeight = FontWeight.Bold)
                            Text(
                                stringResource(R.string.devices_connected_label),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        OutlinedButton(onClick = onDisconnect) {
                            Text(stringResource(R.string.btn_disconnect), fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (knownDevices.isNotEmpty()) {
                Text(stringResource(R.string.devices_saved), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(knownDevices, key = { it.address }) { device ->
                        DeviceRow(
                            name = device.displayName,
                            address = device.address,
                            isCurrent = device.address == connectedAddress,
                            onConnect = { onConnect(device.address) },
                            onRemove = { onRemoveKnown(device.address) },
                            onRename = { alias -> onRenameKnown(device.address, alias) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (bondedDevices.isNotEmpty()) {
                Text(stringResource(R.string.devices_paired), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                bondedDevices.forEach { (name, address) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConnect(address) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, fontSize = 14.sp)
                            Text(address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { onConnect(address) }) {
                            Text(stringResource(R.string.devices_connect))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (knownDevices.isEmpty() && bondedDevices.isEmpty()) {
                Text(
                    stringResource(R.string.devices_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isRegistered) {
                    Button(
                        onClick = onMakeDiscoverable,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                    ) { Text(stringResource(R.string.btn_discoverable), fontSize = 12.sp) }
                    OutlinedButton(
                        onClick = onUnregister,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.btn_unregister), fontSize = 12.sp) }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DeviceRow(
    name: String,
    address: String,
    isCurrent: Boolean,
    onConnect: () -> Unit,
    onRemove: () -> Unit,
    onRename: (String) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var alias by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 14.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
            Text(address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (editing) {
            OutlinedTextField(
                value = alias,
                onValueChange = { alias = it },
                modifier = Modifier.width(120.dp),
                placeholder = { Text(stringResource(R.string.devices_alias_hint), fontSize = 12.sp) },
                singleLine = true
            )
            TextButton(onClick = { onRename(alias); editing = false }) {
                Text(stringResource(R.string.dialog_ok))
            }
        } else {
            if (!isCurrent) {
                TextButton(onClick = onConnect) { Text(stringResource(R.string.devices_connect), fontSize = 12.sp) }
            }
            TextButton(onClick = { editing = true }) { Text(stringResource(R.string.devices_rename), fontSize = 12.sp) }
            TextButton(onClick = onRemove) { Text(stringResource(R.string.devices_remove), fontSize = 12.sp, color = Color.Red) }
        }
    }
}

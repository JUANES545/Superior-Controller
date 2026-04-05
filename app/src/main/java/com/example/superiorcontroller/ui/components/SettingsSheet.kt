package com.example.superiorcontroller.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.superiorcontroller.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    hapticsEnabled: Boolean,
    soundEnabled: Boolean,
    triggerMode: String,
    debugLogVisible: Boolean,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onTriggerModeChange: (String) -> Unit,
    onToggleDebugLog: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(16.dp))

            SettingsToggle(
                title = stringResource(R.string.settings_haptics_title),
                description = stringResource(R.string.settings_haptics_desc),
                checked = hapticsEnabled,
                onCheckedChange = onToggleHaptics
            )

            SettingsToggle(
                title = stringResource(R.string.settings_sound_title),
                description = stringResource(R.string.settings_sound_desc),
                checked = soundEnabled,
                onCheckedChange = onToggleSound
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.settings_trigger_mode_title),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(R.string.settings_trigger_mode_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = triggerMode == "analog",
                    onClick = { onTriggerModeChange("analog") },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text(stringResource(R.string.settings_trigger_analog)) }
                SegmentedButton(
                    selected = triggerMode == "button",
                    onClick = { onTriggerModeChange("button") },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text(stringResource(R.string.settings_trigger_button)) }
            }

            Spacer(Modifier.height(12.dp))

            SettingsToggle(
                title = stringResource(R.string.settings_debug_log_title),
                description = stringResource(R.string.settings_debug_log_desc),
                checked = debugLogVisible,
                onCheckedChange = onToggleDebugLog
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

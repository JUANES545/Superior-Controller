package com.example.superiorcontroller.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt
import com.example.superiorcontroller.R
import com.example.superiorcontroller.input.InputQuantizer
import com.example.superiorcontroller.input.TemporalQuantizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    controllerProfile: String,
    hapticsEnabled: Boolean,
    hapticsIntensity: String,
    soundEnabled: Boolean,
    soundStyle: String,
    soundVolume: Float,
    triggerMode: String,
    debugLogVisible: Boolean,
    debugLogOverlay: Boolean,
    digitalRecording: Boolean,
    assistLeftMode: String,
    assistRightMode: String,
    assistLeftTempo: String,
    assistRightTempo: String,
    profileWarningSuppressed: Boolean,
    onProfileChange: (String) -> Unit,
    onProfileWarningSuppressed: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onHapticsIntensityChange: (String) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onSoundStyleChange: (String) -> Unit,
    onSoundVolumeChange: (Float) -> Unit,
    onTriggerModeChange: (String) -> Unit,
    onToggleDebugLog: (Boolean) -> Unit,
    onToggleDebugOverlay: (Boolean) -> Unit,
    onToggleDigitalRecording: (Boolean) -> Unit,
    onAssistLeftModeChange: (String) -> Unit,
    onAssistRightModeChange: (String) -> Unit,
    onAssistLeftTempoChange: (String) -> Unit,
    onAssistRightTempoChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showAssistConfig by remember { mutableStateOf(false) }
    var showHapticsConfig by remember { mutableStateOf(false) }
    var showSoundConfig by remember { mutableStateOf(false) }
    var pendingProfile by remember { mutableStateOf<String?>(null) }
    var showAbout by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_profile_title),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(R.string.settings_profile_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = controllerProfile == "xbox",
                    onClick = {
                        if (controllerProfile != "xbox") {
                            if (profileWarningSuppressed) onProfileChange("xbox")
                            else pendingProfile = "xbox"
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Xbox") }
                SegmentedButton(
                    selected = controllerProfile == "playstation",
                    onClick = {
                        if (controllerProfile != "playstation") {
                            if (profileWarningSuppressed) onProfileChange("playstation")
                            else pendingProfile = "playstation"
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("PlayStation") }
            }

            Spacer(Modifier.height(12.dp))

            SettingsToggleWithGearSimple(
                title = stringResource(R.string.settings_haptics_title),
                description = stringResource(R.string.settings_haptics_desc),
                checked = hapticsEnabled,
                gearEnabled = hapticsEnabled,
                onCheckedChange = onToggleHaptics,
                onGearClick = { showHapticsConfig = true }
            )

            SettingsToggleWithGearSimple(
                title = stringResource(R.string.settings_sound_title),
                description = stringResource(R.string.settings_sound_desc),
                checked = soundEnabled,
                gearEnabled = soundEnabled,
                onCheckedChange = onToggleSound,
                onGearClick = { showSoundConfig = true }
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

            SettingsToggleWithGear(
                title = stringResource(R.string.settings_digital_rec_title),
                description = stringResource(R.string.settings_digital_rec_desc),
                infoText = stringResource(R.string.settings_digital_rec_info),
                checked = digitalRecording,
                gearEnabled = digitalRecording,
                onCheckedChange = onToggleDigitalRecording,
                onGearClick = { showAssistConfig = true }
            )

            Spacer(Modifier.height(12.dp))

            SettingsToggle(
                title = stringResource(R.string.settings_debug_log_title),
                description = stringResource(R.string.settings_debug_log_desc),
                checked = debugLogVisible,
                onCheckedChange = onToggleDebugLog
            )

            AnimatedVisibility(visible = debugLogVisible) {
                Column(modifier = Modifier.padding(start = 24.dp)) {
                    SettingsToggle(
                        title = stringResource(R.string.settings_debug_overlay_title),
                        description = stringResource(R.string.settings_debug_overlay_desc),
                        checked = debugLogOverlay,
                        onCheckedChange = onToggleDebugOverlay
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAbout = true }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_about),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                val ctx = LocalContext.current
                val versionName = remember {
                    try {
                        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
                    } catch (_: Exception) { "" }
                }
                Text(
                    text = versionName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }

    if (showAssistConfig) {
        AssistConfigDialog(
            leftMode = assistLeftMode,
            rightMode = assistRightMode,
            leftTempo = assistLeftTempo,
            rightTempo = assistRightTempo,
            onLeftModeChange = onAssistLeftModeChange,
            onRightModeChange = onAssistRightModeChange,
            onLeftTempoChange = onAssistLeftTempoChange,
            onRightTempoChange = onAssistRightTempoChange,
            onDismiss = { showAssistConfig = false }
        )
    }

    if (showHapticsConfig) {
        HapticsConfigDialog(
            intensity = hapticsIntensity,
            onIntensityChange = onHapticsIntensityChange,
            onDismiss = { showHapticsConfig = false }
        )
    }

    if (showSoundConfig) {
        SoundConfigDialog(
            style = soundStyle,
            volume = soundVolume,
            onStyleChange = onSoundStyleChange,
            onVolumeChange = onSoundVolumeChange,
            onDismiss = { showSoundConfig = false }
        )
    }

    pendingProfile?.let { newProfile ->
        ProfileChangeDialog(
            onConfirm = { dontShowAgain ->
                if (dontShowAgain) onProfileWarningSuppressed(true)
                onProfileChange(newProfile)
                pendingProfile = null
            },
            onDismiss = { pendingProfile = null }
        )
    }
}

@Composable
private fun AssistConfigDialog(
    leftMode: String,
    rightMode: String,
    leftTempo: String,
    rightTempo: String,
    onLeftModeChange: (String) -> Unit,
    onRightModeChange: (String) -> Unit,
    onLeftTempoChange: (String) -> Unit,
    onRightTempoChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.widthIn(max = 400.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.assist_config_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(20.dp))

                // ── Left stick ──
                Text(
                    text = stringResource(R.string.assist_left_stick_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.assist_section_direction),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = leftMode == InputQuantizer.MODE_8DIR,
                        onClick = { onLeftModeChange(InputQuantizer.MODE_8DIR) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text(stringResource(R.string.assist_left_8dir_label), fontSize = 12.sp)
                    }
                    SegmentedButton(
                        selected = leftMode == InputQuantizer.MODE_4DIR,
                        onClick = { onLeftModeChange(InputQuantizer.MODE_4DIR) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text(stringResource(R.string.assist_left_4dir_label), fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.assist_section_timing),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = leftTempo == TemporalQuantizer.MODE_FREE,
                        onClick = { onLeftTempoChange(TemporalQuantizer.MODE_FREE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {}
                    ) {
                        Text(stringResource(R.string.tempo_free), fontSize = 12.sp)
                    }
                    SegmentedButton(
                        selected = leftTempo == TemporalQuantizer.MODE_GRID,
                        onClick = { onLeftTempoChange(TemporalQuantizer.MODE_GRID) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {}
                    ) {
                        Text(stringResource(R.string.tempo_grid), fontSize = 12.sp)
                    }
                }

                val leftTempoHint = when (leftTempo) {
                    TemporalQuantizer.MODE_FREE -> stringResource(R.string.tempo_hint_free)
                    TemporalQuantizer.MODE_GRID -> stringResource(R.string.tempo_hint_left_grid)
                    else -> null
                }
                if (leftTempoHint != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(leftTempoHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                }
                if (leftTempo == TemporalQuantizer.MODE_GRID) {
                    RecommendedBadge()
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(Modifier.height(16.dp))

                // ── Right stick ──
                Text(
                    text = stringResource(R.string.assist_right_stick_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.assist_section_direction),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val modes = listOf(
                        InputQuantizer.MODE_8DIR to R.string.assist_mode_8dir,
                        InputQuantizer.MODE_4DIR to R.string.assist_mode_4dir,
                        InputQuantizer.MODE_STABLE75 to R.string.assist_mode_stable75,
                        InputQuantizer.MODE_STABLE50 to R.string.assist_mode_stable50
                    )
                    modes.forEachIndexed { idx, (mode, labelRes) ->
                        SegmentedButton(
                            selected = rightMode == mode,
                            onClick = { onRightModeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = modes.size),
                            icon = {}
                        ) {
                            Text(
                                stringResource(labelRes),
                                fontSize = 12.sp,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                val rightHint = when (rightMode) {
                    InputQuantizer.MODE_8DIR -> stringResource(R.string.assist_hint_8dir)
                    InputQuantizer.MODE_4DIR -> stringResource(R.string.assist_hint_4dir)
                    InputQuantizer.MODE_STABLE75 -> stringResource(R.string.assist_hint_stable75)
                    InputQuantizer.MODE_STABLE50 -> stringResource(R.string.assist_hint_stable50)
                    else -> null
                }
                if (rightHint != null) {
                    Text(rightHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.assist_section_timing),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val tempoModes = listOf(
                        TemporalQuantizer.MODE_FREE to R.string.tempo_free,
                        TemporalQuantizer.MODE_GRID to R.string.tempo_grid,
                        TemporalQuantizer.MODE_PULSE to R.string.tempo_pulse
                    )
                    tempoModes.forEachIndexed { idx, (mode, labelRes) ->
                        SegmentedButton(
                            selected = rightTempo == mode,
                            onClick = { onRightTempoChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = tempoModes.size),
                            icon = {}
                        ) {
                            Text(stringResource(labelRes), fontSize = 12.sp, maxLines = 1, textAlign = TextAlign.Center)
                        }
                    }
                }

                val rightTempoHint = when (rightTempo) {
                    TemporalQuantizer.MODE_FREE -> stringResource(R.string.tempo_hint_free)
                    TemporalQuantizer.MODE_GRID -> stringResource(R.string.tempo_hint_right_grid)
                    TemporalQuantizer.MODE_PULSE -> stringResource(R.string.tempo_hint_pulse)
                    else -> null
                }
                if (rightTempoHint != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(rightTempoHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                }
                if (rightTempo == TemporalQuantizer.MODE_PULSE) {
                    RecommendedBadge()
                }

                Spacer(Modifier.height(24.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.dialog_ok))
                }
            }
        }
    }
}

@Composable
private fun ProfileChangeDialog(
    onConfirm: (dontShowAgain: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var dontShowAgain by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.widthIn(max = 400.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.profile_change_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.profile_change_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { dontShowAgain = !dontShowAgain }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(checked = dontShowAgain, onCheckedChange = { dontShowAgain = it })
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.macro_warning_suppress),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.dialog_cancel), textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = { onConfirm(dontShowAgain) }) {
                        Text(stringResource(R.string.profile_change_confirm), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val pkgInfo = remember {
        try { ctx.packageManager.getPackageInfo(ctx.packageName, 0) } catch (_: Exception) { null }
    }
    val versionName = pkgInfo?.versionName ?: "—"
    @Suppress("DEPRECATION")
    val versionCode = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pkgInfo?.longVersionCode?.toString() ?: "—"
        else pkgInfo?.versionCode?.toString() ?: "—"
    }
    val isRooted = remember { checkRoot() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.widthIn(max = 400.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "v$versionName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))
                AboutSectionTitle(stringResource(R.string.about_section_app))
                Spacer(Modifier.height(6.dp))
                AboutRow(stringResource(R.string.about_version), versionName)
                AboutRow(stringResource(R.string.about_build), versionCode)
                AboutRow(stringResource(R.string.about_package), ctx.packageName)
                AboutRow(stringResource(R.string.about_developer), stringResource(R.string.about_developer_value))

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(Modifier.height(12.dp))

                AboutSectionTitle(stringResource(R.string.about_section_device))
                Spacer(Modifier.height(6.dp))
                AboutRow(
                    label = "Root",
                    value = if (isRooted) stringResource(R.string.about_root_yes) else stringResource(R.string.about_root_no),
                    valueColor = if (isRooted) Color(0xFF4CAF50) else Color(0xFFEF5350)
                )
                AboutRow(stringResource(R.string.about_brand), Build.MANUFACTURER.replaceFirstChar { it.uppercase() })
                AboutRow(stringResource(R.string.about_model), Build.MODEL)
                AboutRow(stringResource(R.string.about_android), "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")

                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.dialog_ok))
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleWithGearSimple(
    title: String,
    description: String,
    checked: Boolean,
    gearEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onGearClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (gearEnabled) {
            IconButton(onClick = onGearClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun HapticsConfigDialog(
    intensity: String,
    onIntensityChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 6.dp, modifier = Modifier.widthIn(max = 360.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(stringResource(R.string.haptics_config_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.haptics_intensity_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf(
                        ButtonHaptics.INTENSITY_SOFT to R.string.haptics_soft,
                        ButtonHaptics.INTENSITY_MEDIUM to R.string.haptics_medium,
                        ButtonHaptics.INTENSITY_STRONG to R.string.haptics_strong
                    )
                    options.forEachIndexed { idx, (mode, labelRes) ->
                        SegmentedButton(
                            selected = intensity == mode,
                            onClick = { onIntensityChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = options.size),
                            icon = {}
                        ) { Text(stringResource(labelRes), fontSize = 12.sp) }
                    }
                }
                val hint = when (intensity) {
                    ButtonHaptics.INTENSITY_SOFT -> stringResource(R.string.haptics_hint_soft)
                    ButtonHaptics.INTENSITY_STRONG -> stringResource(R.string.haptics_hint_strong)
                    else -> stringResource(R.string.haptics_hint_medium)
                }
                Spacer(Modifier.height(6.dp))
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                Spacer(Modifier.height(20.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.dialog_ok))
                }
            }
        }
    }
}

@Composable
private fun SoundConfigDialog(
    style: String,
    volume: Float,
    onStyleChange: (String) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var localVolume by remember { mutableFloatStateOf(volume) }
    var lastPreviewMs by remember { mutableStateOf(0L) }

    fun tryPreview(s: String = style, v: Float = localVolume) {
        val now = System.currentTimeMillis()
        if (now - lastPreviewMs < 150) return
        lastPreviewMs = now
        try { ButtonSoundPlayer.playPreview(s, v) } catch (_: Exception) {}
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 6.dp, modifier = Modifier.widthIn(max = 360.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(stringResource(R.string.sound_config_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.sound_style_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf(
                        ButtonSoundPlayer.STYLE_SOFT to R.string.sound_soft,
                        ButtonSoundPlayer.STYLE_SHORT to R.string.sound_short,
                        ButtonSoundPlayer.STYLE_ARCADE to R.string.sound_arcade,
                        ButtonSoundPlayer.STYLE_MECHANICAL to R.string.sound_mechanical
                    )
                    options.forEachIndexed { idx, (mode, labelRes) ->
                        SegmentedButton(
                            selected = style == mode,
                            onClick = {
                                onStyleChange(mode)
                                tryPreview(s = mode)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = options.size),
                            icon = {}
                        ) { Text(stringResource(labelRes), fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.Center) }
                    }
                }
                val hint = when (style) {
                    ButtonSoundPlayer.STYLE_SOFT -> stringResource(R.string.sound_hint_soft)
                    ButtonSoundPlayer.STYLE_SHORT -> stringResource(R.string.sound_hint_short)
                    ButtonSoundPlayer.STYLE_ARCADE -> stringResource(R.string.sound_hint_arcade)
                    ButtonSoundPlayer.STYLE_MECHANICAL -> stringResource(R.string.sound_hint_mechanical)
                    else -> null
                }
                if (hint != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                }

                Spacer(Modifier.height(18.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(Modifier.height(14.dp))

                val isSilent = localVolume == 0f
                val pct = (localVolume * 100).roundToInt()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.sound_volume_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (isSilent) stringResource(R.string.sound_silent) else "$pct%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSilent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val leftIcon = if (isSilent) "🔇" else "🔈"
                    Text(leftIcon, fontSize = 16.sp)
                    Slider(
                        value = localVolume,
                        onValueChange = { localVolume = it },
                        onValueChangeFinished = {
                            onVolumeChange(localVolume)
                            tryPreview(v = localVolume)
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = if (isSilent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            activeTrackColor = if (isSilent) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary
                        )
                    )
                    Text("🔊", fontSize = 16.sp)
                }

                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.dialog_ok))
                }
            }
        }
    }
}

@Composable
private fun RecommendedBadge() {
    Text(
        text = "★ ${stringResource(R.string.assist_recommended)}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(top = 2.dp)
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

private fun checkRoot(): Boolean {
    val paths = arrayOf(
        "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
        "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su",
        "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su"
    )
    if (paths.any { java.io.File(it).exists() }) return true
    return try {
        Runtime.getRuntime().exec(arrayOf("which", "su")).inputStream.bufferedReader().readLine() != null
    } catch (_: Exception) { false }
}

@Composable
private fun AboutSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun AboutRow(label: String, value: String, valueColor: Color? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f)
        )
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

@Composable
private fun SettingsToggleWithGear(
    title: String,
    description: String,
    infoText: String,
    checked: Boolean,
    gearEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onGearClick: () -> Unit
) {
    var showInfo by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = { showInfo = true },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = "Info",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (gearEnabled) {
            IconButton(onClick = onGearClick) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Advanced settings",
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(title) },
            text = { Text(infoText) },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) {
                    Text(stringResource(R.string.dialog_ok))
                }
            }
        )
    }
}

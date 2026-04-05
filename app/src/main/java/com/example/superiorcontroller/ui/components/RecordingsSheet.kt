package com.example.superiorcontroller.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.superiorcontroller.R
import com.example.superiorcontroller.recording.PlaybackProgress
import com.example.superiorcontroller.recording.PlaybackStatus
import com.example.superiorcontroller.recording.RecordingMeta
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsSheet(
    recordings: List<RecordingMeta>,
    playbackProgress: PlaybackProgress,
    onPlay: (String) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onDelete: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
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
                stringResource(R.string.recordings_title),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(12.dp))

            if (recordings.isEmpty()) {
                Text(
                    stringResource(R.string.recordings_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                recordings.forEachIndexed { index, rec ->
                    RecordingRow(
                        recording = rec,
                        playbackProgress = if (playbackProgress.recordingId == rec.id) playbackProgress else null,
                        onPlay = { onPlay(rec.id) },
                        onPause = onPause,
                        onResume = onResume,
                        onStop = onStop,
                        onDelete = { onDelete(rec.id) },
                        onRename = { newName -> onRename(rec.id, newName) }
                    )
                    if (index < recordings.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RecordingRow(
    recording: RecordingMeta,
    playbackProgress: PlaybackProgress?,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(recording.name) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val durationSec = recording.durationMs / 1000

    val isActive = playbackProgress != null && playbackProgress.status != PlaybackStatus.IDLE
    val isPlaying = playbackProgress?.status == PlaybackStatus.PLAYING
    val isPaused = playbackProgress?.status == PlaybackStatus.PAUSED

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Play/Pause button (prominent)
            if (isPlaying) {
                FilledIconButton(
                    onClick = onPause,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFFFF9800)
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(4.dp, 14.dp).background(Color.White, RoundedCornerShape(1.dp)))
                        Box(Modifier.size(4.dp, 14.dp).background(Color.White, RoundedCornerShape(1.dp)))
                    }
                }
            } else if (isPaused) {
                FilledIconButton(
                    onClick = onResume,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = Color.White)
                }
            } else {
                FilledIconButton(
                    onClick = onPlay,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                }
            }

            Spacer(Modifier.width(12.dp))

            // Name + metadata
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    recording.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row {
                    Text(
                        "${dateFormat.format(Date(recording.createdAt))} · ${durationSec}s · ",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${recording.eventCount} events",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Action icons: stop during playback, edit/delete when idle
            if (isActive) {
                IconButton(onClick = onStop, modifier = Modifier.size(36.dp)) {
                    Box(
                        Modifier
                            .size(14.dp)
                            .background(Color(0xFFD32F2F), RoundedCornerShape(2.dp))
                    )
                }
            } else {
                IconButton(onClick = { showRename = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.recording_rename),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.recording_delete),
                        tint = Color(0xFFD32F2F).copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Progress bar — always visible, time-based smooth animation
        val totalMs = recording.durationMs
        val animatable = remember { Animatable(0f) }

        LaunchedEffect(isPlaying, isPaused) {
            when {
                isPlaying -> {
                    val remaining = ((1f - animatable.value) * totalMs).toLong()
                    animatable.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = remaining.toInt().coerceAtLeast(0),
                            easing = LinearEasing
                        )
                    )
                }
                !isActive -> animatable.snapTo(0f)
            }
        }

        Spacer(Modifier.height(6.dp))

        if (recording.eventCount > 0) {
            val dimColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
            val litColor = MaterialTheme.colorScheme.primary
            val progress = animatable.value
            val count = recording.eventCount
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 52.dp, end = 50.dp)
                    .height(4.dp)
            ) {
                val w = size.width
                for (i in 0 until count) {
                    val fraction = if (count > 1) i / (count - 1).toFloat() else 0.5f
                    val x = w * fraction
                    val lit = progress > 0f && fraction <= progress
                    drawCircle(
                        color = if (lit) litColor else dimColor,
                        radius = if (lit) 2f else 1.5f,
                        center = androidx.compose.ui.geometry.Offset(x, size.height / 2f)
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 52.dp)
        ) {
            val barColor = when {
                isPaused -> Color(0xFFFF9800)
                isPlaying -> Color(0xFF4CAF50)
                else -> MaterialTheme.colorScheme.surfaceContainerHighest
            }
            LinearProgressIndicator(
                progress = { animatable.value },
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            Spacer(Modifier.width(8.dp))
            val elapsedMs = (animatable.value * totalMs).toLong()
            Text(
                text = formatMs(elapsedMs) + " / " + formatMs(totalMs),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.recording_delete)) },
            text = { Text(stringResource(R.string.recording_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text(stringResource(R.string.dialog_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text(stringResource(R.string.recording_rename)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { onRename(newName); showRename = false }) {
                    Text(stringResource(R.string.dialog_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}

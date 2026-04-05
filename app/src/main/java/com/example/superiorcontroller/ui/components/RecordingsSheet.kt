package com.example.superiorcontroller.ui.components

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
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
                LazyColumn(modifier = Modifier.height(400.dp)) {
                    items(recordings, key = { it.id }) { rec ->
                        RecordingRow(
                            recording = rec,
                            isPlaying = playbackProgress.recordingId == rec.id &&
                                    playbackProgress.status != PlaybackStatus.IDLE,
                            isPaused = playbackProgress.recordingId == rec.id &&
                                    playbackProgress.status == PlaybackStatus.PAUSED,
                            onPlay = { onPlay(rec.id) },
                            onPause = onPause,
                            onResume = onResume,
                            onStop = onStop,
                            onDelete = { onDelete(rec.id) },
                            onRename = { newName -> onRename(rec.id, newName) }
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
    isPlaying: Boolean,
    isPaused: Boolean,
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(recording.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    "${dateFormat.format(Date(recording.createdAt))} · ${durationSec}s · ${recording.eventCount} events",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (isPlaying && !isPaused) {
                OutlinedButton(onClick = onPause) { Text("Pause", fontSize = 11.sp) }
                OutlinedButton(onClick = onStop) { Text("Stop", fontSize = 11.sp) }
            } else if (isPaused) {
                OutlinedButton(onClick = onResume) { Text("Resume", fontSize = 11.sp) }
                OutlinedButton(onClick = onStop) { Text("Stop", fontSize = 11.sp) }
            } else {
                Button(onClick = onPlay) { Text("Play", fontSize = 11.sp) }
            }
            TextButton(onClick = { showRename = true }) {
                Text(stringResource(R.string.recording_rename), fontSize = 11.sp)
            }
            TextButton(onClick = { showDeleteConfirm = true }) {
                Text(stringResource(R.string.recording_delete), fontSize = 11.sp, color = Color.Red)
            }
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

package com.example.superiorcontroller.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RecordControls(
    isRecording: Boolean,
    elapsedMs: Long,
    hasRecordings: Boolean,
    onToggleRecord: () -> Unit,
    onOpenRecordings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FloatingActionButton(
            onClick = onToggleRecord,
            containerColor = if (isRecording) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape
        ) {
            if (isRecording) {
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White)
                )
            } else {
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD32F2F))
                )
            }
        }

        if (isRecording) {
            Text(
                text = formatElapsed(elapsedMs),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD32F2F)
            )
        }

        if (hasRecordings && !isRecording) {
            SmallFloatingActionButton(
                onClick = onOpenRecordings,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Text("REC", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun formatElapsed(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = (ms % 1000) / 100
    return "%d:%02d.%d".format(minutes, seconds, millis)
}

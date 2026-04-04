package com.example.superiorcontroller.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DebugLog(
    messages: List<String>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "DEBUG LOG",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp, max = 160.dp)
                .background(
                    color = Color(0xFF1A1A1A),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
        ) {
            items(messages) { message ->
                val color = when {
                    "ERROR" in message -> Color(0xFFF44336)
                    "connected" in message.lowercase() -> Color(0xFF4CAF50)
                    "registered" in message.lowercase() -> Color(0xFF2196F3)
                    else -> Color(0xFFBDBDBD)
                }
                Text(
                    text = message,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = color,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.superiorcontroller.R

@Composable
fun DebugLog(
    messages: List<String>,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 160.dp,
    minHeight: Dp = 60.dp
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.debug_log_title),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight, max = maxHeight)
                .background(Color(0xFF0D1117), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            items(messages) { message ->
                val color = when {
                    "ERROR" in message -> Color(0xFFF44336)
                    "connected" in message.lowercase() -> Color(0xFF4CAF50)
                    "registered" in message.lowercase() -> Color(0xFF42A5F5)
                    "PRESS" in message -> Color(0xFF00E5FF)
                    else -> Color(0xFFBDBDBD)
                }
                Text(
                    text = message,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = color,
                    lineHeight = 13.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}

package com.example.superiorcontroller.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt

private const val OUTER_RADIUS_FRACTION = 0.45f
private const val INNER_RADIUS_FRACTION = 0.18f

@Composable
fun VirtualJoystick(
    onAxisChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    label: String = "",
    thumbColor: Color = Color(0xFF00E5FF)
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    var normalizedX by remember { mutableStateOf(0f) }
    var normalizedY by remember { mutableStateOf(0f) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Box(
            modifier = Modifier
                .size(size)
                .pointerInput(Unit) {
                    val outerR = this.size.width * OUTER_RADIUS_FRACTION
                    val innerR = this.size.width * INNER_RADIUS_FRACTION
                    val maxTravel = outerR - innerR
                    val cx = this.size.width / 2f
                    val cy = this.size.height / 2f

                    detectDragGestures(
                        onDragStart = { pos ->
                            val dx = pos.x - cx
                            val dy = pos.y - cy
                            val d = sqrt(dx * dx + dy * dy)
                            thumbOffset = if (d > maxTravel) Offset(dx * maxTravel / d, dy * maxTravel / d) else Offset(dx, dy)
                            normalizedX = (thumbOffset.x / maxTravel).coerceIn(-1f, 1f)
                            normalizedY = (thumbOffset.y / maxTravel).coerceIn(-1f, 1f)
                            onAxisChanged(normalizedX, normalizedY)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val n = thumbOffset + Offset(dragAmount.x, dragAmount.y)
                            val d = sqrt(n.x * n.x + n.y * n.y)
                            thumbOffset = if (d > maxTravel) Offset(n.x * maxTravel / d, n.y * maxTravel / d) else n
                            normalizedX = (thumbOffset.x / maxTravel).coerceIn(-1f, 1f)
                            normalizedY = (thumbOffset.y / maxTravel).coerceIn(-1f, 1f)
                            onAxisChanged(normalizedX, normalizedY)
                        },
                        onDragEnd = { thumbOffset = Offset.Zero; normalizedX = 0f; normalizedY = 0f; onAxisChanged(0f, 0f) },
                        onDragCancel = { thumbOffset = Offset.Zero; normalizedX = 0f; normalizedY = 0f; onAxisChanged(0f, 0f) }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val outerR = this.size.width * OUTER_RADIUS_FRACTION
                val innerR = this.size.width * INNER_RADIUS_FRACTION

                drawCircle(Color(0xFF37474F), outerR, center, style = Stroke(2.dp.toPx()))
                drawCircle(Color(0xFF263238), outerR * 0.5f, center, style = Stroke(1.dp.toPx()))
                drawLine(Color(0xFF263238), Offset(center.x, center.y - outerR), Offset(center.x, center.y + outerR), 1.dp.toPx())
                drawLine(Color(0xFF263238), Offset(center.x - outerR, center.y), Offset(center.x + outerR, center.y), 1.dp.toPx())
                drawCircle(thumbColor, innerR, center + thumbOffset, alpha = 0.9f)
            }
        }

        Text(
            text = "${"%.1f".format(normalizedX)}, ${"%.1f".format(normalizedY)}",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

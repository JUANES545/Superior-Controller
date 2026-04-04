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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt

private val JOYSTICK_SIZE = 160.dp
private const val OUTER_RADIUS_FRACTION = 0.45f
private const val INNER_RADIUS_FRACTION = 0.18f

/**
 * Virtual analog joystick.
 * [onAxisChanged] receives normalized values in -1..1 for both X and Y.
 */
@Composable
fun VirtualJoystick(
    onAxisChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    var normalizedX by remember { mutableStateOf(0f) }
    var normalizedY by remember { mutableStateOf(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(JOYSTICK_SIZE)
                .pointerInput(Unit) {
                    val outerRadius = size.width * OUTER_RADIUS_FRACTION
                    val innerRadius = size.width * INNER_RADIUS_FRACTION
                    val maxTravel = outerRadius - innerRadius
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f

                    detectDragGestures(
                        onDragStart = { startPos ->
                            val dx = startPos.x - centerX
                            val dy = startPos.y - centerY
                            val dist = sqrt(dx * dx + dy * dy)
                            thumbOffset = if (dist > maxTravel) {
                                Offset(dx * maxTravel / dist, dy * maxTravel / dist)
                            } else {
                                Offset(dx, dy)
                            }
                            normalizedX = (thumbOffset.x / maxTravel).coerceIn(-1f, 1f)
                            normalizedY = (thumbOffset.y / maxTravel).coerceIn(-1f, 1f)
                            onAxisChanged(normalizedX, normalizedY)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = thumbOffset + Offset(dragAmount.x, dragAmount.y)
                            val dist = sqrt(newOffset.x * newOffset.x + newOffset.y * newOffset.y)
                            thumbOffset = if (dist > maxTravel) {
                                Offset(
                                    newOffset.x * maxTravel / dist,
                                    newOffset.y * maxTravel / dist
                                )
                            } else {
                                newOffset
                            }
                            normalizedX = (thumbOffset.x / maxTravel).coerceIn(-1f, 1f)
                            normalizedY = (thumbOffset.y / maxTravel).coerceIn(-1f, 1f)
                            onAxisChanged(normalizedX, normalizedY)
                        },
                        onDragEnd = {
                            thumbOffset = Offset.Zero
                            normalizedX = 0f
                            normalizedY = 0f
                            onAxisChanged(0f, 0f)
                        },
                        onDragCancel = {
                            thumbOffset = Offset.Zero
                            normalizedX = 0f
                            normalizedY = 0f
                            onAxisChanged(0f, 0f)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerR = size.width * OUTER_RADIUS_FRACTION
                val innerR = size.width * INNER_RADIUS_FRACTION

                drawCircle(
                    color = Color(0xFF424242),
                    radius = outerR,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF333333),
                    radius = outerR * 0.5f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Crosshair
                drawLine(Color(0xFF333333), Offset(center.x, center.y - outerR), Offset(center.x, center.y + outerR), strokeWidth = 1.dp.toPx())
                drawLine(Color(0xFF333333), Offset(center.x - outerR, center.y), Offset(center.x + outerR, center.y), strokeWidth = 1.dp.toPx())

                drawCircle(
                    color = Color(0xFF00E5FF),
                    radius = innerR,
                    center = center + thumbOffset,
                    alpha = 0.85f
                )
            }
        }

        Text(
            text = "X: ${"%.2f".format(normalizedX)}  Y: ${"%.2f".format(normalizedY)}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

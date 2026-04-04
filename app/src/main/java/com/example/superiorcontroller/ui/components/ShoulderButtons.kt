package com.example.superiorcontroller.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.superiorcontroller.hid.GamepadButtons
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val BUMPER_COLOR = Color(0xFFFF9800)
private val TRIGGER_COLOR = Color(0xFFE65100)
private val TRIGGER_TRACK = Color(0xFF1A1A2E)
private val MENU_COLOR = Color(0xFF00897B)
private val HOME_COLOR = Color(0xFF607D8B)
private val STICK_CLICK_COLOR = Color(0xFF546E7A)

private val CapsuleShape = RoundedCornerShape(percent = 50)

// ── Analog triggers ──────────────────────────────────────────────────────

@Composable
fun TriggerRow(
    onLeftTrigger: (Float) -> Unit,
    onRightTrigger: (Float) -> Unit,
    modifier: Modifier = Modifier,
    buttonMode: Boolean = false,
    hwLeftTrigger: Float = 0f,
    hwRightTrigger: Float = 0f
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        GamepadTrigger("LT", onValueChanged = onLeftTrigger, buttonMode = buttonMode, hwValue = hwLeftTrigger)
        GamepadTrigger("RT", onValueChanged = onRightTrigger, buttonMode = buttonMode, hwValue = hwRightTrigger)
    }
}

@Composable
fun GamepadTrigger(
    label: String,
    onValueChanged: (Float) -> Unit,
    buttonMode: Boolean,
    modifier: Modifier = Modifier,
    hwValue: Float = 0f,
    width: Dp = 110.dp,
    height: Dp = 64.dp
) {
    if (buttonMode) {
        TriggerButton(label, onValueChanged, modifier, hwPressed = hwValue > 0.5f, width = width, height = height)
    } else {
        AnalogTrigger(label, onValueChanged, modifier, hwValue = hwValue, width = width, height = height)
    }
}

@Composable
fun AnalogTrigger(
    label: String,
    onValueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    hwValue: Float = 0f,
    accentColor: Color = TRIGGER_COLOR,
    width: Dp = 110.dp,
    height: Dp = 64.dp
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var fill by remember { mutableFloatStateOf(0f) }
    var animJob by remember { mutableStateOf<Job?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    val displayFill = if (isDragging) fill.coerceIn(0f, 1f) else hwValue.coerceIn(0f, 1f)
    val pct = (displayFill * 100).toInt()
    val borderAlpha = 0.2f + 0.8f * displayFill
    val gradientTop = remember(accentColor) { accentColor.copy(alpha = 0.4f) }

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(CapsuleShape)
            .background(TRIGGER_TRACK)
            .pointerInput(Unit) {
                awaitEachGesture {
                    isDragging = true
                    animJob?.cancel()

                    val down = awaitFirstDown(requireUnconsumed = false).also { it.consume() }
                    ButtonHaptics.performClick(context, label)
                    ButtonSoundPlayer.playClick(label)

                    val h = size.height.toFloat()
                    var value = (down.position.y / h).coerceIn(0f, 1f)
                    fill = value
                    onValueChanged(value)

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pointer = event.changes.firstOrNull() ?: break
                            pointer.consume()
                            if (!pointer.pressed) break
                            val next = (pointer.position.y / h).coerceIn(0f, 1f)
                            if (next != value) {
                                value = next
                                fill = value
                                onValueChanged(value)
                            }
                        }
                    } finally {
                        onValueChanged(0f)
                        val startFill = fill
                        animJob = scope.launch {
                            if (startFill > 0.005f) {
                                animate(
                                    initialValue = startFill,
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ) { v, _ ->
                                    fill = v.coerceIn(0f, 1f)
                                }
                            }
                            fill = 0f
                            isDragging = false
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (displayFill > 0f) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(displayFill)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(gradientTop, accentColor)))
            )
        }

        Box(
            Modifier
                .matchParentSize()
                .border(1.5.dp, accentColor.copy(alpha = borderAlpha), CapsuleShape)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            if (pct > 0) {
                Text(text = "$pct%", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun TriggerButton(
    label: String,
    onValueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    hwPressed: Boolean = false,
    width: Dp = 110.dp,
    height: Dp = 64.dp
) {
    val context = LocalContext.current
    var pressed by remember { mutableStateOf(false) }
    val isActive = pressed || hwPressed

    Surface(
        modifier = modifier
            .width(width)
            .height(height)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false).also { it.consume() }
                    pressed = true
                    ButtonHaptics.performClick(context, label)
                    ButtonSoundPlayer.playClick(label)
                    onValueChanged(1f)
                    waitForUpOrCancellation()?.consume()
                    pressed = false
                    onValueChanged(0f)
                }
            },
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) TRIGGER_COLOR.copy(alpha = 0.6f) else TRIGGER_COLOR,
        shadowElevation = if (isActive) 1.dp else 4.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = label, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color.White)
        }
    }
}

// ── Digital buttons ──────────────────────────────────────────────────────

@Composable
fun BumperRow(
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hwButtons: Int = 0
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ControlButton("LB", GamepadButtons.LB, BUMPER_COLOR, onPress, onRelease,
            hwPressed = (hwButtons and GamepadButtons.LB) != 0)
        ControlButton("RB", GamepadButtons.RB, BUMPER_COLOR, onPress, onRelease,
            hwPressed = (hwButtons and GamepadButtons.RB) != 0)
    }
}

@Composable
fun MenuButtons(
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hwButtons: Int = 0
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ControlButton("BACK", GamepadButtons.BACK, MENU_COLOR, onPress, onRelease, width = 72.dp,
            hwPressed = (hwButtons and GamepadButtons.BACK) != 0)
        HomeButton(onPress = onPress, onRelease = onRelease,
            hwPressed = (hwButtons and GamepadButtons.HOME) != 0)
        ControlButton("START", GamepadButtons.START, MENU_COLOR, onPress, onRelease, width = 72.dp,
            hwPressed = (hwButtons and GamepadButtons.START) != 0)
    }
}

@Composable
fun HomeButton(
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hwPressed: Boolean = false,
    size: Dp = 38.dp
) {
    val context = LocalContext.current
    var pressed by remember { mutableStateOf(false) }
    val isActive = pressed || hwPressed

    Surface(
        modifier = modifier
            .size(size)
            .pointerInput(GamepadButtons.HOME) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false).also { it.consume() }
                    pressed = true
                    ButtonHaptics.performClick(context, "HOME")
                    ButtonSoundPlayer.playClick("HOME")
                    onPress(GamepadButtons.HOME)
                    waitForUpOrCancellation()?.consume()
                    pressed = false
                    onRelease(GamepadButtons.HOME)
                }
            },
        shape = CircleShape,
        color = if (isActive) HOME_COLOR.copy(alpha = 0.6f) else HOME_COLOR,
        shadowElevation = if (isActive) 1.dp else 4.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                Icons.Default.Home,
                contentDescription = "Home",
                tint = Color.White,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}

@Composable
fun StickClickRow(
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hwButtons: Int = 0
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        ControlButton("L3", GamepadButtons.L3, STICK_CLICK_COLOR, onPress, onRelease, width = 56.dp, height = 32.dp, fontSize = 11,
            hwPressed = (hwButtons and GamepadButtons.L3) != 0)
        ControlButton("R3", GamepadButtons.R3, STICK_CLICK_COLOR, onPress, onRelease, width = 56.dp, height = 32.dp, fontSize = 11,
            hwPressed = (hwButtons and GamepadButtons.R3) != 0)
    }
}

@Composable
fun ControlButton(
    label: String,
    button: Int,
    color: Color,
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hwPressed: Boolean = false,
    width: Dp = 100.dp,
    height: Dp = 38.dp,
    fontSize: Int = 13
) {
    val context = LocalContext.current
    var pressed by remember { mutableStateOf(false) }
    val isActive = pressed || hwPressed

    Surface(
        modifier = modifier
            .width(width)
            .height(height)
            .pointerInput(button) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false).also { it.consume() }
                    pressed = true
                    ButtonHaptics.performClick(context, label)
                    ButtonSoundPlayer.playClick(label)
                    onPress(button)
                    waitForUpOrCancellation()?.consume()
                    pressed = false
                    onRelease(button)
                }
            },
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) color.copy(alpha = 0.6f) else color,
        shadowElevation = if (isActive) 1.dp else 4.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = fontSize.sp, color = Color.White)
        }
    }
}

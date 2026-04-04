package com.example.superiorcontroller.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.view.HapticFeedbackConstants
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.superiorcontroller.hid.GamepadButtons

private val BUMPER_COLOR = Color(0xFFFF9800)
private val TRIGGER_COLOR = Color(0xFFE65100)
private val MENU_COLOR = Color(0xFF00897B)
private val STICK_CLICK_COLOR = Color(0xFF546E7A)

@Composable
fun BumperRow(
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ControlButton("LB", GamepadButtons.LB, BUMPER_COLOR, onPress, onRelease)
        ControlButton("RB", GamepadButtons.RB, BUMPER_COLOR, onPress, onRelease)
    }
}

@Composable
fun TriggerRow(
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ControlButton("LT", GamepadButtons.LT, TRIGGER_COLOR, onPress, onRelease)
        ControlButton("RT", GamepadButtons.RT, TRIGGER_COLOR, onPress, onRelease)
    }
}

@Composable
fun MenuButtons(
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        ControlButton("BACK", GamepadButtons.BACK, MENU_COLOR, onPress, onRelease, width = 80.dp)
        ControlButton("START", GamepadButtons.START, MENU_COLOR, onPress, onRelease, width = 80.dp)
    }
}

@Composable
fun StickClickRow(
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        ControlButton("L3", GamepadButtons.L3, STICK_CLICK_COLOR, onPress, onRelease, width = 56.dp, height = 32.dp, fontSize = 11)
        ControlButton("R3", GamepadButtons.R3, STICK_CLICK_COLOR, onPress, onRelease, width = 56.dp, height = 32.dp, fontSize = 11)
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
    width: Dp = 100.dp,
    height: Dp = 38.dp,
    fontSize: Int = 13
) {
    val view = LocalView.current
    var pressed by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .width(width)
            .height(height)
            .pointerInput(button) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false).also { it.consume() }
                    pressed = true
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onPress(button)
                    waitForUpOrCancellation()?.consume()
                    pressed = false
                    onRelease(button)
                }
            },
        shape = RoundedCornerShape(8.dp),
        color = if (pressed) color.copy(alpha = 0.6f) else color,
        shadowElevation = if (pressed) 1.dp else 4.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = fontSize.sp, color = Color.White)
        }
    }
}

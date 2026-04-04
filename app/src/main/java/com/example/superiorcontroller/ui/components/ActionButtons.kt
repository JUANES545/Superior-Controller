package com.example.superiorcontroller.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.superiorcontroller.hid.GamepadButtons

@Composable
fun ActionButtons(
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 52.dp,
    spacing: Dp = 36.dp
) {
    Box(
        modifier = modifier.size(buttonSize * 2 + spacing),
        contentAlignment = Alignment.Center
    ) {
        GamepadFaceButton("Y", Color(0xFFFFEB3B), Color.Black, buttonSize,
            { onPress(GamepadButtons.Y) }, { onRelease(GamepadButtons.Y) },
            Modifier.offset(y = -spacing))
        GamepadFaceButton("A", Color(0xFF4CAF50), Color.White, buttonSize,
            { onPress(GamepadButtons.A) }, { onRelease(GamepadButtons.A) },
            Modifier.offset(y = spacing))
        GamepadFaceButton("X", Color(0xFF2196F3), Color.White, buttonSize,
            { onPress(GamepadButtons.X) }, { onRelease(GamepadButtons.X) },
            Modifier.offset(x = -spacing))
        GamepadFaceButton("B", Color(0xFFF44336), Color.White, buttonSize,
            { onPress(GamepadButtons.B) }, { onRelease(GamepadButtons.B) },
            Modifier.offset(x = spacing))
    }
}

@Composable
fun GamepadFaceButton(
    label: String,
    color: Color,
    textColor: Color,
    size: Dp = 52.dp,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPress()
                        tryAwaitRelease()
                        pressed = false
                        onRelease()
                    }
                )
            },
        shape = CircleShape,
        color = if (pressed) color.copy(alpha = 0.6f) else color,
        shadowElevation = if (pressed) 1.dp else 6.dp,
        contentColor = textColor
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

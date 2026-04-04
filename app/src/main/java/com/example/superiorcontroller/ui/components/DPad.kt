package com.example.superiorcontroller.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.superiorcontroller.hid.GamepadButtons

private val DPAD_COLOR = Color(0xFF37474F)

@Composable
fun DPad(
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    modifier: Modifier = Modifier,
    btnWidth: Dp = 46.dp,
    btnHeight: Dp = 40.dp,
    offset: Dp = 42.dp
) {
    Box(
        modifier = modifier.size(btnWidth + offset * 2),
        contentAlignment = Alignment.Center
    ) {
        DPadButton("▲", btnWidth, btnHeight, { onPress(GamepadButtons.DPAD_UP) },
            { onRelease(GamepadButtons.DPAD_UP) }, Modifier.offset(y = -offset))
        DPadButton("▼", btnWidth, btnHeight, { onPress(GamepadButtons.DPAD_DOWN) },
            { onRelease(GamepadButtons.DPAD_DOWN) }, Modifier.offset(y = offset))
        DPadButton("◄", btnWidth, btnHeight, { onPress(GamepadButtons.DPAD_LEFT) },
            { onRelease(GamepadButtons.DPAD_LEFT) }, Modifier.offset(x = -offset))
        DPadButton("►", btnWidth, btnHeight, { onPress(GamepadButtons.DPAD_RIGHT) },
            { onRelease(GamepadButtons.DPAD_RIGHT) }, Modifier.offset(x = offset))
    }
}

@Composable
private fun DPadButton(
    label: String,
    width: Dp,
    height: Dp,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .size(width = width, height = height)
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
        shape = RoundedCornerShape(8.dp),
        color = if (pressed) DPAD_COLOR.copy(alpha = 0.6f) else DPAD_COLOR,
        shadowElevation = if (pressed) 1.dp else 4.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

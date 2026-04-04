package com.example.superiorcontroller.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.superiorcontroller.hid.GamepadButtons

private val DPAD_BTN_WIDTH = 50.dp
private val DPAD_BTN_HEIGHT = 44.dp
private val DPAD_OFFSET = 46.dp
private val DPAD_COLOR = Color(0xFF616161)

@Composable
fun DPad(
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(DPAD_BTN_WIDTH + DPAD_OFFSET * 2),
        contentAlignment = Alignment.Center
    ) {
        // Up
        DPadButton(
            label = "▲",
            onPress = { onPress(GamepadButtons.DPAD_UP) },
            onRelease = { onRelease(GamepadButtons.DPAD_UP) },
            modifier = Modifier.offset(y = -DPAD_OFFSET)
        )
        // Down
        DPadButton(
            label = "▼",
            onPress = { onPress(GamepadButtons.DPAD_DOWN) },
            onRelease = { onRelease(GamepadButtons.DPAD_DOWN) },
            modifier = Modifier.offset(y = DPAD_OFFSET)
        )
        // Left
        DPadButton(
            label = "◄",
            onPress = { onPress(GamepadButtons.DPAD_LEFT) },
            onRelease = { onRelease(GamepadButtons.DPAD_LEFT) },
            modifier = Modifier.offset(x = -DPAD_OFFSET)
        )
        // Right
        DPadButton(
            label = "►",
            onPress = { onPress(GamepadButtons.DPAD_RIGHT) },
            onRelease = { onRelease(GamepadButtons.DPAD_RIGHT) },
            modifier = Modifier.offset(x = DPAD_OFFSET)
        )
    }
}

@Composable
private fun DPadButton(
    label: String,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = {},
        modifier = modifier
            .size(width = DPAD_BTN_WIDTH, height = DPAD_BTN_HEIGHT)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPress()
                        tryAwaitRelease()
                        onRelease()
                    }
                )
            },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DPAD_COLOR),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

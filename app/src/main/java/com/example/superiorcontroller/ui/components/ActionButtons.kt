package com.example.superiorcontroller.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.superiorcontroller.hid.GamepadButtons

private val BUTTON_SIZE = 56.dp
private val DIAMOND_OFFSET = 38.dp

@Composable
fun ActionButtons(
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(BUTTON_SIZE * 2 + DIAMOND_OFFSET),
        contentAlignment = Alignment.Center
    ) {
        // Y - top
        GamepadButton(
            label = "Y",
            color = Color(0xFFFFEB3B),
            textColor = Color.Black,
            onPress = { onPress(GamepadButtons.Y) },
            onRelease = { onRelease(GamepadButtons.Y) },
            modifier = Modifier.offset(y = -DIAMOND_OFFSET)
        )
        // A - bottom
        GamepadButton(
            label = "A",
            color = Color(0xFF4CAF50),
            textColor = Color.White,
            onPress = { onPress(GamepadButtons.A) },
            onRelease = { onRelease(GamepadButtons.A) },
            modifier = Modifier.offset(y = DIAMOND_OFFSET)
        )
        // X - left
        GamepadButton(
            label = "X",
            color = Color(0xFF2196F3),
            textColor = Color.White,
            onPress = { onPress(GamepadButtons.X) },
            onRelease = { onRelease(GamepadButtons.X) },
            modifier = Modifier.offset(x = -DIAMOND_OFFSET)
        )
        // B - right
        GamepadButton(
            label = "B",
            color = Color(0xFFF44336),
            textColor = Color.White,
            onPress = { onPress(GamepadButtons.B) },
            onRelease = { onRelease(GamepadButtons.B) },
            modifier = Modifier.offset(x = DIAMOND_OFFSET)
        )
    }
}

@Composable
fun GamepadButton(
    label: String,
    color: Color,
    textColor: Color,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { /* handled by pointer input */ },
        modifier = modifier
            .size(BUTTON_SIZE)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPress()
                        tryAwaitRelease()
                        onRelease()
                    }
                )
            },
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

package com.example.superiorcontroller.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.superiorcontroller.hid.GamepadButtons

private val SHOULDER_COLOR = Color(0xFFFF9800)
private val MENU_COLOR = Color(0xFF009688)

@Composable
fun ShoulderButtons(
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TriggerButton("L1", GamepadButtons.L1, SHOULDER_COLOR, onPress, onRelease)
        TriggerButton("R1", GamepadButtons.R1, SHOULDER_COLOR, onPress, onRelease)
    }
}

@Composable
fun MenuButtons(
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        TriggerButton(
            "SELECT", GamepadButtons.SELECT, MENU_COLOR, onPress, onRelease,
            widthDp = 90
        )
        TriggerButton(
            "START", GamepadButtons.START, MENU_COLOR, onPress, onRelease,
            modifier = Modifier.padding(start = 24.dp),
            widthDp = 90
        )
    }
}

@Composable
private fun TriggerButton(
    label: String,
    button: Int,
    color: Color,
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    modifier: Modifier = Modifier,
    widthDp: Int = 100
) {
    Button(
        onClick = {},
        modifier = modifier
            .width(widthDp.dp)
            .height(40.dp)
            .pointerInput(button) {
                detectTapGestures(
                    onPress = {
                        onPress(button)
                        tryAwaitRelease()
                        onRelease(button)
                    }
                )
            },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
    }
}

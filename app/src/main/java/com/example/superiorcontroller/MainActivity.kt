package com.example.superiorcontroller

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.superiorcontroller.ui.GamepadScreen
import com.example.superiorcontroller.ui.theme.SuperiorControllerTheme
import com.example.superiorcontroller.viewmodel.GamepadViewModel

class MainActivity : ComponentActivity() {

    private val gamepadViewModel: GamepadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SuperiorControllerTheme {
                var permissionsGranted by remember { mutableStateOf(hasBluetoothPermissions()) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    permissionsGranted = results.values.all { it }
                    if (permissionsGranted) gamepadViewModel.initializeBluetooth()
                }

                LaunchedEffect(permissionsGranted) {
                    if (permissionsGranted) gamepadViewModel.initializeBluetooth()
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GamepadScreen(
                        viewModel = gamepadViewModel,
                        permissionsGranted = permissionsGranted,
                        onRequestPermissions = { permissionLauncher.launch(bluetoothPermissions()) },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    // ── Hardware gamepad event capture ────────────────────────────────

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && gamepadViewModel.processHardwareKeyEvent(event)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && gamepadViewModel.processHardwareKeyEvent(event)) return true
        return super.onKeyUp(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event != null && gamepadViewModel.processHardwareMotionEvent(event)) return true
        return super.onGenericMotionEvent(event)
    }

    // ── Permissions ──────────────────────────────────────────────────

    private fun hasBluetoothPermissions(): Boolean =
        bluetoothPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    companion object {
        fun bluetoothPermissions(): Array<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            }
    }
}

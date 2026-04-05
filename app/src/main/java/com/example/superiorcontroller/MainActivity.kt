package com.example.superiorcontroller

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
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
    private var hasResumedOnce = false

    private fun logLifecycle(event: String) {
        val state = gamepadViewModel.diagnosticState()
        Log.d("GamepadDebug", "LIFECYCLE: $event | $state")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gamepadViewModel.currentLifecycleState = "CREATED"
        logLifecycle("onCreate (savedState=${savedInstanceState != null})")
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

    override fun onStart() {
        super.onStart()
        gamepadViewModel.currentLifecycleState = "STARTED"
        logLifecycle("onStart")
    }

    override fun onResume() {
        super.onResume()
        gamepadViewModel.currentLifecycleState = "RESUMED"
        logLifecycle("onResume (hasResumedOnce=$hasResumedOnce)")
        if (hasResumedOnce && hasBluetoothPermissions()) {
            gamepadViewModel.recoverConnection()
        }
        hasResumedOnce = true
    }

    override fun onPause() {
        gamepadViewModel.currentLifecycleState = "PAUSED"
        logLifecycle("onPause")
        super.onPause()
    }

    override fun onStop() {
        gamepadViewModel.currentLifecycleState = "STOPPED"
        logLifecycle("onStop")
        super.onStop()
    }

    override fun onDestroy() {
        gamepadViewModel.currentLifecycleState = "DESTROYED"
        logLifecycle("onDestroy (isFinishing=$isFinishing)")
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d("GamepadDebug",
            "CONFIG_CHANGED: keyboard=${newConfig.keyboard} " +
            "keyboardHidden=${newConfig.keyboardHidden} " +
            "navigation=${newConfig.navigation} " +
            "orientation=${newConfig.orientation} " +
            "uiMode=${newConfig.uiMode} | ${gamepadViewModel.diagnosticState()}"
        )
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

package com.tiaosheng.counter.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tiaosheng.counter.camera.CameraManager
import com.tiaosheng.counter.counter.DetectionState
import com.tiaosheng.counter.counter.JumpDetector
import com.tiaosheng.counter.ui.theme.ErrorRed
import com.tiaosheng.counter.ui.theme.PauseYellow
import com.tiaosheng.counter.ui.theme.SportGreen
import com.tiaosheng.counter.ui.theme.SportOrange
import com.tiaosheng.counter.ui.theme.TextWhite

@Composable
fun MainScreen(
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    val hasCamPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "需要相机权限才能使用跳绳计数", Toast.LENGTH_LONG).show()
        }
    }

    val previewView = remember { PreviewView(context) }
    val cameraManager = remember { CameraManager(
        lifecycleOwner = lifecycleOwner,
        previewView = previewView,
        onPoseResult = { result -> viewModel.onPoseResult(result) }
    ) }

    LaunchedEffect(hasCamPermission) {
        if (hasCamPermission) {
            cameraManager.start()
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            cameraManager.stop()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview (full screen)
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // HUD overlay
        HudOverlay(
            state = uiState.detectionState,
            count = uiState.count,
            bpm = uiState.bpm,
            calories = uiState.calories,
            elapsedSeconds = uiState.elapsedSeconds,
            isPaused = uiState.isPaused
        )

        // Bottom controls
        if (uiState.detectionState != DetectionState.IDLE) {
            BottomControls(
                isPaused = uiState.isPaused,
                onPauseResume = {
                    if (uiState.isPaused) viewModel.resume() else viewModel.pause()
                },
                onStop = { viewModel.stop() },
                onSwitchMode = {
                    val newMode = if (uiState.jumpMode == JumpDetector.JumpMode.BOTH_FEET)
                        JumpDetector.JumpMode.ALTERNATE
                    else
                        JumpDetector.JumpMode.BOTH_FEET
                    viewModel.setMode(newMode)
                },
                onSwitchCamera = { cameraManager.switchCamera() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
            )
        }

        // Camera permission request
        if (!hasCamPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "需要相机权限",
                    color = TextWhite,
                    fontSize = 20.sp
                )
                Text(
                    text = "跳绳计数需要使用摄像头识别您的动作\n所有数据仅在本机处理",
                    color = TextWhite.copy(alpha = 0.65f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = SportOrange)
                ) {
                    Text("授予权限")
                }
            }
        }
    }
}

@Composable
private fun BottomControls(
    isPaused: Boolean,
    onPauseResume: () -> Unit,
    onStop: () -> Unit,
    onSwitchMode: () -> Unit,
    onSwitchCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ControlButton(
            icon = Icons.Default.Cameraswitch,
            label = "翻转",
            onClick = onSwitchCamera
        )
        ControlButton(
            icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
            label = if (isPaused) "继续" else "暂停",
            onClick = onPauseResume,
            tint = if (isPaused) SportGreen else PauseYellow
        )
        ControlButton(
            icon = Icons.Default.Stop,
            label = "结束",
            onClick = onStop,
            tint = ErrorRed
        )
        ControlButton(
            icon = null,
            label = "模式",
            onClick = onSwitchMode
        )
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    label: String,
    onClick: () -> Unit,
    tint: Color = TextWhite,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Text(
                    text = label,
                    color = tint,
                    fontSize = 13.sp
                )
            }
        }
        Text(
            text = label,
            color = TextWhite.copy(alpha = 0.65f),
            fontSize = 11.sp
        )
    }
}

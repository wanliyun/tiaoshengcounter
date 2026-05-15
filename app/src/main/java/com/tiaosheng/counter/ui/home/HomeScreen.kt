package com.tiaosheng.counter.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tiaosheng.counter.ui.theme.SportGreen
import com.tiaosheng.counter.ui.theme.SportOrange
import com.tiaosheng.counter.ui.theme.SurfaceDark
import com.tiaosheng.counter.ui.theme.TextDim
import com.tiaosheng.counter.ui.theme.TextWhite

@Composable
fun HomeScreen(
    onStartTimed: (durationSeconds: Int) -> Unit,
    onStartCount: (targetCount: Int) -> Unit,
    onVideoCount: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit
) {
    var showTimeSheet by remember { mutableStateOf(false) }
    var showCountSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "跳绳计数",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 定时模式
        ModeButton(
            icon = { Icon(Icons.Filled.Timer, null, Modifier.size(32.dp), tint = TextWhite) },
            title = "定时模式",
            subtitle = "设置时间，倒计时跳",
            containerColor = SportOrange,
            onClick = { showTimeSheet = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 定数模式
        ModeButton(
            icon = { Icon(Icons.Filled.FilterCenterFocus, null, Modifier.size(32.dp), tint = TextWhite) },
            title = "定数模式",
            subtitle = "设定目标，跳完即停",
            containerColor = SportGreen,
            onClick = { showCountSheet = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 视频计数
        OutlinedModeButton(
            icon = { Icon(Icons.Filled.Videocam, null, Modifier.size(32.dp), tint = TextWhite) },
            title = "视频计数",
            subtitle = "从视频文件识别次数",
            onClick = onVideoCount
        )

        Spacer(modifier = Modifier.weight(1f))

        // 底部快捷入口
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(onClick = onHistory) {
                Icon(Icons.AutoMirrored.Filled.List, null, Modifier.size(20.dp), tint = TextDim)
                Spacer(modifier = Modifier.width(4.dp))
                Text("历史记录", color = TextDim, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(32.dp))
            TextButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, null, Modifier.size(20.dp), tint = TextDim)
                Spacer(modifier = Modifier.width(4.dp))
                Text("设置", color = TextDim, fontSize = 14.sp)
            }
        }
    }

    if (showTimeSheet) {
        ModeConfigSheet(
            configType = ConfigType.TIME,
            onDismiss = { showTimeSheet = false },
            onStart = { seconds ->
                showTimeSheet = false
                onStartTimed(seconds)
            }
        )
    }

    if (showCountSheet) {
        ModeConfigSheet(
            configType = ConfigType.COUNT,
            onDismiss = { showCountSheet = false },
            onStart = { value ->
                showCountSheet = false
                onStartCount(value)
            }
        )
    }
}

@Composable
private fun ModeButton(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    containerColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor)
    ) {
        icon()
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextWhite.copy(alpha = 0.75f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun OutlinedModeButton(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(TextDim)
        )
    ) {
        icon()
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextDim, fontSize = 13.sp)
        }
    }
}

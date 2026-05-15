package com.tiaosheng.counter.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tiaosheng.counter.ui.theme.BodyLabel
import com.tiaosheng.counter.ui.theme.CaptionHint
import com.tiaosheng.counter.ui.theme.HeadlineStat
import com.tiaosheng.counter.ui.theme.SportOrange
import com.tiaosheng.counter.ui.theme.SurfaceDark
import com.tiaosheng.counter.ui.theme.TextDim
import com.tiaosheng.counter.ui.theme.TextWhite

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .padding(top = 48.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "设置",
            style = HeadlineStat,
            color = TextWhite,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        // Weight
        SettingsGroup(title = "体重") {
            Text(
                text = "${uiState.weightKg.toInt()} kg",
                style = BodyLabel,
                color = SportOrange
            )
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = uiState.weightKg,
                onValueChange = { viewModel.setWeight(it) },
                valueRange = 30f..150f,
                steps = 0,
                colors = SliderDefaults.colors(
                    thumbColor = SportOrange,
                    activeTrackColor = SportOrange
                )
            )
        }

        // Sensitivity
        SettingsGroup(title = "计数灵敏度") {
            SensitivitySelector(
                current = uiState.sensitivity,
                onSelect = { viewModel.setSensitivity(it) }
            )
        }

        // Voice settings
        SettingsGroup(title = "语音播报") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "启用语音播报", style = BodyLabel, color = TextWhite)
                }
                Switch(
                    checked = uiState.voiceEnabled,
                    onCheckedChange = { viewModel.setVoiceEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SportOrange,
                        checkedTrackColor = SportOrange.copy(alpha = 0.5f)
                    )
                )
            }

            if (uiState.voiceEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "播报间隔", style = CaptionHint, color = TextDim)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
                ) {
                    listOf(0 to "关闭", 10 to "10次", 50 to "50次", 100 to "100次").forEach { (interval, label) ->
                        val selected = uiState.voiceInterval == interval
                        Text(
                            text = label,
                            style = BodyLabel,
                            color = if (selected) SportOrange else TextDim,
                            modifier = Modifier
                                .clickable { viewModel.setVoiceInterval(interval) }
                                .background(
                                    if (selected) SportOrange.copy(alpha = 0.15f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Camera facing
        SettingsGroup(title = "默认摄像头") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
            ) {
                listOf("back" to "后置", "front" to "前置").forEach { (value, label) ->
                    val selected = uiState.cameraFacing == value
                    Text(
                        text = label,
                        style = BodyLabel,
                        color = if (selected) SportOrange else TextDim,
                        modifier = Modifier
                            .clickable { viewModel.setCameraFacing(value) }
                            .background(
                                if (selected) SportOrange.copy(alpha = 0.15f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // About
        SettingsGroup(title = "关于") {
            Text(text = "跳绳计数 v1.0.0", style = BodyLabel, color = TextWhite)
            Text(
                text = "完全离线运行，所有数据仅在设备本地处理",
                style = CaptionHint,
                color = TextDim,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = CaptionHint,
            color = TextDim,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
private fun SensitivitySelector(
    current: String,
    onSelect: (String) -> Unit
) {
    val options = listOf(
        "low" to "低（大幅度跳跃）",
        "medium" to "中（标准）",
        "high" to "高（小幅快速）"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
    ) {
        options.forEach { (value, label) ->
            val selected = current == value
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onSelect(value) }
                    .background(
                        if (selected) SportOrange.copy(alpha = 0.15f) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = label.split("（").first(),
                    style = BodyLabel,
                    color = if (selected) SportOrange else TextDim
                )
                Text(
                    text = label.split("（").last().dropLast(1),
                    style = CaptionHint,
                    color = if (selected) SportOrange.copy(alpha = 0.7f) else TextDim
                )
            }
        }
    }
}

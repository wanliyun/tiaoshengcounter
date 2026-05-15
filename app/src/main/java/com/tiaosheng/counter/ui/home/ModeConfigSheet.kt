package com.tiaosheng.counter.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tiaosheng.counter.ui.theme.SportGreen
import com.tiaosheng.counter.ui.theme.SportOrange
import com.tiaosheng.counter.ui.theme.SurfaceDark
import com.tiaosheng.counter.ui.theme.TextDim
import com.tiaosheng.counter.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeConfigSheet(
    configType: ConfigType,
    onDismiss: () -> Unit,
    onStart: (value: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val (title, _, defaultValue, step, presets, min, max) = when (configType) {
        ConfigType.TIME -> SheetConfig(
            title = "定时设置",
            unit = "秒",
            defaultValue = 60,
            step = 30,
            presets = listOf(60, 120, 180, 300),
            min = 30,
            max = 600
        )
        ConfigType.COUNT -> SheetConfig(
            title = "目标设置",
            unit = "个",
            defaultValue = 200,
            step = 50,
            presets = listOf(100, 200, 500, 1000),
            min = 50,
            max = 9999
        )
    }

    var value by remember { mutableIntStateOf(defaultValue) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = TextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 数值调整区
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = { value = (value - step).coerceAtLeast(min) },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = SportOrange.copy(alpha = 0.2f),
                        contentColor = SportOrange
                    )
                ) {
                    Icon(Icons.Filled.Remove, "减少", Modifier.size(28.dp))
                }

                Spacer(modifier = Modifier.width(20.dp))

                val displayText = if (configType == ConfigType.TIME) {
                    formatSeconds(value)
                } else {
                    value.toString()
                }

                OutlinedTextField(
                    value = displayText,
                    onValueChange = { input ->
                        val parsed = if (configType == ConfigType.TIME) {
                            parseTimeInput(input)
                        } else {
                            input.filter { it.isDigit() }.toIntOrNull()
                        }
                        if (parsed != null) {
                            value = parsed.coerceIn(min, max)
                        }
                    },
                    modifier = Modifier.width(if (configType == ConfigType.TIME) 140.dp else 120.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = TextWhite,
                        fontSize = if (configType == ConfigType.TIME) 36.sp else 40.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SportOrange,
                        unfocusedBorderColor = TextDim,
                        cursorColor = SportOrange
                    )
                )

                Spacer(modifier = Modifier.width(20.dp))

                IconButton(
                    onClick = { value = (value + step).coerceAtMost(max) },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = SportOrange.copy(alpha = 0.2f),
                        contentColor = SportOrange
                    )
                ) {
                    Icon(Icons.Filled.Add, "增加", Modifier.size(28.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 快捷预设
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                presets.forEach { preset ->
                    val isSelected = value == preset
                    val presetLabel = if (configType == ConfigType.TIME) {
                        formatMinutes(preset)
                    } else {
                        preset.toString()
                    }
                    TextButton(
                        onClick = { value = preset },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isSelected) SportOrange else TextDim
                        )
                    ) {
                        Text(
                            text = presetLabel,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 开始按钮
            Button(
                onClick = { onStart(value) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (configType == ConfigType.TIME) SportOrange else SportGreen
                )
            ) {
                Text(
                    text = "开始跳绳",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

enum class ConfigType { TIME, COUNT }

private data class SheetConfig(
    val title: String,
    val unit: String,
    val defaultValue: Int,
    val step: Int,
    val presets: List<Int>,
    val min: Int,
    val max: Int
)

private fun formatSeconds(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}

private fun formatMinutes(totalSeconds: Int): String {
    val m = totalSeconds / 60
    return if (m >= 1) "${m}分" else "${totalSeconds}秒"
}

private fun parseTimeInput(input: String): Int? {
    val trimmed = input.trim()
    return when {
        // mm:ss format
        trimmed.contains(":") -> {
            val parts = trimmed.split(":")
            if (parts.size == 2) {
                val m = parts[0].toIntOrNull() ?: return null
                val s = parts[1].toIntOrNull() ?: return null
                m * 60 + s
            } else null
        }
        // plain number (seconds)
        trimmed.toIntOrNull() != null -> trimmed.toInt()
        else -> null
    }
}

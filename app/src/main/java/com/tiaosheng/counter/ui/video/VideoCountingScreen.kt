package com.tiaosheng.counter.ui.video

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tiaosheng.counter.ui.theme.ErrorRed
import com.tiaosheng.counter.ui.theme.SportGreen
import com.tiaosheng.counter.ui.theme.SportOrange
import com.tiaosheng.counter.ui.theme.TextDim
import com.tiaosheng.counter.ui.theme.TextWhite

@Composable
fun VideoCountingScreen(
    onBack: () -> Unit,
    viewModel: VideoCountingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            // 保留持久化读取权限
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.startProcessing(uri)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顶部栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                viewModel.cancel()
                onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = TextWhite)
            }
            Text(
                text = "视频计数",
                color = TextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        when (uiState.phase) {
            VideoCountingViewModel.Phase.IDLE -> {
                IdleContent(onSelectVideo = { filePicker.launch(arrayOf("video/*")) })
            }
            VideoCountingViewModel.Phase.PROCESSING -> {
                ProcessingContent(progress = uiState.progress, count = uiState.count)
            }
            VideoCountingViewModel.Phase.COMPLETED -> {
                ResultContent(
                    count = uiState.count,
                    avgBpm = uiState.avgBpm,
                    durationSeconds = uiState.durationSeconds,
                    onRetry = { viewModel.reset() },
                    onBack = onBack
                )
            }
            VideoCountingViewModel.Phase.ERROR -> {
                ErrorContent(
                    error = uiState.error ?: "未知错误",
                    onRetry = { viewModel.reset() },
                    onBack = onBack
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun IdleContent(onSelectVideo: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Filled.Videocam,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = TextDim
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "选择视频文件",
            color = TextWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "应用将逐帧分析视频中的跳绳动作并计数",
            color = TextDim,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onSelectVideo,
            modifier = Modifier.fillMaxWidth(0.7f).height(56.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SportOrange)
        ) {
            Text("选择视频", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ProcessingContent(progress: Float, count: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "分析中...",
            color = TextWhite.copy(alpha = pulseAlpha),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(24.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth(0.8f).height(8.dp),
            color = SportGreen,
            trackColor = SportGreen.copy(alpha = 0.2f),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "${(progress * 100).toInt()}%",
            color = TextDim,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "已识别: $count 次",
            color = TextWhite,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ResultContent(
    count: Int,
    avgBpm: Float,
    durationSeconds: Int,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "分析完成",
            color = SportGreen,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(32.dp))

        // 结果卡片
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .background(SportGreen.copy(alpha = 0.1f))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "$count", color = SportGreen, fontSize = 64.sp, fontWeight = FontWeight.Bold)
            Text(text = "次", color = SportGreen.copy(alpha = 0.7f), fontSize = 16.sp)
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = formatSeconds(durationSeconds), color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "视频时长", color = TextDim, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${avgBpm.toInt()}", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "平均 BPM", color = TextDim, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Row {
            OutlinedButton(
                onClick = onRetry,
                shape = RoundedCornerShape(999.dp)
            ) {
                Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("重新选择")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onBack,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SportOrange)
            ) {
                Text("返回首页")
            }
        }
    }
}

@Composable
private fun ErrorContent(error: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "处理失败",
            color = ErrorRed,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = error, color = TextDim, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Row {
            OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(999.dp)) {
                Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("重试")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onBack,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SportOrange)
            ) {
                Text("返回首页")
            }
        }
    }
}

private fun formatSeconds(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}

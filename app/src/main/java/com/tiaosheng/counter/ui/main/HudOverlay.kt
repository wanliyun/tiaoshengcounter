package com.tiaosheng.counter.ui.main

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tiaosheng.counter.counter.DetectionState
import com.tiaosheng.counter.ui.theme.CaptionHint
import com.tiaosheng.counter.ui.theme.CounterDisplay
import com.tiaosheng.counter.ui.theme.HeadlineStat
import com.tiaosheng.counter.ui.theme.SportGreen
import com.tiaosheng.counter.ui.theme.SportOrange
import com.tiaosheng.counter.ui.theme.TextDim
import com.tiaosheng.counter.ui.theme.TextWhite

@Composable
fun HudOverlay(
    state: DetectionState,
    count: Int,
    bpm: Float,
    calories: Float,
    elapsedSeconds: Int,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 暂停半透明遮罩
        if (isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部状态栏
            TopStatusBar(
                elapsedSeconds = elapsedSeconds,
                mode = "双脚跳",
                bpm = bpm
            )

            Spacer(modifier = Modifier.weight(1f))

            // 中央计数区
            CenterCounter(
                count = count,
                bpm = bpm,
                isCounting = state == DetectionState.COUNTING
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 卡路里
            Text(
                text = "卡路里: ${"%.1f".format(calories)} kcal",
                style = CaptionHint,
                color = TextDim
            )

            Spacer(modifier = Modifier.weight(1f))

            // 状态提示
            StatusHint(state = state, isPaused = isPaused)
        }
    }
}

@Composable
private fun TopStatusBar(
    elapsedSeconds: Int,
    mode: String,
    bpm: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatTime(elapsedSeconds),
            style = HeadlineStat,
            color = TextWhite
        )

        Text(
            text = mode,
            style = CaptionHint,
            color = TextDim
        )

        Text(
            text = "${bpm.toInt()} BPM",
            style = HeadlineStat,
            color = SportOrange
        )
    }
}

@Composable
private fun CenterCounter(
    count: Int,
    bpm: Float,
    isCounting: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (isCounting) 1.0f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "count_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(200.dp)
    ) {
        // BPM 环
        Canvas(modifier = Modifier.size(200.dp)) {
            val sweepAngle = (bpm / 200f).coerceIn(0f, 1f) * 360f
            drawArc(
                color = SportGreen,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$count",
                style = CounterDisplay.copy(
                    fontSize = (CounterDisplay.fontSize.value * scale).sp
                ),
                color = TextWhite,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatusHint(state: DetectionState, isPaused: Boolean) {
    val text = when {
        isPaused -> "已暂停"
        state == DetectionState.IDLE -> "将手机放置在身前，确保全身入镜"
        state == DetectionState.READY -> "准备开始"
        state == DetectionState.COUNTING -> ""
        state == DetectionState.PAUSED -> "已暂停"
        state == DetectionState.LOW_LIGHT -> "光线较暗，可能影响识别"
        else -> ""
    }
    if (text.isNotEmpty()) {
        Text(
            text = text,
            style = CaptionHint,
            color = if (state == DetectionState.LOW_LIGHT) SportOrange else TextDim,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

private fun formatTime(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}

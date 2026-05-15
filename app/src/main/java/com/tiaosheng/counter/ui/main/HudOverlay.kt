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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tiaosheng.counter.ExerciseMode
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
    exerciseMode: ExerciseMode = ExerciseMode.Free,
    remainingSeconds: Int = 0,
    targetCount: Int = 0,
    jumpModeLabel: String = "双脚跳",
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
                remainingSeconds = remainingSeconds,
                exerciseMode = exerciseMode,
                mode = when (exerciseMode) {
                    is ExerciseMode.Timed -> "定时"
                    is ExerciseMode.Count -> "定数"
                    else -> jumpModeLabel
                },
                bpm = bpm
            )

            Spacer(modifier = Modifier.weight(1f))

            // 中央计数区
            CenterCounter(
                count = count,
                bpm = bpm,
                isCounting = state == DetectionState.COUNTING,
                exerciseMode = exerciseMode,
                remainingSeconds = remainingSeconds,
                targetCount = targetCount
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 卡路里
            Text(
                text = "卡路里: ${"%.1f".format(calories)} kcal",
                style = CaptionHint,
                color = TextDim
            )

            // 定数模式进度条
            if (exerciseMode is ExerciseMode.Count && targetCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = (count.toFloat() / targetCount).coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = SportGreen,
                    trackColor = SportGreen.copy(alpha = 0.2f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$count / $targetCount",
                    style = CaptionHint,
                    color = TextDim
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 状态提示
            StatusHint(state = state, isPaused = isPaused)
        }
    }
}

@Composable
private fun TopStatusBar(
    elapsedSeconds: Int,
    remainingSeconds: Int,
    exerciseMode: ExerciseMode,
    mode: String,
    bpm: Float
) {
    val timeDisplay = when (exerciseMode) {
        is ExerciseMode.Timed -> formatTime(remainingSeconds)
        else -> formatTime(elapsedSeconds)
    }

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
            text = timeDisplay,
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
    isCounting: Boolean,
    exerciseMode: ExerciseMode,
    remainingSeconds: Int,
    targetCount: Int
) {
    val scale by animateFloatAsState(
        targetValue = if (isCounting) 1.0f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "count_scale"
    )

    // 计算进度环的 sweepAngle
    val arcSweep: Float = when {
        exerciseMode is ExerciseMode.Timed -> {
            val duration = exerciseMode.durationSeconds
            if (duration > 0) (remainingSeconds.toFloat() / duration) * 360f else 0f
        }
        exerciseMode is ExerciseMode.Count && targetCount > 0 -> {
            (count.toFloat() / targetCount).coerceIn(0f, 1f) * 360f
        }
        else -> (bpm / 200f).coerceIn(0f, 1f) * 360f
    }

    val arcColor = when (exerciseMode) {
        is ExerciseMode.Timed -> SportOrange
        is ExerciseMode.Count -> SportGreen
        else -> SportGreen
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(200.dp)
    ) {
        // 进度环
        Canvas(modifier = Modifier.size(200.dp)) {
            drawArc(
                color = arcColor,
                startAngle = -90f,
                sweepAngle = arcSweep,
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
            // 模式标签
            when (exerciseMode) {
                is ExerciseMode.Timed -> Text(
                    text = "剩余 ${formatTime(remainingSeconds)}",
                    style = CaptionHint,
                    color = SportOrange
                )
                is ExerciseMode.Count -> Text(
                    text = "目标 $targetCount",
                    style = CaptionHint,
                    color = SportGreen
                )
                else -> {}
            }
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

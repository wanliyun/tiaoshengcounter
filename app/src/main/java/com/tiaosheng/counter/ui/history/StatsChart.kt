package com.tiaosheng.counter.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tiaosheng.counter.ui.theme.SportOrange
import com.tiaosheng.counter.ui.theme.TextDim

@Composable
fun StatsChart(
    data: List<Int>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val maxVal = data.max().coerceAtLeast(1)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 24.dp)
    ) {
        val barWidth = size.width / (data.size * 1.5f)
        val gap = barWidth * 0.5f

        data.forEachIndexed { index, value ->
            val barHeight = (value.toFloat() / maxVal) * (size.height - 4.dp.toPx())
            val x = index * (barWidth + gap)
            val y = size.height - barHeight

            drawRect(
                color = SportOrange.copy(alpha = 0.8f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )
        }
    }
}

package com.tiaosheng.counter.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tiaosheng.counter.data.db.ExerciseEntity
import com.tiaosheng.counter.ui.theme.BodyLabel
import com.tiaosheng.counter.ui.theme.CaptionHint
import com.tiaosheng.counter.ui.theme.HeadlineStat
import com.tiaosheng.counter.ui.theme.SportGreen
import com.tiaosheng.counter.ui.theme.SportOrange
import com.tiaosheng.counter.ui.theme.SurfaceDark
import com.tiaosheng.counter.ui.theme.TextDim
import com.tiaosheng.counter.ui.theme.TextWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    onBack: () -> Unit = {},
    viewModel: HistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .padding(top = 48.dp)
    ) {
        // Header
        Text(
            text = "运动记录",
            style = HeadlineStat,
            color = TextWhite,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        // Tab row
        TabRow(
            selectedTabIndex = uiState.selectedTab.ordinal,
            containerColor = SurfaceDark,
            contentColor = SportOrange,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    color = SportOrange,
                    modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab.ordinal])
                )
            }
        ) {
            HistoryViewModel.Tab.entries.forEach { tab ->
                Tab(
                    selected = uiState.selectedTab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    text = {
                        Text(
                            text = when (tab) {
                                HistoryViewModel.Tab.DAY -> "日"
                                HistoryViewModel.Tab.WEEK -> "周"
                                HistoryViewModel.Tab.MONTH -> "月"
                            },
                            color = if (uiState.selectedTab == tab) SportOrange else TextDim
                        )
                    }
                )
            }
        }

        // Summary card
        if (uiState.dailySummaries.isNotEmpty()) {
            SummaryCard(summaries = uiState.dailySummaries)
        }

        // Chart
        if (uiState.dailySummaries.isNotEmpty()) {
            val chartData = uiState.dailySummaries
                .take(7)
                .reversed()
                .map { it.totalCount }
            val chartLabels = uiState.dailySummaries
                .take(7)
                .reversed()
                .map { it.date.takeLast(5) }

            StatsChart(data = chartData, labels = chartLabels)

            Text(
                text = "最近 ${chartData.size} 天",
                style = CaptionHint,
                color = TextDim,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Record list
        if (uiState.records.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无运动记录",
                    style = BodyLabel,
                    color = TextDim
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.records, key = { it.id }) { record ->
                    RecordItem(record = record)
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun SummaryCard(summaries: List<HistoryViewModel.DailySummary>) {
    val totalCount = summaries.sumOf { it.totalCount }
    val totalMinutes = summaries.sumOf { it.totalDurationMinutes }
    val totalCalories = summaries.sumOf { it.totalCalories.toDouble() }.toFloat()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(value = "$totalCount", label = "总次数")
        StatItem(value = "$totalMinutes", label = "总时长(分)")
        StatItem(value = "%.0f".format(totalCalories), label = "总卡路里")
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = HeadlineStat,
            color = SportOrange
        )
        Text(
            text = label,
            style = CaptionHint,
            color = TextDim
        )
    }
}

@Composable
private fun RecordItem(record: ExerciseEntity) {
    val dateFormat = androidx.compose.runtime.remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = dateFormat.format(Date(record.startTime)),
                style = BodyLabel,
                color = TextWhite
            )
            Text(
                text = "${record.totalCount}次 | ${record.durationSeconds / 60}分 | ${record.avgBpm.toInt()}BPM",
                style = CaptionHint,
                color = TextDim
            )
        }
        Text(
            text = "%.1f kcal".format(record.calories),
            style = BodyLabel,
            color = SportGreen
        )
    }
}


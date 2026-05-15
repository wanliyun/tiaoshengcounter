package com.tiaosheng.counter.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tiaosheng.counter.data.db.AppDatabase
import com.tiaosheng.counter.data.db.ExerciseEntity
import com.tiaosheng.counter.data.repository.RecordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = RecordRepository(db.exerciseDao())

    data class DailySummary(
        val date: String,
        val totalCount: Int,
        val totalDurationMinutes: Int,
        val exerciseCount: Int,
        val totalCalories: Float
    )

    data class UiState(
        val records: List<ExerciseEntity> = emptyList(),
        val dailySummaries: List<DailySummary> = emptyList(),
        val isLoading: Boolean = false,
        val selectedTab: Tab = Tab.WEEK
    )

    enum class Tab { DAY, WEEK, MONTH }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadRecords()
        observeRecords()
    }

    private fun observeRecords() {
        viewModelScope.launch {
            repository.getRecordsForWeek(System.currentTimeMillis())
                .combine(_uiState) { records, state ->
                    state.copy(
                        records = records,
                        dailySummaries = buildDailySummaries(records)
                    )
                }
                .collect { _uiState.value = it }
        }
    }

    fun selectTab(tab: Tab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
        loadRecords()
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    private fun loadRecords() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val now = System.currentTimeMillis()
                val flow = when (_uiState.value.selectedTab) {
                    Tab.DAY -> repository.getRecordsForDay(now)
                    Tab.WEEK -> repository.getRecordsForWeek(now)
                    Tab.MONTH -> repository.getRecordsForMonth(now)
                }
                flow.collect { records ->
                    _uiState.value = _uiState.value.copy(
                        records = records,
                        dailySummaries = buildDailySummaries(records),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun buildDailySummaries(records: List<ExerciseEntity>): List<DailySummary> {
        return records
            .groupBy {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                sdf.format(java.util.Date(it.startTime))
            }
            .map { (date, dayRecords) ->
                DailySummary(
                    date = date,
                    totalCount = dayRecords.sumOf { it.totalCount },
                    totalDurationMinutes = dayRecords.sumOf { it.durationSeconds } / 60,
                    exerciseCount = dayRecords.size,
                    totalCalories = dayRecords.sumOf { it.calories.toDouble() }.toFloat()
                )
            }
            .sortedByDescending { it.date }
    }
}

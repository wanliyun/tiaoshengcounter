package com.tiaosheng.counter.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tiaosheng.counter.ExerciseMode
import com.tiaosheng.counter.counter.DetectionState
import com.tiaosheng.counter.counter.JumpDetector
import com.tiaosheng.counter.data.db.AppDatabase
import com.tiaosheng.counter.data.db.ExerciseEntity
import com.tiaosheng.counter.data.preferences.SettingsStore
import com.tiaosheng.counter.data.repository.RecordRepository
import com.tiaosheng.counter.pose.PoseEngine
import com.tiaosheng.counter.util.CalorieCalculator
import com.tiaosheng.counter.util.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = RecordRepository(db.exerciseDao())
    private val settingsStore = SettingsStore(application)
    private val jumpDetector = JumpDetector()
    val ttsManager = TtsManager(application)

    private var weightKg = SettingsStore.DEFAULT_WEIGHT_KG
    private var voiceInterval = SettingsStore.DEFAULT_VOICE_INTERVAL
    private var voiceEnabled = SettingsStore.DEFAULT_VOICE_ENABLED
    private var lastVoiceCount = 0

    data class UiState(
        val detectionState: DetectionState = DetectionState.IDLE,
        val count: Int = 0,
        val bpm: Float = 0f,
        val calories: Float = 0f,
        val elapsedSeconds: Int = 0,
        val isPaused: Boolean = false,
        val jumpMode: JumpDetector.JumpMode = JumpDetector.JumpMode.BOTH_FEET,
        val sensitivity: JumpDetector.Sensitivity = JumpDetector.Sensitivity.MEDIUM,
        val hasCameraError: Boolean = false,
        val hasPoseError: Boolean = false,
        val isLowLight: Boolean = false,
        val exerciseMode: ExerciseMode = ExerciseMode.Free,
        val remainingSeconds: Int = 0,
        val targetCount: Int = 0,
        val isCompleted: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var sessionStartMs = 0L
    private var elapsedTimerJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            weightKg = settingsStore.weightKg.first()
            voiceInterval = settingsStore.voiceInterval.first()
            voiceEnabled = settingsStore.voiceEnabled.first()
            val sensitivityStr = settingsStore.sensitivity.first()
            val sensitivity = when (sensitivityStr) {
                "low" -> JumpDetector.Sensitivity.LOW
                "high" -> JumpDetector.Sensitivity.HIGH
                else -> JumpDetector.Sensitivity.MEDIUM
            }
            jumpDetector.setSensitivity(sensitivity)
            _uiState.value = _uiState.value.copy(sensitivity = sensitivity)
        }
    }

    fun setExerciseMode(mode: ExerciseMode) {
        _uiState.value = _uiState.value.copy(
            exerciseMode = mode,
            remainingSeconds = if (mode is ExerciseMode.Timed) mode.durationSeconds else 0,
            targetCount = if (mode is ExerciseMode.Count) mode.targetCount else 0,
            isCompleted = false
        )
    }

    fun onPoseResult(poseResult: PoseEngine.PoseResult) {
        val result = jumpDetector.processFrame(poseResult)
        val state = _uiState.value

        // Auto-start timer when first entering COUNTING state
        if (result.state == DetectionState.COUNTING &&
            state.detectionState != DetectionState.COUNTING &&
            !state.isPaused
        ) {
            startTimer()
        }

        val newState = state.copy(
            detectionState = result.state,
            count = result.count,
            bpm = result.bpm,
            calories = CalorieCalculator.estimatePerJump(weightKg) * result.count
        )

        _uiState.value = newState

        // Check auto-stop for count mode
        val mode = state.exerciseMode
        if (mode is ExerciseMode.Count && result.count >= mode.targetCount) {
            ttsManager.speak("目标达成")
            stop()
            _uiState.value = _uiState.value.copy(isCompleted = true)
            return
        }

        // 语音播报
        if (voiceEnabled && result.state == DetectionState.COUNTING) {
            val milestone = (result.count / voiceInterval) * voiceInterval
            if (milestone > lastVoiceCount) {
                lastVoiceCount = milestone
                ttsManager.speak("$milestone")
            }
        }
    }

    fun setMode(mode: JumpDetector.JumpMode) {
        jumpDetector.setMode(mode)
        _uiState.value = _uiState.value.copy(jumpMode = mode)
    }

    fun setSensitivity(sensitivity: JumpDetector.Sensitivity) {
        jumpDetector.setSensitivity(sensitivity)
        _uiState.value = _uiState.value.copy(sensitivity = sensitivity)
        viewModelScope.launch {
            val key = when (sensitivity) {
                JumpDetector.Sensitivity.LOW -> "low"
                JumpDetector.Sensitivity.MEDIUM -> "medium"
                JumpDetector.Sensitivity.HIGH -> "high"
            }
            settingsStore.setSensitivity(key)
        }
    }

    fun pause() {
        jumpDetector.pause()
        elapsedTimerJob?.cancel()
        _uiState.value = _uiState.value.copy(isPaused = true)
    }

    fun resume() {
        jumpDetector.resume()
        startTimer()
        _uiState.value = _uiState.value.copy(isPaused = false)
    }

    fun stop() {
        val state = _uiState.value
        if (state.count > 0) {
            viewModelScope.launch {
                val record = ExerciseEntity(
                    startTime = sessionStartMs,
                    endTime = System.currentTimeMillis(),
                    totalCount = state.count,
                    mode = if (state.jumpMode == JumpDetector.JumpMode.BOTH_FEET) "both_feet" else "alternate",
                    avgBpm = state.bpm,
                    maxBpm = state.bpm,
                    calories = state.calories,
                    durationSeconds = state.elapsedSeconds
                )
                repository.save(record)
            }
        }
        jumpDetector.reset()
        elapsedTimerJob?.cancel()
        sessionStartMs = 0L
        lastVoiceCount = 0
        _uiState.value = UiState(
            exerciseMode = _uiState.value.exerciseMode  // 保留模式设置
        )
    }

    fun onCameraError() {
        _uiState.value = _uiState.value.copy(hasCameraError = true)
    }

    fun onPoseError() {
        _uiState.value = _uiState.value.copy(hasPoseError = true)
    }

    fun clearErrors() {
        _uiState.value = _uiState.value.copy(hasCameraError = false, hasPoseError = false)
    }

    private fun startTimer() {
        if (sessionStartMs == 0L) {
            sessionStartMs = System.currentTimeMillis()
        }
        elapsedTimerJob?.cancel()
        elapsedTimerJob = viewModelScope.launch {
            kotlinx.coroutines.delay(1000 - (System.currentTimeMillis() - sessionStartMs) % 1000)
            while (isActive) {
                val elapsed = ((System.currentTimeMillis() - sessionStartMs) / 1000).toInt()
                val mode = _uiState.value.exerciseMode

                if (mode is ExerciseMode.Timed) {
                    val remaining = (mode.durationSeconds - elapsed).coerceAtLeast(0)
                    _uiState.value = _uiState.value.copy(
                        elapsedSeconds = elapsed,
                        remainingSeconds = remaining
                    )
                    if (remaining <= 0) {
                        stop()
                        _uiState.value = _uiState.value.copy(isCompleted = true)
                        return@launch
                    }
                } else {
                    _uiState.value = _uiState.value.copy(elapsedSeconds = elapsed)
                }
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
        elapsedTimerJob?.cancel()
    }
}

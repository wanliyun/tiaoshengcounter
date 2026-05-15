package com.tiaosheng.counter.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tiaosheng.counter.data.preferences.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsStore = SettingsStore(application)

    data class UiState(
        val weightKg: Float = SettingsStore.DEFAULT_WEIGHT_KG,
        val sensitivity: String = SettingsStore.DEFAULT_SENSITIVITY,
        val voiceInterval: Int = SettingsStore.DEFAULT_VOICE_INTERVAL,
        val voiceEnabled: Boolean = SettingsStore.DEFAULT_VOICE_ENABLED,
        val cameraFacing: String = SettingsStore.DEFAULT_CAMERA_FACING
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = UiState(
                weightKg = settingsStore.weightKg.first(),
                sensitivity = settingsStore.sensitivity.first(),
                voiceInterval = settingsStore.voiceInterval.first(),
                voiceEnabled = settingsStore.voiceEnabled.first(),
                cameraFacing = settingsStore.cameraFacing.first()
            )
        }
    }

    fun setWeight(kg: Float) {
        _uiState.value = _uiState.value.copy(weightKg = kg)
        viewModelScope.launch { settingsStore.setWeightKg(kg) }
    }

    fun setSensitivity(value: String) {
        _uiState.value = _uiState.value.copy(sensitivity = value)
        viewModelScope.launch { settingsStore.setSensitivity(value) }
    }

    fun setVoiceInterval(interval: Int) {
        _uiState.value = _uiState.value.copy(voiceInterval = interval)
        viewModelScope.launch { settingsStore.setVoiceInterval(interval) }
    }

    fun setVoiceEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(voiceEnabled = enabled)
        viewModelScope.launch { settingsStore.setVoiceEnabled(enabled) }
    }

    fun setCameraFacing(facing: String) {
        _uiState.value = _uiState.value.copy(cameraFacing = facing)
        viewModelScope.launch { settingsStore.setCameraFacing(facing) }
    }
}

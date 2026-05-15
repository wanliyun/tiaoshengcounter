package com.tiaosheng.counter.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    companion object {
        private val KEY_WEIGHT_KG = floatPreferencesKey("weight_kg")
        private val KEY_SENSITIVITY = stringPreferencesKey("sensitivity")
        private val KEY_VOICE_INTERVAL = intPreferencesKey("voice_interval")
        private val KEY_VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        private val KEY_CAMERA_FACING = stringPreferencesKey("camera_facing")

        const val DEFAULT_WEIGHT_KG = 60f
        const val DEFAULT_SENSITIVITY = "medium"
        const val DEFAULT_VOICE_INTERVAL = 50
        const val DEFAULT_VOICE_ENABLED = true
        const val DEFAULT_CAMERA_FACING = "back"
    }

    val weightKg: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_WEIGHT_KG] ?: DEFAULT_WEIGHT_KG
    }

    val sensitivity: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SENSITIVITY] ?: DEFAULT_SENSITIVITY
    }

    val voiceInterval: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_VOICE_INTERVAL] ?: DEFAULT_VOICE_INTERVAL
    }

    val voiceEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_VOICE_ENABLED] ?: DEFAULT_VOICE_ENABLED
    }

    val cameraFacing: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CAMERA_FACING] ?: DEFAULT_CAMERA_FACING
    }

    suspend fun setWeightKg(weight: Float) {
        context.dataStore.edit { prefs -> prefs[KEY_WEIGHT_KG] = weight }
    }

    suspend fun setSensitivity(sensitivity: String) {
        context.dataStore.edit { prefs -> prefs[KEY_SENSITIVITY] = sensitivity }
    }

    suspend fun setVoiceInterval(interval: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_VOICE_INTERVAL] = interval }
    }

    suspend fun setVoiceEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_VOICE_ENABLED] = enabled }
    }

    suspend fun setCameraFacing(facing: String) {
        context.dataStore.edit { prefs -> prefs[KEY_CAMERA_FACING] = facing }
    }
}

package com.tiaosheng.counter.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var initialized = false
    private var pendingUtterance: String? = null

    fun initialize(language: String = "zh") {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = when (language) {
                    "zh" -> Locale.SIMPLIFIED_CHINESE
                    else -> Locale.ENGLISH
                }
                tts?.language = locale
                initialized = true
                pendingUtterance?.let { speak(it) }
                pendingUtterance = null
            }
        }
    }

    fun speak(text: String) {
        if (initialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tiaosheng_tts")
        } else {
            pendingUtterance = text
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        initialized = false
    }
}

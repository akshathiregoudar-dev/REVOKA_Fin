package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TextToSpeechManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var onDoneListener: (() -> Unit)? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTSManager", "Language not supported")
            } else {
                _isInitialized.value = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        onDoneListener?.invoke()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        onDoneListener?.invoke()
                    }
                })
            }
        } else {
            Log.e("TTSManager", "TTS Initialization failed")
        }
    }

    fun speak(text: String, onDone: () -> Unit = {}) {
        if (!_isInitialized.value || text.isBlank()) {
            onDone()
            return
        }

        stop()
        onDoneListener = onDone
        _isSpeaking.value = true
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "Utterance_${System.currentTimeMillis()}")
    }

    fun stop() {
        if (_isSpeaking.value) {
            tts?.stop()
            _isSpeaking.value = false
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isInitialized.value = false
        _isSpeaking.value = false
    }
}

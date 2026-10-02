package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.min

class AudioRecorderManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds.asStateFlow()

    private val _amplitudes = MutableStateFlow<List<Float>>(emptyList())
    val amplitudes: StateFlow<List<Float>> = _amplitudes.asStateFlow()

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun startRecording(jobCode: String): File? {
        try {
            stopRecording() // ensure any previous recording is closed

            val outputFile = File(context.cacheDir, "temp_${jobCode}_${System.currentTimeMillis()}.m4a")
            currentOutputFile = outputFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioSamplingRate(44100)
            recorder.setAudioEncodingBitRate(96000)
            recorder.setOutputFile(outputFile.absolutePath)

            recorder.prepare()
            recorder.start()

            mediaRecorder = recorder
            _isRecording.value = true
            _recordingDurationSeconds.value = 0
            _amplitudes.value = emptyList()

            startPolling()
            return outputFile
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Error starting recording", e)
            cleanup()
            return null
        }
    }

    private fun startPolling() {
        timerJob?.cancel()
        timerJob = scope.launch {
            var ticks = 0
            val ampList = mutableListOf<Float>()
            while (_isRecording.value) {
                delay(100)
                ticks++
                if (ticks % 10 == 0) {
                    _recordingDurationSeconds.value += 1
                }

                val maxAmp = try {
                    mediaRecorder?.maxAmplitude ?: 0
                } catch (e: Exception) {
                    0
                }

                // Normalize maxAmplitude (0 to ~32767) to 0.0f..1.0f
                val norm = min(1f, maxAmp / 20000f)
                ampList.add(norm)
                if (ampList.size > 40) {
                    ampList.removeAt(0)
                }
                _amplitudes.value = ampList.toList()
            }
        }
    }

    fun stopRecording(): File? {
        timerJob?.cancel()
        timerJob = null

        val recorder = mediaRecorder
        val file = currentOutputFile

        if (recorder != null) {
            try {
                if (_isRecording.value) {
                    recorder.stop()
                }
            } catch (e: Exception) {
                Log.e("AudioRecorderManager", "Error stopping recorder", e)
            } finally {
                try {
                    recorder.reset()
                    recorder.release()
                } catch (e: Exception) {
                    Log.e("AudioRecorderManager", "Error releasing recorder", e)
                }
            }
        }

        mediaRecorder = null
        _isRecording.value = false

        if (file != null && file.exists() && file.length() > 0) {
            return file
        }
        return null
    }

    fun cancelRecording() {
        stopRecording()
        currentOutputFile?.let { file ->
            if (file.exists()) {
                file.delete()
            }
        }
        currentOutputFile = null
        cleanup()
    }

    private fun cleanup() {
        _isRecording.value = false
        _recordingDurationSeconds.value = 0
        _amplitudes.value = emptyList()
        currentOutputFile = null
        mediaRecorder = null
    }
}

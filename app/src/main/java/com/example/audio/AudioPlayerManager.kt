package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class AudioPlayerManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentlyPlayingPath = MutableStateFlow<String?>(null)
    val currentlyPlayingPath: StateFlow<String?> = _currentlyPlayingPath.asStateFlow()

    fun playAudio(filePath: String, onCompletion: () -> Unit = {}) {
        stop()

        try {
            val file = File(filePath)
            if (!file.exists()) {
                Log.e("AudioPlayerManager", "File does not exist: $filePath")
                return
            }

            val player = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(file))
                prepare()
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentlyPlayingPath.value = null
                    onCompletion()
                }
                start()
            }

            mediaPlayer = player
            _isPlaying.value = true
            _currentlyPlayingPath.value = filePath
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error playing audio", e)
            stop()
        }
    }

    fun stop() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
                player.reset()
                player.release()
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "Error stopping player", e)
            }
        }
        mediaPlayer = null
        _isPlaying.value = false
        _currentlyPlayingPath.value = null
    }
}

package com.example.voice

import android.content.Context
import android.util.Log
import java.io.File

class PersonalizedVoiceCoordinator(
    private val context: Context,
    val profileManager: VoiceProfileManager = VoiceProfileManager(context),
    val voiceService: ElevenLabsVoiceService = ElevenLabsVoiceService()
) {

    val isVoiceCloningEnabled: Boolean
        get() = profileManager.isVoiceCloningConfigured()

    suspend fun enrollVoice(name: String, sampleFile: File): Result<String> {
        val apiKey = profileManager.getApiKey()
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("Please set your ElevenLabs API Key in Voice Profile"))
        }

        val result = voiceService.addVoice(name, sampleFile, apiKey)
        result.onSuccess { voiceId ->
            profileManager.setVoiceId(voiceId)
            profileManager.setVoiceName(name)
        }
        return result
    }

    suspend fun synthesizeVoiceForTask(text: String, jobCode: String): File? {
        if (!isVoiceCloningEnabled) {
            Log.d(TAG, "Voice cloning not configured, skipping personalized synthesis")
            return null
        }

        val apiKey = profileManager.getApiKey()
        val voiceId = profileManager.getVoiceId()
        val permanentDir = context.filesDir
        val outputFile = File(permanentDir, "cloned_${jobCode}_${System.currentTimeMillis()}.mp3")

        val result = voiceService.synthesizeSpeech(
            text = text,
            voiceId = voiceId,
            apiKey = apiKey,
            outputFile = outputFile
        )

        return result.getOrNull()
    }

    companion object {
        private const val TAG = "PersonalizedVoiceCoordinator"
    }
}

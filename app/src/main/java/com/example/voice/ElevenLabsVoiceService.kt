package com.example.voice

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class ElevenLabsVoiceService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    suspend fun addVoice(
        name: String,
        sampleFile: File,
        apiKey: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("ElevenLabs API Key is required"))
            }
            if (!sampleFile.exists() || sampleFile.length() == 0L) {
                return@withContext Result.failure(IllegalArgumentException("Voice sample file is empty or missing"))
            }

            val fileBody = sampleFile.asRequestBody("audio/*".toMediaTypeOrNull())
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("name", name)
                .addFormDataPart("description", "Enrolled user voice sample for Revoca reminders")
                .addFormDataPart("files", sampleFile.name, fileBody)
                .build()

            val request = Request.Builder()
                .url("$BASE_URL/voices/add")
                .addHeader("xi-api-key", apiKey)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Voice enrollment failed (${response.code}): $responseBody")
                return@withContext Result.failure(Exception("Enrollment failed: HTTP ${response.code}"))
            }

            val json = JSONObject(responseBody)
            val voiceId = json.optString("voice_id")
            if (voiceId.isNotBlank()) {
                Log.d(TAG, "Voice successfully enrolled with ID: $voiceId")
                Result.success(voiceId)
            } else {
                Result.failure(Exception("No voice_id in API response: $responseBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during voice enrollment", e)
            Result.failure(e)
        }
    }

    suspend fun synthesizeSpeech(
        text: String,
        voiceId: String,
        apiKey: String,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("ElevenLabs API Key is required"))
            }
            if (voiceId.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Voice ID is required"))
            }
            if (text.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Text content cannot be blank"))
            }

            val jsonBody = JSONObject().apply {
                put("text", text)
                put("model_id", "eleven_multilingual_v2")
                val voiceSettings = JSONObject().apply {
                    put("stability", 0.5)
                    put("similarity_boost", 0.8)
                }
                put("voice_settings", voiceSettings)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$BASE_URL/text-to-speech/$voiceId")
                .addHeader("xi-api-key", apiKey)
                .addHeader("Accept", "audio/mpeg")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorMsg = response.body?.string() ?: ""
                Log.e(TAG, "Speech synthesis failed (${response.code}): $errorMsg")
                return@withContext Result.failure(Exception("Synthesis failed: HTTP ${response.code}"))
            }

            val inputStream = response.body?.byteStream()
                ?: return@withContext Result.failure(Exception("Empty audio stream from API"))

            FileOutputStream(outputFile).use { outStream ->
                inputStream.copyTo(outStream)
            }

            Log.d(TAG, "Synthesized audio saved to: ${outputFile.absolutePath}, size: ${outputFile.length()} bytes")
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during speech synthesis", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "ElevenLabsVoiceService"
        private const val BASE_URL = "https://api.elevenlabs.io/v1"
    }
}

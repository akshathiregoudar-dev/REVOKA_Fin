package com.example.ai

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class GeminiTaskParser(
    private val context: Context,
    val configManager: GeminiConfigManager = GeminiConfigManager(context),
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    suspend fun parseTextReminder(
        userPrompt: String,
        referenceEpochMs: Long = System.currentTimeMillis()
    ): Result<ParsedReminder> = withContext(Dispatchers.IO) {
        val apiKey = configManager.getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Gemini API Key is not configured."))
        }

        try {
            val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()).format(Date(referenceEpochMs))

            val systemPrompt = "You are an intelligent task parser for a voice reminder app. " +
                    "The current user reference time is: $formattedDate (Epoch Ms: $referenceEpochMs). " +
                    "Analyze the user's natural language reminder. " +
                    "Calculate the exact target due timestamp in milliseconds (dueEpochMs). " +
                    "If no specific time is specified, default to 1 hour from the reference time. " +
                    "Return ONLY a JSON object with these exact keys: " +
                    "\"label\" (short title, max 4 words), " +
                    "\"spokenMessage\" (the concise spoken reminder), " +
                    "\"dueEpochMs\" (integer milliseconds epoch timestamp strictly in the future), " +
                    "\"dateDescription\" (plain language description, e.g. 'Tomorrow at 4:00 PM')."

            val jsonBody = JSONObject().apply {
                put("system_instruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply { put("text", systemPrompt) }))
                })
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply { put("text", userPrompt) }))
                }))
                put("generationConfig", JSONObject().apply {
                    put("response_mime_type", "application/json")
                    put("temperature", 0.1)
                })
            }

            executeGeminiRequest(jsonBody, apiKey, referenceEpochMs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse text with Gemini", e)
            Result.failure(e)
        }
    }

    suspend fun parseAudioReminder(
        audioFile: File,
        referenceEpochMs: Long = System.currentTimeMillis()
    ): Result<ParsedReminder> = withContext(Dispatchers.IO) {
        val apiKey = configManager.getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Gemini API Key is not configured."))
        }
        if (!audioFile.exists() || audioFile.length() == 0L) {
            return@withContext Result.failure(IllegalArgumentException("Audio file is missing or empty."))
        }

        try {
            val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()).format(Date(referenceEpochMs))
            val audioBytes = audioFile.readBytes()
            val audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

            val systemPrompt = "You are an intelligent audio assistant for a voice reminder app. " +
                    "The current user reference time is: $formattedDate (Epoch Ms: $referenceEpochMs). " +
                    "Listen to the user's audio reminder recording. " +
                    "Transcribe the voice message, extract the key task, and calculate the exact scheduled timestamp (dueEpochMs). " +
                    "If no specific time is specified in audio, default to 1 hour from now. " +
                    "Return ONLY a JSON object with these exact keys: " +
                    "\"label\" (short title, max 4 words), " +
                    "\"spokenMessage\" (concise transcription/summary to speak aloud), " +
                    "\"dueEpochMs\" (integer milliseconds epoch timestamp strictly in the future), " +
                    "\"dateDescription\" (plain language description, e.g. 'Tomorrow at 4:00 PM')."

            val jsonBody = JSONObject().apply {
                put("system_instruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply { put("text", systemPrompt) }))
                })
                put("contents", JSONArray().put(JSONObject().apply {
                    val partsArray = JSONArray()
                    partsArray.put(JSONObject().apply {
                        put("inline_data", JSONObject().apply {
                            put("mime_type", "audio/mp4")
                            put("data", audioBase64)
                        })
                    })
                    partsArray.put(JSONObject().apply {
                        put("text", "Extract task and schedule from this recorded audio.")
                    })
                    put("parts", partsArray)
                }))
                put("generationConfig", JSONObject().apply {
                    put("response_mime_type", "application/json")
                    put("temperature", 0.1)
                })
            }

            executeGeminiRequest(jsonBody, apiKey, referenceEpochMs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse audio with Gemini", e)
            Result.failure(e)
        }
    }

    private fun executeGeminiRequest(jsonBody: JSONObject, apiKey: String, referenceEpochMs: Long): Result<ParsedReminder> {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url("$BASE_URL?key=$apiKey")
            .post(jsonBody.toString().toRequestBody(mediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            Log.e(TAG, "Gemini API error (${response.code}): $responseBody")
            return Result.failure(Exception("Gemini error HTTP ${response.code}"))
        }

        val json = JSONObject(responseBody)
        val candidates = json.optJSONArray("candidates")
        val content = candidates?.optJSONObject(0)?.optJSONObject("content")
        val text = content?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: ""

        if (text.isBlank()) {
            return Result.failure(Exception("Empty response from Gemini"))
        }

        return parseResultJson(text, referenceEpochMs)
    }

    fun parseResultJson(jsonString: String, referenceEpochMs: Long): Result<ParsedReminder> {
        return try {
            val cleanJson = jsonString.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsed = JSONObject(cleanJson)

            val label = parsed.optString("label", "AI Reminder")
            val spokenMessage = parsed.optString("spokenMessage", "")
            var dueEpochMs = parsed.optLong("dueEpochMs", referenceEpochMs + 3600000L)
            if (dueEpochMs <= referenceEpochMs) {
                dueEpochMs = referenceEpochMs + 3600000L
            }
            val dateDescription = parsed.optString("dateDescription", "Scheduled by AI")

            Result.success(
                ParsedReminder(
                    label = label,
                    spokenMessage = spokenMessage,
                    dueEpochMs = dueEpochMs,
                    dateDescription = dateDescription
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON content: $jsonString", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "GeminiTaskParser"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
    }
}

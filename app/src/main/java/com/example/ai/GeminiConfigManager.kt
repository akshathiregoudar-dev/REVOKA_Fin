package com.example.ai

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

class GeminiConfigManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getApiKey(): String {
        val userKey = prefs.getString(KEY_GEMINI_KEY, "") ?: ""
        if (userKey.isNotBlank()) return userKey

        return try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            (field.get(null) as? String) ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    fun setApiKey(apiKey: String) {
        prefs.edit().putString(KEY_GEMINI_KEY, apiKey.trim()).apply()
    }

    fun isConfigured(): Boolean {
        return getApiKey().isNotBlank()
    }

    companion object {
        private const val PREFS_NAME = "revoca_gemini_prefs"
        private const val KEY_GEMINI_KEY = "gemini_api_key"
    }
}

package com.example.voice

import android.content.Context
import android.content.SharedPreferences

class VoiceProfileManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getApiKey(): String {
        return prefs.getString(KEY_API_KEY, "") ?: ""
    }

    fun setApiKey(apiKey: String) {
        prefs.edit().putString(KEY_API_KEY, apiKey.trim()).apply()
    }

    fun getVoiceId(): String {
        return prefs.getString(KEY_VOICE_ID, "") ?: ""
    }

    fun setVoiceId(voiceId: String) {
        prefs.edit().putString(KEY_VOICE_ID, voiceId.trim()).apply()
    }

    fun getVoiceName(): String {
        return prefs.getString(KEY_VOICE_NAME, "My Cloned Voice") ?: "My Cloned Voice"
    }

    fun setVoiceName(name: String) {
        prefs.edit().putString(KEY_VOICE_NAME, name.trim()).apply()
    }

    fun isVoiceCloningConfigured(): Boolean {
        return getApiKey().isNotBlank() && getVoiceId().isNotBlank()
    }

    fun clearVoiceProfile() {
        prefs.edit()
            .remove(KEY_VOICE_ID)
            .remove(KEY_VOICE_NAME)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "revoca_voice_profile_prefs"
        private const val KEY_API_KEY = "elevenlabs_api_key"
        private const val KEY_VOICE_ID = "elevenlabs_voice_id"
        private const val KEY_VOICE_NAME = "elevenlabs_voice_name"
    }
}

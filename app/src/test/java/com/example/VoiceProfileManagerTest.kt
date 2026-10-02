package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.voice.VoiceProfileManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoiceProfileManagerTest {

    @Test
    fun testVoiceProfileConfigurationFlow() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = VoiceProfileManager(context)

        manager.clearVoiceProfile()
        manager.setApiKey("")
        assertFalse(manager.isVoiceCloningConfigured())

        manager.setApiKey("test_xi_api_key_12345")
        manager.setVoiceId("test_voice_id_67890")
        manager.setVoiceName("My Custom Voice")

        assertTrue(manager.isVoiceCloningConfigured())
        assertEquals("test_xi_api_key_12345", manager.getApiKey())
        assertEquals("test_voice_id_67890", manager.getVoiceId())
        assertEquals("My Custom Voice", manager.getVoiceName())

        manager.clearVoiceProfile()
        assertFalse(manager.isVoiceCloningConfigured())
    }
}

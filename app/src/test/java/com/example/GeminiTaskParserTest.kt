package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.GeminiTaskParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeminiTaskParserTest {

    @Test
    fun testParseResultJsonWithCleanJson() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = GeminiTaskParser(context)

        val refTime = 1700000000000L
        val mockJson = """
            {
              "label": "Safety Inspection",
              "spokenMessage": "Inspect high-voltage panels on Sector 4",
              "dueEpochMs": 1700003600000,
              "dateDescription": "Today at 2:00 PM"
            }
        """.trimIndent()

        val result = parser.parseResultJson(mockJson, refTime)
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()

        assertEquals("Safety Inspection", parsed.label)
        assertEquals("Inspect high-voltage panels on Sector 4", parsed.spokenMessage)
        assertEquals(1700003600000L, parsed.dueEpochMs)
        assertEquals("Today at 2:00 PM", parsed.dateDescription)
    }

    @Test
    fun testParseResultJsonWithMarkdownFences() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = GeminiTaskParser(context)

        val refTime = 1700000000000L
        val mockJsonWithFences = """
            ```json
            {
              "label": "Concrete Pour",
              "spokenMessage": "Verify concrete temperature before pour",
              "dueEpochMs": 1700086400000,
              "dateDescription": "Tomorrow at 9:00 AM"
            }
            ```
        """.trimIndent()

        val result = parser.parseResultJson(mockJsonWithFences, refTime)
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()

        assertEquals("Concrete Pour", parsed.label)
        assertEquals("Tomorrow at 9:00 AM", parsed.dateDescription)
    }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.GeminiConfigManager
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.AmberVariant
import com.example.ui.theme.BorderNavy
import com.example.ui.theme.CharcoalNavyDark
import com.example.ui.theme.LedGreen
import com.example.ui.theme.OnAmber
import com.example.ui.theme.SurfaceHigh
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.voice.VoiceProfileManager

@Composable
fun VoiceProfileModal(
    profileManager: VoiceProfileManager,
    geminiManager: GeminiConfigManager,
    isRecordingSample: Boolean,
    recordingDurationSeconds: Int,
    onStartRecordSample: () -> Unit,
    onStopRecordSample: () -> Unit,
    onEnrollSample: (name: String) -> Unit,
    onTestVoice: (testText: String) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKey by remember { mutableStateOf(profileManager.getApiKey()) }
    var geminiKey by remember { mutableStateOf(geminiManager.getApiKey()) }
    var voiceId by remember { mutableStateOf(profileManager.getVoiceId()) }
    var voiceName by remember { mutableStateOf(profileManager.getVoiceName()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, BorderNavy, RoundedCornerShape(12.dp)),
            color = CharcoalNavyDark
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = AmberPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "AI & VOICE CONFIG",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val isGeminiActive = geminiManager.isConfigured()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isGeminiActive) LedGreen.copy(alpha = 0.15f) else SurfaceHigh)
                        .border(1.dp, if (isGeminiActive) LedGreen else BorderNavy, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isGeminiActive) LedGreen else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isGeminiActive) "Gemini Task Parser: Active" else "Gemini API Key Needed",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isGeminiActive) LedGreen else TextSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val isEnrolled = profileManager.isVoiceCloningConfigured()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isEnrolled) LedGreen.copy(alpha = 0.15f) else SurfaceHigh)
                        .border(1.dp, if (isEnrolled) LedGreen else BorderNavy, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isEnrolled) LedGreen else TextMuted)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEnrolled) "Cloned Voice: Active (${profileManager.getVoiceName()})" else "Voice: Device Standard TTS",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isEnrolled) LedGreen else TextSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "GEMINI API KEY (TASK PARSER)",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberPrimary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = geminiKey,
                    onValueChange = {
                        geminiKey = it
                        geminiManager.setApiKey(it)
                    },
                    placeholder = { Text("Paste Google AI / Gemini API key...", color = TextMuted, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceHigh,
                        unfocusedContainerColor = SurfaceHigh,
                        focusedBorderColor = AmberPrimary,
                        unfocusedBorderColor = BorderNavy,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "ELEVENLABS API KEY (VOICE CLONING)",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        profileManager.setApiKey(it)
                    },
                    placeholder = { Text("Paste xi-api-key here...", color = TextMuted, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceHigh,
                        unfocusedContainerColor = SurfaceHigh,
                        focusedBorderColor = AmberPrimary,
                        unfocusedBorderColor = BorderNavy,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "ELEVENLABS VOICE ID",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = voiceId,
                    onValueChange = {
                        voiceId = it
                        profileManager.setVoiceId(it)
                    },
                    placeholder = { Text("e.g. 21m00Tcm4TlvDq8ikWAM or enroll below", color = TextMuted, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceHigh,
                        unfocusedContainerColor = SurfaceHigh,
                        focusedBorderColor = AmberPrimary,
                        unfocusedBorderColor = BorderNavy,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "INSTANT VOICE ENROLLMENT",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberPrimary
                    )
                )
                Text(
                    text = "Record a 15–30 sec voice sample to clone your voice.",
                    style = TextStyle(
                        fontFamily = FontFamily.Default,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (isRecordingSample) {
                                onStopRecordSample()
                            } else {
                                onStartRecordSample()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecordingSample) Color(0xFFE53935) else SurfaceHigh,
                            contentColor = if (isRecordingSample) Color.White else AmberVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isRecordingSample) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRecordingSample) "Stop ($recordingDurationSeconds s)" else "Record Sample",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = { onEnrollSample(voiceName.ifBlank { "My Cloned Voice" }) },
                        enabled = !isRecordingSample && apiKey.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberPrimary,
                            contentColor = OnAmber
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Enroll Voice",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { onTestVoice("Hello! This is a test reminder in your voice.") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = AmberVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Test Current Voice",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = AmberVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (isEnrolled) {
                        OutlinedButton(
                            onClick = {
                                profileManager.clearVoiceProfile()
                                voiceId = ""
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Reset Voice", color = TextMuted, fontSize = 12.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberPrimary,
                            contentColor = OnAmber
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

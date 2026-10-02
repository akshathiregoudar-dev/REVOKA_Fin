package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import com.example.ui.theme.LedGreen
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ContentMode
import com.example.ui.DueTab
import com.example.ui.QuickPickPreset
import com.example.ui.UiState
import com.example.ui.formatPlainLanguageDue
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.AmberVariant
import com.example.ui.theme.BorderNavy
import com.example.ui.theme.CharcoalNavyDark
import com.example.ui.theme.LedRed
import com.example.ui.theme.OnAmber
import com.example.ui.theme.PanelNavy
import com.example.ui.theme.SurfaceHigh
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun NewEntryPanel(
    uiState: UiState,
    isRecording: Boolean,
    recordingDurationSeconds: Int,
    amplitudes: List<Float>,
    onContentModeChange: (ContentMode) -> Unit,
    onJobLabelChange: (String) -> Unit,
    onTypedMessageChange: (String) -> Unit,
    onDueTabChange: (DueTab) -> Unit,
    onSelectPreset: (QuickPickPreset) -> Unit,
    onCustomEpochChange: (Long) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onDiscardRecording: () -> Unit,
    onTogglePreviewPlayback: () -> Unit,
    onTestSpeak: () -> Unit,
    onAiParse: () -> Unit,
    onSaveReminder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PanelNavy)
            .border(1.dp, BorderNavy, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            // Header label
            Text(
                text = "NEW ENTRY",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                    color = TextMuted
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 1. Two Content Modes (Segmented Toggle: Record vs Type)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceHigh)
                    .padding(4.dp)
            ) {
                // Record Mode Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (uiState.contentMode == ContentMode.RECORD) AmberPrimary else Color.Transparent
                        )
                        .clickable { onContentModeChange(ContentMode.RECORD) }
                        .testTag("mode_record_tab"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Record",
                            tint = if (uiState.contentMode == ContentMode.RECORD) OnAmber else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Record",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (uiState.contentMode == ContentMode.RECORD) OnAmber else TextSecondary
                            )
                        )
                    }
                }

                // Type Mode Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (uiState.contentMode == ContentMode.TYPE) AmberPrimary else Color.Transparent
                        )
                        .clickable { onContentModeChange(ContentMode.TYPE) }
                        .testTag("mode_type_tab"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TextFields,
                            contentDescription = "Type",
                            tint = if (uiState.contentMode == ContentMode.TYPE) OnAmber else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Type",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (uiState.contentMode == ContentMode.TYPE) OnAmber else TextSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Content Body
            if (uiState.contentMode == ContentMode.RECORD) {
                // RECORD MODE UI
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (uiState.recordingFile == null && !isRecording) {
                        // Initial Record State
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(SurfaceHigh)
                                .border(1.dp, BorderNavy, CircleShape)
                                .clickable { onStartRecording() }
                                .testTag("record_mic_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(LedRed)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tap to record",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your voice memo plays back exactly when the job's due.",
                            style = TextStyle(
                                fontFamily = FontFamily.Default,
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        )
                    } else if (isRecording) {
                        // Active Recording State
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 0.95f,
                            targetValue = 1.12f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulseScale"
                        )

                        Box(
                            modifier = Modifier
                                .scale(pulseScale)
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(LedRed)
                                .clickable { onStopRecording() }
                                .testTag("stop_recording_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        val mins = recordingDurationSeconds / 60
                        val secs = recordingDurationSeconds % 60
                        val timerText = String.format(Locale.US, "%02d:%02d RECORDING...", mins, secs)

                        Text(
                            text = timerText,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = LedRed
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        WaveformCanvas(amplitudes = amplitudes)
                    } else {
                        // Recorded Preview Available
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceHigh)
                                .border(1.dp, AmberContainer, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = onTogglePreviewPlayback,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(AmberPrimary)
                                            .testTag("preview_play_button")
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.isPlayingPreview) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play Preview",
                                            tint = OnAmber
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Voice Memo Recorded",
                                            style = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = AmberVariant
                                            )
                                        )
                                        Text(
                                            text = "Tap to preview or re-record",
                                            style = TextStyle(
                                                fontFamily = FontFamily.Default,
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onDiscardRecording,
                                    modifier = Modifier.testTag("discard_recording_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Discard",
                                        tint = TextMuted
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onAiParse,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberPrimary,
                                contentColor = OnAmber
                            ),
                            shape = RoundedCornerShape(6.dp),
                            enabled = !uiState.isAiParsing,
                            modifier = Modifier.fillMaxWidth().testTag("ai_parse_audio_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (uiState.isAiParsing) "AI Transcribing & Scheduling..." else "AI Transcribe & Auto-Schedule",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // TYPE MODE UI
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.typedMessage,
                        onValueChange = onTypedMessageChange,
                        placeholder = {
                            Text(
                                "Type reminder message to be spoken aloud...",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("typed_message_input"),
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

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        Button(
                            onClick = onAiParse,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberPrimary,
                                contentColor = OnAmber
                            ),
                            shape = RoundedCornerShape(6.dp),
                            enabled = uiState.typedMessage.isNotBlank() && !uiState.isAiParsing,
                            modifier = Modifier.testTag("ai_parse_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (uiState.isAiParsing) "AI Scheduling..." else "AI Auto-Schedule",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onTestSpeak,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceHigh,
                                contentColor = AmberVariant
                            ),
                            shape = RoundedCornerShape(6.dp),
                            enabled = uiState.typedMessage.isNotBlank(),
                            modifier = Modifier.testTag("test_voice_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Test Voice",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Voice", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            if (uiState.aiStatusMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(LedGreen.copy(alpha = 0.15f))
                        .border(1.dp, LedGreen, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = LedGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.aiStatusMessage ?: "",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = LedGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Job Label
            Text(
                text = "JOB LABEL",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = uiState.jobLabel,
                onValueChange = onJobLabelChange,
                placeholder = {
                    Text(
                        "Short title for the log (optional)",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("job_label_input"),
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

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Due-time Section
            Text(
                text = "DUE",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Toggle Tabs: Quick Pick vs Custom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceHigh)
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (uiState.dueTab == DueTab.QUICK_PICK) AmberPrimary else Color.Transparent
                        )
                        .clickable { onDueTabChange(DueTab.QUICK_PICK) }
                        .testTag("due_tab_quick_pick"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Quick pick",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (uiState.dueTab == DueTab.QUICK_PICK) OnAmber else TextSecondary
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (uiState.dueTab == DueTab.CUSTOM) AmberPrimary else Color.Transparent
                        )
                        .clickable { onDueTabChange(DueTab.CUSTOM) }
                        .testTag("due_tab_custom"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Custom",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (uiState.dueTab == DueTab.CUSTOM) OnAmber else TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (uiState.dueTab == DueTab.QUICK_PICK) {
                // Quick Pick Presets Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(
                        QuickPickPreset.PLUS_1H to "+1 hour",
                        QuickPickPreset.PLUS_3H to "+3 hours",
                        QuickPickPreset.PLUS_1D to "+1 day",
                        QuickPickPreset.TOMORROW_9AM to "tomorrow 9am"
                    )

                    presets.forEach { (preset, label) ->
                        val isSelected = uiState.selectedPreset == preset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) AmberPrimary else SurfaceHigh)
                                .border(
                                    1.dp,
                                    if (isSelected) AmberPrimary else BorderNavy,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { onSelectPreset(preset) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) OnAmber else TextSecondary
                                )
                            )
                        }
                    }
                }
            } else {
                // Custom Datetime Selection
                val currentCal = Calendar.getInstance().apply { timeInMillis = uiState.customDueEpochMs }
                val dateFmt = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date(uiState.customDueEpochMs))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceHigh)
                        .border(1.dp, BorderNavy, RoundedCornerShape(8.dp))
                        .clickable {
                            // Launch DatePicker & TimePicker
                            val datePicker = DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val timePicker = TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            val newCal = Calendar.getInstance().apply {
                                                set(Calendar.YEAR, year)
                                                set(Calendar.MONTH, month)
                                                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                                set(Calendar.HOUR_OF_DAY, hourOfDay)
                                                set(Calendar.MINUTE, minute)
                                                set(Calendar.SECOND, 0)
                                            }
                                            onCustomEpochChange(newCal.timeInMillis)
                                        },
                                        currentCal.get(Calendar.HOUR_OF_DAY),
                                        currentCal.get(Calendar.MINUTE),
                                        false
                                    )
                                    timePicker.show()
                                },
                                currentCal.get(Calendar.YEAR),
                                currentCal.get(Calendar.MONTH),
                                currentCal.get(Calendar.DAY_OF_MONTH)
                            )
                            datePicker.show()
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                        .testTag("custom_datetime_picker_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateFmt,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Pick Date",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Plain language due summary
            Text(
                text = formatPlainLanguageDue(uiState.customDueEpochMs),
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = AmberPrimary
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 6. Save Button
            val canSave = (uiState.contentMode == ContentMode.RECORD && uiState.recordingFile != null) ||
                    (uiState.contentMode == ContentMode.TYPE && uiState.typedMessage.isNotBlank())

            Button(
                onClick = onSaveReminder,
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberPrimary,
                    contentColor = OnAmber,
                    disabledContainerColor = SurfaceHigh,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("log_reminder_button")
            ) {
                Text(
                    text = "LOG REMINDER",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                )
            }
        }
    }
}

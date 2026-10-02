package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ContentMode
import com.example.data.ReminderEntity
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.AmberVariant
import com.example.ui.theme.BorderNavy
import com.example.ui.theme.CharcoalNavyDark
import com.example.ui.theme.LedGreen
import com.example.ui.theme.LedRed
import com.example.ui.theme.OnAmber
import com.example.ui.theme.PanelNavy
import com.example.ui.theme.SurfaceHigh
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun FiringModal(
    reminder: ReminderEntity,
    onSnooze: (minutes: Int) -> Unit,
    onMarkDone: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "firing_pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaAnim"
    )

    Dialog(
        onDismissRequest = { /* dismiss disabled to ensure prompt action */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, LedRed.copy(alpha = alphaAnim), RoundedCornerShape(16.dp)),
                color = PanelNavy
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Alert Header Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(LedRed.copy(alpha = 0.2f))
                            .border(1.dp, LedRed, RoundedCornerShape(8.dp))
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Due",
                                tint = LedRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ALERT: JOB DUE // ${reminder.jobCode}",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = LedRed
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Mode Icon Badge
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(AmberPrimary)
                            .testTag("firing_modal_icon"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (reminder.contentMode == ContentMode.RECORD.name) Icons.Default.Mic else Icons.Default.TextFields,
                            contentDescription = null,
                            tint = OnAmber,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Title / Content
                    Text(
                        text = reminder.label.ifBlank { reminder.textMessage },
                        style = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        ),
                        maxLines = 3
                    )

                    if (reminder.contentMode == ContentMode.TYPE.name && reminder.textMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\"${reminder.textMessage}\"",
                            style = TextStyle(
                                fontFamily = FontFamily.Default,
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Playing indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AmberPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (reminder.contentMode == ContentMode.RECORD.name) "Playing Voice Memo..." else "Speaking Message...",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = AmberVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons (Snooze presets & Mark done)
                    Text(
                        text = "SNOOZE OPTIONS",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5 to "5m", 15 to "15m", 60 to "1h").forEach { (mins, label) ->
                            OutlinedButton(
                                onClick = { onSnooze(mins) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("snooze_${label}"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = TextPrimary
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Snooze,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onMarkDone,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LedGreen,
                            contentColor = OnAmber
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("firing_mark_done_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MARK COMPLETED",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

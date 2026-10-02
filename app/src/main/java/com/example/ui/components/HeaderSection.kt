package com.example.ui.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.BorderNavy
import com.example.ui.theme.LedGreen
import com.example.ui.theme.SurfaceHigh
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HeaderSection(
    currentTime: String,
    currentDate: String,
    activeCount: Int,
    isVoiceCloned: Boolean,
    onOpenVoiceProfile: () -> Unit,
    onClearLog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(LedGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SITE LOG",
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            letterSpacing = 1.5.sp,
                            color = TextPrimary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "voice reminders, logged by job",
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceHigh)
                        .border(1.dp, if (isVoiceCloned) LedGreen else BorderNavy, RoundedCornerShape(6.dp))
                        .clickable { onOpenVoiceProfile() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Voice Profile",
                            tint = if (isVoiceCloned) LedGreen else AmberPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isVoiceCloned) "VOICE: CLONED" else "VOICE: DEFAULT TTS",
                            style = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isVoiceCloned) LedGreen else TextSecondary
                            )
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (currentTime.isNotBlank()) currentTime else "12:00:00 PM",
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = AmberPrimary
                    )
                )
                Text(
                    text = if (currentDate.isNotBlank()) currentDate else "Today",
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "clear log",
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TextMuted
                    ),
                    modifier = Modifier.clickable { onClearLog() }
                )
            }
        }
    }
}

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ContentMode
import com.example.data.ReminderEntity
import com.example.ui.LogFilter
import com.example.ui.formatPlainLanguageDue
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.AmberVariant
import com.example.ui.theme.BorderNavy
import com.example.ui.theme.CharcoalNavyDark
import com.example.ui.theme.LedAmber
import com.example.ui.theme.LedGreen
import com.example.ui.theme.LedGrey
import com.example.ui.theme.LedRed
import com.example.ui.theme.OnAmber
import com.example.ui.theme.PanelNavy
import com.example.ui.theme.SurfaceHigh
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LogSection(
    reminders: List<ReminderEntity>,
    playingReminderId: Int?,
    activeFilter: LogFilter,
    onFilterChange: (LogFilter) -> Unit,
    onPlayReminder: (ReminderEntity) -> Unit,
    onToggleComplete: (ReminderEntity) -> Unit,
    onDeleteReminder: (ReminderEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredList = when (activeFilter) {
        LogFilter.ALL -> reminders
        LogFilter.PENDING -> reminders.filter { !it.isCompleted }
        LogFilter.COMPLETED -> reminders.filter { it.isCompleted }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header & Filter Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LOGBOOK [${filteredList.size}]",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.2.sp,
                    color = TextPrimary
                )
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceHigh)
                    .padding(2.dp)
            ) {
                listOf(LogFilter.ALL, LogFilter.PENDING, LogFilter.COMPLETED).forEach { filter ->
                    val isSel = activeFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSel) AmberPrimary else Color.Transparent)
                            .clickable { onFilterChange(filter) }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) OnAmber else TextSecondary
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PanelNavy)
                    .border(1.dp, BorderNavy, RoundedCornerShape(12.dp))
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NO LOGS FOUND",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextMuted
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Record a voice memo or type a reminder above.",
                        style = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredList.forEach { reminder ->
                    TicketCardItem(
                        reminder = reminder,
                        isPlaying = playingReminderId == reminder.id,
                        onPlayClick = { onPlayReminder(reminder) },
                        onToggleComplete = { onToggleComplete(reminder) },
                        onDeleteClick = { onDeleteReminder(reminder) }
                    )
                }
            }
        }
    }
}

@Composable
fun TicketCardItem(
    reminder: ReminderEntity,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onToggleComplete: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val now = System.currentTimeMillis()
    val isOverdue = !reminder.isCompleted && reminder.dueTimestamp <= now
    val isDueSoon = !reminder.isCompleted && !isOverdue && (reminder.dueTimestamp - now) <= 15 * 60 * 1000L

    val statusColor = when {
        reminder.isCompleted -> LedGrey
        isOverdue -> LedRed
        isDueSoon -> LedAmber
        else -> LedGreen
    }

    val statusLabel = when {
        reminder.isCompleted -> "COMPLETED"
        isOverdue -> "OVERDUE"
        isDueSoon -> "DUE SOON"
        else -> "PENDING"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (reminder.isCompleted) CharcoalNavyDark else PanelNavy)
            .border(
                1.dp,
                if (isOverdue) LedRed else if (isPlaying) AmberPrimary else BorderNavy,
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
            .testTag("ticket_card_${reminder.jobCode}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Perforated left LED bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(statusColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Main Content Area
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = reminder.jobCode,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = AmberPrimary
                        )
                    )

                    // Mode Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceHigh)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (reminder.contentMode == ContentMode.RECORD.name) Icons.Default.Mic else Icons.Default.TextFields,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (reminder.contentMode == ContentMode.RECORD.name) "VOICE" else "TYPED",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            )
                        }
                    }

                    // Status Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Title / Label
                Text(
                    text = reminder.label.ifBlank { reminder.textMessage.take(40) },
                    style = TextStyle(
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = if (reminder.isCompleted) TextMuted else TextPrimary,
                        textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Due Time Summary
                Text(
                    text = formatPlainLanguageDue(reminder.dueTimestamp),
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = if (isOverdue) LedRed else TextSecondary
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons (Play/Stop, Complete, Delete)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Play / Stop Button
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) AmberPrimary else SurfaceHigh)
                        .testTag("play_button_${reminder.jobCode}")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = if (isPlaying) OnAmber else TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Mark Complete Checkbox
                IconButton(
                    onClick = onToggleComplete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("complete_button_${reminder.jobCode}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Complete",
                        tint = if (reminder.isCompleted) LedGreen else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Delete Button
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_button_${reminder.jobCode}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.alarm.ReminderScheduler
import com.example.data.RevocaDatabase
import com.example.service.ReminderAlarmService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, -1)

        Log.d(TAG, "Received broadcast action: $action for reminderId: $reminderId")

        when (action) {
            ACTION_TRIGGER_REMINDER -> {
                if (reminderId != -1) {
                    val serviceIntent = Intent(context, ReminderAlarmService::class.java).apply {
                        this.action = ReminderAlarmService.ACTION_START_ALARM
                        putExtra(EXTRA_REMINDER_ID, reminderId)
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                }
            }

            ACTION_DISMISS_REMINDER -> {
                stopAlarmService(context)

                if (reminderId != -1) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = RevocaDatabase.getDatabase(context.applicationContext)
                            val reminder = db.reminderDao().getReminderById(reminderId)
                            if (reminder != null) {
                                db.reminderDao().updateReminder(reminder.copy(isCompleted = true))
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error completing reminder $reminderId", e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }

            ACTION_SNOOZE_REMINDER -> {
                stopAlarmService(context)

                val snoozeMins = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)
                if (reminderId != -1) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = RevocaDatabase.getDatabase(context.applicationContext)
                            val reminder = db.reminderDao().getReminderById(reminderId)
                            if (reminder != null) {
                                val newDueTime = System.currentTimeMillis() + (snoozeMins * 60 * 1000L)
                                val snoozed = reminder.copy(dueTimestamp = newDueTime, isCompleted = false)
                                db.reminderDao().updateReminder(snoozed)
                                ReminderScheduler.schedule(context.applicationContext, snoozed)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error snoozing reminder $reminderId", e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }

    private fun stopAlarmService(context: Context) {
        val stopIntent = Intent(context, ReminderAlarmService::class.java).apply {
            action = ReminderAlarmService.ACTION_STOP_SERVICE
        }
        context.startService(stopIntent)
    }

    companion object {
        const val TAG = "ReminderBroadcastReceiver"
        const val ACTION_TRIGGER_REMINDER = "com.example.ACTION_TRIGGER_REMINDER"
        const val ACTION_DISMISS_REMINDER = "com.example.ACTION_DISMISS_REMINDER"
        const val ACTION_SNOOZE_REMINDER = "com.example.ACTION_SNOOZE_REMINDER"

        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
    }
}

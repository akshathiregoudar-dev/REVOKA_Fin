package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.ReminderEntity
import com.example.receiver.ReminderBroadcastReceiver

object ReminderScheduler {
    private const val TAG = "ReminderScheduler"

    fun schedule(context: Context, reminder: ReminderEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val dueTime = reminder.dueTimestamp

        if (dueTime <= System.currentTimeMillis()) {
            Log.w(TAG, "Cannot schedule reminder ${reminder.id} with past timestamp: $dueTime")
            return
        }

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminder.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        dueTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        dueTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    dueTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled exact alarm for reminder ${reminder.id} at $dueTime")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling exact alarm, falling back to standard alarm", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                dueTime,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule reminder ${reminder.id}", e)
        }
    }

    fun cancel(context: Context, reminderId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm for reminder $reminderId")
        }
    }
}

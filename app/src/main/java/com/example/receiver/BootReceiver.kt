package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.alarm.ReminderScheduler
import com.example.data.RevocaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d(TAG, "Device rebooted or package replaced ($action). Rescheduling pending reminders...")

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = RevocaDatabase.getDatabase(context.applicationContext)
                    val now = System.currentTimeMillis()
                    val pendingReminders = db.reminderDao().getPendingReminders(now)

                    Log.d(TAG, "Found ${pendingReminders.size} pending reminders to reschedule.")
                    for (reminder in pendingReminders) {
                        ReminderScheduler.schedule(context.applicationContext, reminder)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reschedule reminders on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
        val missedReminders = db.reminderDao().getMissedReminders(now)
    for (missed in missedReminders) {
    val triggerIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
        action = ReminderBroadcastReceiver.ACTION_TRIGGER_REMINDER
        putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, missed.id)
    }
    context.sendBroadcast(triggerIntent)
}
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}

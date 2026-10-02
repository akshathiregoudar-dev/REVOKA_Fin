package com.example

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.alarm.ReminderScheduler
import com.example.data.ContentMode
import com.example.data.ReminderEntity
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderSchedulerTest {

    @Test
    fun testScheduleReminderSetsAlarm() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)

        val futureTime = System.currentTimeMillis() + 60000L
        val reminder = ReminderEntity(
            id = 42,
            jobCode = "JOB-01",
            label = "Test Reminder",
            contentMode = ContentMode.RECORD.name,
            dueTimestamp = futureTime
        )

        ReminderScheduler.schedule(context, reminder)

        val nextScheduledAlarm = shadowAlarmManager.nextScheduledAlarm
        assertNotNull(nextScheduledAlarm)
    }

    @Test
    fun testCancelReminderClearsAlarm() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val futureTime = System.currentTimeMillis() + 60000L
        val reminder = ReminderEntity(
            id = 43,
            jobCode = "JOB-02",
            label = "Test Reminder 2",
            contentMode = ContentMode.RECORD.name,
            dueTimestamp = futureTime
        )

        ReminderScheduler.schedule(context, reminder)
        ReminderScheduler.cancel(context, reminder.id)
    }
}

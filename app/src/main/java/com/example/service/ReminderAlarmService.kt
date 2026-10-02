package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.ContentMode
import com.example.data.ReminderEntity
import com.example.data.RevocaDatabase
import com.example.receiver.ReminderBroadcastReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class ReminderAlarmService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var vibrator: Vibrator? = null
    private var currentReminder: ReminderEntity? = null
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "RevocaHub:ReminderWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(3 * 60 * 1000L)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring wake lock", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent.action) {
            ACTION_STOP_SERVICE -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_ALARM -> {
                val reminderId = intent.getIntExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, -1)
                if (reminderId == -1) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                loadAndTriggerReminder(reminderId)
            }
        }

        return START_STICKY
    }

    private fun loadAndTriggerReminder(reminderId: Int) {
        serviceScope.launch {
            val db = RevocaDatabase.getDatabase(applicationContext)
            val reminder = db.reminderDao().getReminderById(reminderId)
            if (reminder == null || reminder.isCompleted) {
                stopSelf()
                return@launch
            }
            currentReminder = reminder

            val notification = buildForegroundNotification(reminder)
            startForeground(NOTIFICATION_ID, notification)

            startVibration()
            playReminderAudio(reminder)
        }
    }

    private fun playReminderAudio(reminder: ReminderEntity) {
        if (reminder.contentMode == ContentMode.RECORD.name && !reminder.audioFilePath.isNullOrEmpty()) {
            val file = File(reminder.audioFilePath)
            if (file.exists()) {
                try {
                    mediaPlayer?.release()
                    val player = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                        )
                        setDataSource(applicationContext, Uri.fromFile(file))
                        isLooping = true
                        prepare()
                        start()
                    }
                    mediaPlayer = player
                } catch (e: Exception) {
                    Log.e(TAG, "Error playing audio file in service", e)
                }
            }
        } else if (reminder.textMessage.isNotBlank()) {
            initTtsAndSpeak(reminder.textMessage)
        }
    }

    private fun initTtsAndSpeak(text: String) {
        tts = TextToSpeech(applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ALARM_TTS_${System.currentTimeMillis()}")
            }
        }
    }

    private fun startVibration() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            val pattern = longArrayOf(0, 600, 400, 600, 400, 600)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering vibration", e)
        }
    }

    private fun buildForegroundNotification(reminder: ReminderEntity): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminder.id)
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            reminder.id,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_DISMISS_REMINDER
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminder.id)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            reminder.id,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_SNOOZE_REMINDER
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderBroadcastReceiver.EXTRA_SNOOZE_MINUTES, 10)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            reminder.id,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (reminder.contentMode == ContentMode.RECORD.name) {
            "Playing voice memo..."
        } else {
            reminder.textMessage.ifBlank { "Scheduled Reminder" }
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(reminder.label.ifBlank { "Reminder: ${reminder.jobCode}" })
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openPendingIntent)
            .setFullScreenIntent(openPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Complete", dismissPendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze 10m", snoozePendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Revoca Reminder Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority notifications for firing voice and text reminders"
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()

        try {
            vibrator?.cancel()
        } catch (_: Exception) {}

        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}

        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (_: Exception) {}

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
    }

    companion object {
        const val TAG = "ReminderAlarmService"
        const val CHANNEL_ID = "revoca_reminder_channel_v1"
        const val NOTIFICATION_ID = 9001
        const val ACTION_START_ALARM = "com.example.service.ACTION_START_ALARM"
        const val ACTION_STOP_SERVICE = "com.example.service.ACTION_STOP_SERVICE"
    }
}

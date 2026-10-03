package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.ContentMode
import com.example.receiver.ReminderBroadcastReceiver
import com.example.ui.RevocaViewModel
import com.example.ui.components.FiringModal
import com.example.ui.components.HeaderSection
import com.example.ui.components.LogSection
import com.example.ui.components.NewEntryPanel
import com.example.ui.components.VoiceProfileModal
import com.example.ui.theme.AmberVariant
import com.example.ui.theme.CharcoalNavyDark
import com.example.ui.theme.LedAmber
import com.example.ui.theme.RevocaHubTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
    private val viewModel: RevocaViewModel by viewModels()

    private val requestMicPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setMicPermissionGranted(isGranted)
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setNotificationPermissionGranted(isGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setupLockScreenFlags()
        {
            private fun setupLockScreenFlags() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        km?.requestDismissKeyguard(this, null)
    } else {
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
    }
}
        }
        checkMicrophonePermission()
        checkNotificationPermission()

        handleNotificationIntent(intent)

        setContent {
            RevocaHubTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = CharcoalNavyDark
                ) { innerPadding ->
                    RevocaHubApp(
                        viewModel = viewModel,
                        onRequestMicPermission = { checkMicrophonePermission() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val reminderId = intent?.getIntExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, -1) ?: -1
        if (reminderId != -1) {
            viewModel.triggerFiringById(reminderId)
        }
    }

    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
    }

    private fun checkMicrophonePermission() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        viewModel.setMicPermissionGranted(hasPermission)

        if (!hasPermission) {
            requestMicPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            viewModel.setNotificationPermissionGranted(hasPermission)

            if (!hasPermission) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun RevocaHubApp(
    viewModel: RevocaViewModel,
    onRequestMicPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val reminders by viewModel.allReminders.collectAsState()

    val isRecording by viewModel.recorderManager.isRecording.collectAsState()
    val recordingDuration by viewModel.recorderManager.recordingDurationSeconds.collectAsState()
    val amplitudes by viewModel.recorderManager.amplitudes.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalNavyDark)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            HeaderSection(
                currentTime = uiState.currentTimeString,
                currentDate = uiState.currentDateString,
                activeCount = reminders.count { !it.isCompleted },
                isVoiceCloned = uiState.isVoiceCloningActive,
                onOpenVoiceProfile = { viewModel.setVoiceProfileModalVisible(true) },
                onClearLog = { viewModel.clearAllLogs() }
            )

            if (uiState.userErrorMessage != null) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LedAmber.copy(alpha = 0.15f))
                        .border(1.dp, LedAmber, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .testTag("error_banner")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = LedAmber
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = uiState.userErrorMessage ?: "",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Default,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Tap here to switch to Type Mode",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = AmberVariant
                                    ),
                                    modifier = Modifier.clickable {
                                        viewModel.setContentMode(ContentMode.TYPE)
                                        viewModel.clearUserErrorMessage()
                                    }
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.clearUserErrorMessage() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            NewEntryPanel(
                uiState = uiState,
                isRecording = isRecording,
                recordingDurationSeconds = recordingDuration,
                amplitudes = amplitudes,
                onContentModeChange = { viewModel.setContentMode(it) },
                onJobLabelChange = { viewModel.setJobLabel(it) },
                onTypedMessageChange = { viewModel.setTypedMessage(it) },
                onDueTabChange = { viewModel.setDueTab(it) },
                onSelectPreset = { viewModel.selectPreset(it) },
                onCustomEpochChange = { viewModel.updateCustomDueEpoch(it) },
                onStartRecording = {
                    if (!uiState.isMicPermissionGranted) {
                        onRequestMicPermission()
                    }
                    viewModel.startRecording()
                },
                onStopRecording = { viewModel.stopRecording() },
                onDiscardRecording = { viewModel.discardRecording() },
                onTogglePreviewPlayback = { viewModel.togglePreviewPlayback() },
                onTestSpeak = { viewModel.testSpeakTypedMessage() },
                onAiParse = { viewModel.parseWithAi() },
                onSaveReminder = { viewModel.saveReminder() },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            LogSection(
                reminders = reminders,
                playingReminderId = uiState.playingReminderId,
                activeFilter = uiState.logFilter,
                onFilterChange = { viewModel.setLogFilter(it) },
                onPlayReminder = { viewModel.playReminderCard(it) },
                onToggleComplete = { viewModel.toggleCompleteReminder(it) },
                onDeleteReminder = { viewModel.deleteReminder(it) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        val firing = uiState.firingReminder
        if (firing != null) {
            FiringModal(
                reminder = firing,
                onSnooze = { mins -> viewModel.snoozeFiringReminder(mins) },
                onMarkDone = { viewModel.dismissFiringModalAndComplete() }
            )
        }

        if (uiState.showVoiceProfileModal) {
            VoiceProfileModal(
                profileManager = viewModel.voiceCoordinator.profileManager,
                geminiManager = viewModel.geminiParser.configManager,
                isRecordingSample = uiState.isRecordingVoiceSample,
                recordingDurationSeconds = recordingDuration,
                onStartRecordSample = { viewModel.startEnrollmentRecording() },
                onStopRecordSample = { viewModel.stopEnrollmentRecording() },
                onEnrollSample = { name -> viewModel.enrollVoiceSample(name) },
                onTestVoice = { text -> viewModel.testSpeakTypedMessage(text) },
                onDismiss = { viewModel.setVoiceProfileModalVisible(false) }
            )
        }
    }
}

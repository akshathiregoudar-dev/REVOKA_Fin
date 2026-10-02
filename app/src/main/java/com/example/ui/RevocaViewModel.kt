package com.example.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiTaskParser
import com.example.ai.ParsedReminder
import com.example.alarm.ReminderScheduler
import com.example.audio.AudioPlayerManager
import com.example.audio.AudioRecorderManager
import com.example.audio.TextToSpeechManager
import com.example.data.ContentMode
import com.example.data.ReminderEntity
import com.example.data.ReminderRepository
import com.example.data.RevocaDatabase
import com.example.service.ReminderAlarmService
import com.example.voice.PersonalizedVoiceCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DueTab {
    QUICK_PICK,
    CUSTOM
}

enum class QuickPickPreset {
    PLUS_1H,
    PLUS_3H,
    PLUS_1D,
    TOMORROW_9AM,
    NONE
}

enum class LogFilter {
    ALL,
    PENDING,
    COMPLETED
}

data class UiState(
    val currentTimeString: String = "",
    val currentDateString: String = "",
    val contentMode: ContentMode = ContentMode.RECORD,
    val jobLabel: String = "",
    val typedMessage: String = "",
    val dueTab: DueTab = DueTab.QUICK_PICK,
    val selectedPreset: QuickPickPreset = QuickPickPreset.PLUS_1H,
    val customDueEpochMs: Long = System.currentTimeMillis() + 3600000L,
    val isMicPermissionGranted: Boolean = true,
    val isNotificationPermissionGranted: Boolean = true,
    val recordingFile: File? = null,
    val isPlayingPreview: Boolean = false,
    val logFilter: LogFilter = LogFilter.ALL,
    val firingReminder: ReminderEntity? = null,
    val playingReminderId: Int? = null,
    val userErrorMessage: String? = null,
    val showVoiceProfileModal: Boolean = false,
    val isRecordingVoiceSample: Boolean = false,
    val voiceSampleFile: File? = null,
    val isVoiceCloningActive: Boolean = false,
    val isAiParsing: Boolean = false,
    val aiStatusMessage: String? = null
)

class RevocaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ReminderRepository
    val recorderManager = AudioRecorderManager(application)
    val playerManager = AudioPlayerManager(application)
    val ttsManager = TextToSpeechManager(application)
    val voiceCoordinator = PersonalizedVoiceCoordinator(application)
    val geminiParser = GeminiTaskParser(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val allReminders: StateFlow<List<ReminderEntity>>

    init {
        val dao = RevocaDatabase.getDatabase(application).reminderDao()
        repository = ReminderRepository(dao)

        allReminders = repository.allReminders.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        updateVoiceCloningStatus()
        calculateInitialDueTime()

        viewModelScope.launch(Dispatchers.Main) {
            while (true) {
                updateClockAndCheckFiring()
                delay(1000)
            }
        }
    }

    fun updateVoiceCloningStatus() {
        _uiState.value = _uiState.value.copy(
            isVoiceCloningActive = voiceCoordinator.isVoiceCloningEnabled
        )
    }

    private fun calculateInitialDueTime() {
        val defaultEpoch = System.currentTimeMillis() + 3600000L
        _uiState.value = _uiState.value.copy(
            selectedPreset = QuickPickPreset.PLUS_1H,
            customDueEpochMs = defaultEpoch
        )
    }

    private fun updateClockAndCheckFiring() {
        val now = System.currentTimeMillis()
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())

        val timeStr = timeFormat.format(Date(now))
        val dateStr = dateFormat.format(Date(now))

        _uiState.value = _uiState.value.copy(
            currentTimeString = timeStr,
            currentDateString = dateStr
        )

        if (_uiState.value.firingReminder == null) {
            val remindersList = allReminders.value
            val dueItem = remindersList.firstOrNull { reminder ->
                !reminder.isCompleted && reminder.dueTimestamp <= now
            }
            if (dueItem != null) {
                triggerFiringModal(dueItem)
            }
        }
    }

    private fun triggerFiringModal(reminder: ReminderEntity) {
        stopBackgroundAlarmService()
        _uiState.value = _uiState.value.copy(firingReminder = reminder)

        if (!reminder.audioFilePath.isNullOrEmpty()) {
            playerManager.playAudio(reminder.audioFilePath)
        } else if (reminder.textMessage.isNotBlank()) {
            ttsManager.speak(reminder.textMessage)
        }
    }

    fun triggerFiringById(reminderId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val reminder = repository.getById(reminderId)
            if (reminder != null && !reminder.isCompleted) {
                viewModelScope.launch(Dispatchers.Main) {
                    triggerFiringModal(reminder)
                }
            }
        }
    }

    private fun stopBackgroundAlarmService() {
        try {
            val stopIntent = Intent(getApplication(), ReminderAlarmService::class.java).apply {
                action = ReminderAlarmService.ACTION_STOP_SERVICE
            }
            getApplication<Application>().startService(stopIntent)
        } catch (_: Exception) {}
    }

    fun setContentMode(mode: ContentMode) {
        _uiState.value = _uiState.value.copy(contentMode = mode)
    }

    fun setJobLabel(label: String) {
        _uiState.value = _uiState.value.copy(jobLabel = label)
    }

    fun setTypedMessage(msg: String) {
        _uiState.value = _uiState.value.copy(typedMessage = msg)
    }

    fun setDueTab(tab: DueTab) {
        _uiState.value = _uiState.value.copy(dueTab = tab)
    }

    fun setVoiceProfileModalVisible(visible: Boolean) {
        updateVoiceCloningStatus()
        _uiState.value = _uiState.value.copy(showVoiceProfileModal = visible)
    }

    fun selectPreset(preset: QuickPickPreset) {
        val now = System.currentTimeMillis()
        val targetMs = when (preset) {
            QuickPickPreset.PLUS_1H -> now + 3600000L
            QuickPickPreset.PLUS_3H -> now + 10800000L
            QuickPickPreset.PLUS_1D -> now + 86400000L
            QuickPickPreset.TOMORROW_9AM -> {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                cal.timeInMillis
            }
            QuickPickPreset.NONE -> _uiState.value.customDueEpochMs
        }

        _uiState.value = _uiState.value.copy(
            selectedPreset = preset,
            customDueEpochMs = targetMs
        )
    }

    fun updateCustomDueEpoch(epochMs: Long) {
        _uiState.value = _uiState.value.copy(
            selectedPreset = QuickPickPreset.NONE,
            customDueEpochMs = epochMs
        )
    }

    fun setMicPermissionGranted(granted: Boolean) {
        _uiState.value = _uiState.value.copy(isMicPermissionGranted = granted)
    }

    fun setNotificationPermissionGranted(granted: Boolean) {
        _uiState.value = _uiState.value.copy(isNotificationPermissionGranted = granted)
    }

    fun startRecording() {
        if (!_uiState.value.isMicPermissionGranted) {
            _uiState.value = _uiState.value.copy(
                userErrorMessage = "Microphone permission is required for voice recording."
            )
            return
        }

        val jobCode = "TEMP"
        val file = recorderManager.startRecording(jobCode)
        if (file == null) {
            _uiState.value = _uiState.value.copy(
                userErrorMessage = "Could not access microphone. You can switch to Type mode."
            )
        } else {
            _uiState.value = _uiState.value.copy(
                recordingFile = null,
                userErrorMessage = null
            )
        }
    }

    fun stopRecording() {
        val file = recorderManager.stopRecording()
        _uiState.value = _uiState.value.copy(recordingFile = file)
    }

    fun discardRecording() {
        recorderManager.cancelRecording()
        playerManager.stop()
        _uiState.value = _uiState.value.copy(
            recordingFile = null,
            isPlayingPreview = false
        )
    }

    fun togglePreviewPlayback() {
        val file = _uiState.value.recordingFile ?: return
        if (_uiState.value.isPlayingPreview) {
            playerManager.stop()
            _uiState.value = _uiState.value.copy(isPlayingPreview = false)
        } else {
            _uiState.value = _uiState.value.copy(isPlayingPreview = true)
            playerManager.playAudio(file.absolutePath) {
                _uiState.value = _uiState.value.copy(isPlayingPreview = false)
            }
        }
    }

    fun startEnrollmentRecording() {
        val sampleFile = recorderManager.startRecording("ENROLLMENT")
        if (sampleFile != null) {
            _uiState.value = _uiState.value.copy(
                isRecordingVoiceSample = true,
                voiceSampleFile = null
            )
        }
    }

    fun stopEnrollmentRecording() {
        val file = recorderManager.stopRecording()
        _uiState.value = _uiState.value.copy(
            isRecordingVoiceSample = false,
            voiceSampleFile = file
        )
    }

    fun enrollVoiceSample(name: String) {
        val file = _uiState.value.voiceSampleFile
        if (file == null || !file.exists()) {
            _uiState.value = _uiState.value.copy(
                userErrorMessage = "Please record a voice sample first."
            )
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val result = voiceCoordinator.enrollVoice(name, file)
            viewModelScope.launch(Dispatchers.Main) {
                result.onSuccess {
                    updateVoiceCloningStatus()
                    _uiState.value = _uiState.value.copy(
                        userErrorMessage = "Voice cloned successfully!",
                        voiceSampleFile = null
                    )
                }.onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        userErrorMessage = "Voice enrollment failed: ${err.localizedMessage}"
                    )
                }
            }
        }
    }

    fun parseWithAi() {
        val state = _uiState.value
        _uiState.value = _uiState.value.copy(isAiParsing = true, aiStatusMessage = "Gemini is analyzing reminder...")

        viewModelScope.launch(Dispatchers.IO) {
            val result: Result<ParsedReminder> = if (state.contentMode == ContentMode.TYPE && state.typedMessage.isNotBlank()) {
                geminiParser.parseTextReminder(state.typedMessage)
            } else if (state.contentMode == ContentMode.RECORD && state.recordingFile != null) {
                geminiParser.parseAudioReminder(state.recordingFile)
            } else {
                Result.failure(IllegalArgumentException("Please provide a typed message or recorded voice note first."))
            }

            viewModelScope.launch(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(isAiParsing = false)
                result.onSuccess { parsed ->
                    _uiState.value = _uiState.value.copy(
                        jobLabel = parsed.label,
                        typedMessage = if (state.contentMode == ContentMode.TYPE) parsed.spokenMessage else state.typedMessage,
                        customDueEpochMs = parsed.dueEpochMs,
                        selectedPreset = QuickPickPreset.NONE,
                        dueTab = DueTab.CUSTOM,
                        aiStatusMessage = "AI scheduled: ${parsed.dateDescription}"
                    )
                }.onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        userErrorMessage = "AI parsing failed: ${err.localizedMessage}",
                        aiStatusMessage = null
                    )
                }
            }
        }
    }

    fun testSpeakTypedMessage(customText: String? = null) {
        val text = customText ?: _uiState.value.typedMessage
        if (text.isBlank()) return

        if (voiceCoordinator.isVoiceCloningEnabled) {
            viewModelScope.launch(Dispatchers.IO) {
                val synthesized = voiceCoordinator.synthesizeVoiceForTask(text, "TEST")
                viewModelScope.launch(Dispatchers.Main) {
                    if (synthesized != null && synthesized.exists()) {
                        playerManager.playAudio(synthesized.absolutePath)
                    } else {
                        ttsManager.speak(text)
                    }
                }
            }
        } else {
            ttsManager.speak(text)
        }
    }

    fun saveReminder() {
        val state = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            val count = repository.getCount()
            val nextJobNum = count + 1
            val jobCode = String.format(Locale.US, "JOB-%02d", nextJobNum)

            val label = if (state.jobLabel.isNotBlank()) {
                state.jobLabel.trim()
            } else if (state.contentMode == ContentMode.TYPE && state.typedMessage.isNotBlank()) {
                state.typedMessage.trim().take(30)
            } else {
                "Voice Reminder $jobCode"
            }

            var finalAudioPath: String? = null

            if (state.contentMode == ContentMode.RECORD && state.recordingFile != null) {
                val permanentDir = getApplication<Application>().filesDir
                val destFile = File(permanentDir, "${jobCode}_${System.currentTimeMillis()}.m4a")
                state.recordingFile.copyTo(destFile, overwrite = true)
                finalAudioPath = destFile.absolutePath
            } else if (state.contentMode == ContentMode.TYPE && state.typedMessage.isNotBlank()) {
                if (voiceCoordinator.isVoiceCloningEnabled) {
                    val clonedFile = voiceCoordinator.synthesizeVoiceForTask(state.typedMessage.trim(), jobCode)
                    if (clonedFile != null && clonedFile.exists()) {
                        finalAudioPath = clonedFile.absolutePath
                    }
                }
            }

            val entity = ReminderEntity(
                jobCode = jobCode,
                label = label,
                contentMode = state.contentMode.name,
                textMessage = state.typedMessage.trim(),
                audioFilePath = finalAudioPath,
                dueTimestamp = state.customDueEpochMs
            )

            val insertedId = repository.insert(entity).toInt()
            val scheduledEntity = entity.copy(id = insertedId)

            ReminderScheduler.schedule(getApplication(), scheduledEntity)

            viewModelScope.launch(Dispatchers.Main) {
                discardRecording()
                calculateInitialDueTime()
                _uiState.value = _uiState.value.copy(
                    jobLabel = "",
                    typedMessage = "",
                    recordingFile = null,
                    aiStatusMessage = null
                )
            }
        }
    }

    fun playReminderCard(reminder: ReminderEntity) {
        if (_uiState.value.playingReminderId == reminder.id) {
            playerManager.stop()
            ttsManager.stop()
            _uiState.value = _uiState.value.copy(playingReminderId = null)
            return
        }

        playerManager.stop()
        ttsManager.stop()

        _uiState.value = _uiState.value.copy(playingReminderId = reminder.id)

        if (!reminder.audioFilePath.isNullOrEmpty()) {
            playerManager.playAudio(reminder.audioFilePath) {
                _uiState.value = _uiState.value.copy(playingReminderId = null)
            }
        } else if (reminder.textMessage.isNotBlank()) {
            ttsManager.speak(reminder.textMessage) {
                _uiState.value = _uiState.value.copy(playingReminderId = null)
            }
        } else {
            _uiState.value = _uiState.value.copy(playingReminderId = null)
        }
    }

    fun toggleCompleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = reminder.copy(isCompleted = !reminder.isCompleted)
            repository.update(updated)

            if (updated.isCompleted) {
                ReminderScheduler.cancel(getApplication(), updated.id)
            } else if (updated.dueTimestamp > System.currentTimeMillis()) {
                ReminderScheduler.schedule(getApplication(), updated)
            }

            if (_uiState.value.playingReminderId == reminder.id) {
                playerManager.stop()
                ttsManager.stop()
                _uiState.value = _uiState.value.copy(playingReminderId = null)
            }
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            ReminderScheduler.cancel(getApplication(), reminder.id)
            repository.deleteById(reminder.id)
            if (!reminder.audioFilePath.isNullOrEmpty()) {
                try { File(reminder.audioFilePath).delete() } catch (_: Exception) {}
            }
        }
    }

    fun setLogFilter(filter: LogFilter) {
        _uiState.value = _uiState.value.copy(logFilter = filter)
    }

    fun snoozeFiringReminder(minutes: Int) {
        val firing = _uiState.value.firingReminder ?: return
        playerManager.stop()
        ttsManager.stop()
        stopBackgroundAlarmService()

        val newDue = System.currentTimeMillis() + (minutes * 60 * 1000L)
        val updated = firing.copy(
            dueTimestamp = newDue,
            isCompleted = false
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.update(updated)
            ReminderScheduler.schedule(getApplication(), updated)
            _uiState.value = _uiState.value.copy(firingReminder = null)
        }
    }

    fun dismissFiringModalAndComplete() {
        val firing = _uiState.value.firingReminder ?: return
        playerManager.stop()
        ttsManager.stop()
        stopBackgroundAlarmService()

        val updated = firing.copy(isCompleted = true)
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(updated)
            ReminderScheduler.cancel(getApplication(), updated.id)
            _uiState.value = _uiState.value.copy(firingReminder = null)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            allReminders.value.forEach { reminder ->
                ReminderScheduler.cancel(getApplication(), reminder.id)
            }
            repository.clearAll()
        }
    }

    fun clearUserErrorMessage() {
        _uiState.value = _uiState.value.copy(userErrorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        recorderManager.cancelRecording()
        playerManager.stop()
        ttsManager.shutdown()
    }
}

fun formatPlainLanguageDue(dueEpochMs: Long): String {
    val nowCal = Calendar.getInstance()
    val dueCal = Calendar.getInstance().apply { timeInMillis = dueEpochMs }

    val isToday = nowCal.get(Calendar.YEAR) == dueCal.get(Calendar.YEAR) &&
            nowCal.get(Calendar.DAY_OF_YEAR) == dueCal.get(Calendar.DAY_OF_YEAR)

    val tomCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val isTomorrow = tomCal.get(Calendar.YEAR) == dueCal.get(Calendar.YEAR) &&
            tomCal.get(Calendar.DAY_OF_YEAR) == dueCal.get(Calendar.DAY_OF_YEAR)

    val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(dueEpochMs))

    return when {
        isToday -> "Due: Today, $timeStr"
        isTomorrow -> "Due: Tomorrow, $timeStr"
        else -> {
            val dateStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(dueEpochMs))
            "Due: $dateStr"
        }
    }
}

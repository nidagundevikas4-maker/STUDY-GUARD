package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.FocusPreferencesRepository
import com.example.data.timer.FocusTimerRepository
import com.example.model.FocusMode
import com.example.model.FocusTimerState
import com.example.services.FocusTimerService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FocusTimerUiState(
  val timerState: FocusTimerState = FocusTimerState(),
  val selectedMode: FocusMode = FocusMode.STUDY_MODE,
  val configuredDurationMinutes: Int = 25,
  val isStrictLockConfiguredForMode: Boolean = true,
  val isCustomDurationDialogOpen: Boolean = false,
  val showNotificationDeniedBanner: Boolean = false,
  val isEmergencyUnlockDialogOpen: Boolean = false,
  val unlockCountdownSeconds: Int = 10,
  val unlockPhraseInput: String = "",
) {
  val canConfirmEmergencyUnlock: Boolean
    get() =
      unlockCountdownSeconds <= 0 &&
        unlockPhraseInput.trim().equals(EMERGENCY_UNLOCK_PHRASE, ignoreCase = true)

  companion object {
    const val EMERGENCY_UNLOCK_PHRASE = "UNLOCK"
  }
}

class FocusTimerViewModel(
  private val appContext: Context,
  private val preferencesRepository: FocusPreferencesRepository,
  private val timerRepository: FocusTimerRepository,
  private val unlockCooldownSeconds: Int = 10,
) : ViewModel() {

  private val isCustomDialogOpen = MutableStateFlow(false)
  private val notificationDeniedBanner = MutableStateFlow(false)
  private val emergencyDialogOpen = MutableStateFlow(false)
  private val emergencyCountdown = MutableStateFlow(unlockCooldownSeconds)
  private val emergencyPhrase = MutableStateFlow("")

  private var emergencyCountdownJob: Job? = null

  val uiState: StateFlow<FocusTimerUiState> =
    combine(
      combine(
        timerRepository.timerState,
        preferencesRepository.preferencesFlow,
        isCustomDialogOpen,
        notificationDeniedBanner,
      ) { timer, prefs, dialogOpen, deniedBanner ->
        FocusTimerUiState(
          timerState = timer,
          selectedMode = prefs.activeFocusMode,
          configuredDurationMinutes = prefs.selectedDurationMinutes,
          isStrictLockConfiguredForMode = prefs.configFor(prefs.activeFocusMode).strictSessionLock,
          isCustomDurationDialogOpen = dialogOpen,
          showNotificationDeniedBanner = deniedBanner,
        )
      },
      emergencyDialogOpen,
      emergencyCountdown,
      emergencyPhrase,
    ) { base, unlockOpen, countdown, phrase ->
      base.copy(
        isEmergencyUnlockDialogOpen = unlockOpen,
        unlockCountdownSeconds = countdown,
        unlockPhraseInput = phrase,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = FocusTimerUiState(unlockCountdownSeconds = unlockCooldownSeconds),
    )

  init {
    viewModelScope.launch {
      preferencesRepository.preferencesFlow.collect { prefs ->
        timerRepository.syncIdleConfig(
          mode = prefs.activeFocusMode,
          durationMinutes = prefs.selectedDurationMinutes,
        )
      }
    }
  }

  fun selectDurationMinutes(minutes: Int) {
    if (timerRepository.timerState.value.isActive) return
    viewModelScope.launch {
      preferencesRepository.setSelectedDurationMinutes(minutes)
      isCustomDialogOpen.update { false }
    }
  }

  fun openCustomDurationDialog() {
    if (timerRepository.timerState.value.isActive) return
    isCustomDialogOpen.update { true }
  }

  fun dismissCustomDurationDialog() {
    isCustomDialogOpen.update { false }
  }

  fun onNotificationPermissionResult(granted: Boolean) {
    notificationDeniedBanner.update { !granted }
  }

  fun startFocusSession() {
    if (timerRepository.timerState.value.isActive) return
    val current = uiState.value
    FocusTimerService.startSession(
      context = appContext,
      mode = current.selectedMode,
      durationMinutes = current.configuredDurationMinutes,
      strictLock = current.isStrictLockConfiguredForMode,
    )
  }

  fun pauseFocusSession() {
    if (timerRepository.timerState.value.isStrictLocked) return
    FocusTimerService.pauseSession(appContext)
  }

  fun resumeFocusSession() {
    FocusTimerService.resumeSession(appContext)
  }

  fun resetFocusSession() {
    if (timerRepository.timerState.value.isStrictLocked) return
    FocusTimerService.resetSession(appContext)
  }

  fun openEmergencyUnlockDialog() {
    emergencyCountdownJob?.cancel()
    emergencyPhrase.value = ""
    emergencyCountdown.value = unlockCooldownSeconds.coerceAtLeast(0)
    emergencyDialogOpen.value = true

    if (unlockCooldownSeconds > 0) {
      emergencyCountdownJob =
        viewModelScope.launch {
          for (sec in (unlockCooldownSeconds - 1) downTo 0) {
            delay(1_000L)
            emergencyCountdown.value = sec
          }
        }
    }
  }

  fun updateEmergencyUnlockPhrase(input: String) {
    emergencyPhrase.value = input
  }

  fun dismissEmergencyUnlockDialog() {
    emergencyCountdownJob?.cancel()
    emergencyCountdownJob = null
    emergencyDialogOpen.value = false
    emergencyPhrase.value = ""
  }

  fun confirmEmergencyUnlock() {
    if (!uiState.value.canConfirmEmergencyUnlock) return
    dismissEmergencyUnlockDialog()
    FocusTimerService.emergencyUnlockSession(appContext)
  }

  fun dismissCompletionState() {
    val current = uiState.value
    timerRepository.acknowledgeCompletion(
      mode = current.selectedMode,
      durationMinutes = current.configuredDurationMinutes,
    )
  }

  class Factory(
    private val appContext: Context,
    private val preferencesRepository: FocusPreferencesRepository,
    private val timerRepository: FocusTimerRepository,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      require(modelClass.isAssignableFrom(FocusTimerViewModel::class.java)) {
        "Unknown ViewModel class: ${modelClass.name}"
      }
      return FocusTimerViewModel(appContext, preferencesRepository, timerRepository) as T
    }
  }
}

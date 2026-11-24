package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.FocusPreferencesRepository
import com.example.data.timer.FocusTimerRepository
import com.example.model.EmergencyUnlockMethod
import com.example.model.FocusMode
import com.example.model.FocusTimerState
import com.example.security.PinHashUtil
import com.example.services.FocusTimerService
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MathChallenge(
  val prompt: String,
  val answer: Int,
) {
  companion object {
    fun generateRandom(): MathChallenge {
      val a = Random.nextInt(12, 24)
      val b = Random.nextInt(4, 10)
      val c = Random.nextInt(11, 35)
      return MathChallenge(
        prompt = "($a × $b) + $c",
        answer = (a * b) + c,
      )
    }
  }
}

private data class EmergencyUnlockDraft(
  val isOpen: Boolean = false,
  val countdownSeconds: Int = 10,
  val phraseInput: String = "",
  val mathChallenge: MathChallenge = MathChallenge(prompt = "(14 × 6) + 19", answer = 103),
  val mathInput: String = "",
  val pinInput: String = "",
)

data class FocusTimerUiState(
  val timerState: FocusTimerState = FocusTimerState(),
  val selectedMode: FocusMode = FocusMode.STUDY_MODE,
  val configuredDurationMinutes: Int = 25,
  val isStrictLockConfiguredForMode: Boolean = true,
  val configuredUnlockMethod: EmergencyUnlockMethod = EmergencyUnlockMethod.COOLDOWN_PHRASE,
  val hasAccountabilityPin: Boolean = false,
  val isCustomDurationDialogOpen: Boolean = false,
  val showNotificationDeniedBanner: Boolean = false,
  val isEmergencyUnlockDialogOpen: Boolean = false,
  val unlockCountdownSeconds: Int = 10,
  val unlockPhraseInput: String = "",
  val mathChallenge: MathChallenge = MathChallenge(prompt = "(14 × 6) + 19", answer = 103),
  val unlockMathInput: String = "",
  val unlockPinInput: String = "",
  val isPinMatched: Boolean = false,
) {
  val effectiveUnlockMethod: EmergencyUnlockMethod
    get() =
      if (configuredUnlockMethod == EmergencyUnlockMethod.ACCOUNTABILITY_PIN && !hasAccountabilityPin) {
        EmergencyUnlockMethod.COOLDOWN_PHRASE
      } else {
        configuredUnlockMethod
      }

  val isPinIncorrectAfterFourDigits: Boolean
    get() =
      effectiveUnlockMethod == EmergencyUnlockMethod.ACCOUNTABILITY_PIN &&
        unlockPinInput.length == 4 &&
        !isPinMatched

  val canConfirmEmergencyUnlock: Boolean
    get() =
      when (effectiveUnlockMethod) {
        EmergencyUnlockMethod.COOLDOWN_PHRASE ->
          unlockCountdownSeconds <= 0 &&
            unlockPhraseInput.trim().equals(EMERGENCY_UNLOCK_PHRASE, ignoreCase = true)
        EmergencyUnlockMethod.MATH_CHALLENGE ->
          unlockMathInput.trim().toIntOrNull() == mathChallenge.answer &&
            unlockPhraseInput.trim().equals(EMERGENCY_UNLOCK_PHRASE, ignoreCase = true)
        EmergencyUnlockMethod.ACCOUNTABILITY_PIN -> isPinMatched
      }

  companion object {
    const val EMERGENCY_UNLOCK_PHRASE = "UNLOCK"
  }
}

class FocusTimerViewModel(
  private val appContext: Context,
  private val preferencesRepository: FocusPreferencesRepository,
  private val timerRepository: FocusTimerRepository,
  private val unlockCooldownSeconds: Int = 10,
  private val mathChallengeGenerator: () -> MathChallenge = { MathChallenge.generateRandom() },
) : ViewModel() {

  private val isCustomDialogOpen = MutableStateFlow(false)
  private val notificationDeniedBanner = MutableStateFlow(false)
  private val unlockDraft =
    MutableStateFlow(EmergencyUnlockDraft(countdownSeconds = unlockCooldownSeconds))

  private var emergencyCountdownJob: Job? = null

  val uiState: StateFlow<FocusTimerUiState> =
    combine(
      timerRepository.timerState,
      preferencesRepository.preferencesFlow,
      isCustomDialogOpen,
      notificationDeniedBanner,
      unlockDraft,
    ) { timer, prefs, dialogOpen, deniedBanner, draft ->
      val pinMatched =
        if (prefs.hasAccountabilityPin) {
          PinHashUtil.verifyPin(draft.pinInput, prefs.accountabilityPinHash)
        } else {
          false
        }

      FocusTimerUiState(
        timerState = timer,
        selectedMode = prefs.activeFocusMode,
        configuredDurationMinutes = prefs.selectedDurationMinutes,
        isStrictLockConfiguredForMode = prefs.configFor(prefs.activeFocusMode).strictSessionLock,
        configuredUnlockMethod = prefs.emergencyUnlockMethod,
        hasAccountabilityPin = prefs.hasAccountabilityPin,
        isCustomDurationDialogOpen = dialogOpen,
        showNotificationDeniedBanner = deniedBanner,
        isEmergencyUnlockDialogOpen = draft.isOpen,
        unlockCountdownSeconds = draft.countdownSeconds,
        unlockPhraseInput = draft.phraseInput,
        mathChallenge = draft.mathChallenge,
        unlockMathInput = draft.mathInput,
        unlockPinInput = draft.pinInput,
        isPinMatched = pinMatched,
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
    val startSeconds = unlockCooldownSeconds.coerceAtLeast(0)
    unlockDraft.value =
      EmergencyUnlockDraft(
        isOpen = true,
        countdownSeconds = startSeconds,
        phraseInput = "",
        mathChallenge = mathChallengeGenerator(),
        mathInput = "",
        pinInput = "",
      )

    if (uiState.value.effectiveUnlockMethod == EmergencyUnlockMethod.COOLDOWN_PHRASE && startSeconds > 0) {
      emergencyCountdownJob =
        viewModelScope.launch {
          for (sec in (startSeconds - 1) downTo 0) {
            delay(1_000L)
            unlockDraft.update { it.copy(countdownSeconds = sec) }
          }
        }
    }
  }

  fun updateEmergencyUnlockPhrase(input: String) {
    unlockDraft.update { it.copy(phraseInput = input) }
  }

  fun updateEmergencyMathInput(input: String) {
    val sanitized = input.filter { it.isDigit() }.take(6)
    unlockDraft.update { it.copy(mathInput = sanitized) }
  }

  fun updateEmergencyPinInput(input: String) {
    val sanitized = input.filter { it.isDigit() }.take(4)
    unlockDraft.update { it.copy(pinInput = sanitized) }
  }

  fun dismissEmergencyUnlockDialog() {
    emergencyCountdownJob?.cancel()
    emergencyCountdownJob = null
    unlockDraft.update {
      it.copy(
        isOpen = false,
        phraseInput = "",
        mathInput = "",
        pinInput = "",
      )
    }
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

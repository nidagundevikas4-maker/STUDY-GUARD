package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.FocusPreferencesRepository
import com.example.data.rules.NotificationRuleRepository
import com.example.data.session.SessionRepository
import com.example.data.timer.FocusTimerRepository
import com.example.model.FocusMode
import com.example.model.ThemePreference
import com.example.model.UserPreferences
import com.example.permissions.SystemPermissionReader
import com.example.permissions.SystemPermissionSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
  val preferences: UserPreferences = UserPreferences(),
  val permissionSnapshot: SystemPermissionSnapshot = SystemPermissionSnapshot(),
  val privacyActionMessage: String? = null,
  val isSessionActive: Boolean = false,
  val isSessionLocked: Boolean = false,
  val activeRemainingSeconds: Int = 0,
)

class SettingsViewModel(
  private val preferencesRepository: FocusPreferencesRepository,
  private val permissionReader: SystemPermissionReader,
  private val sessionRepository: SessionRepository,
  private val notificationRuleRepository: NotificationRuleRepository,
  private val timerRepository: FocusTimerRepository,
) : ViewModel() {

  private val _uiState =
    MutableStateFlow(
      SettingsUiState(permissionSnapshot = permissionReader.readSnapshot())
    )
  val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      combine(
        preferencesRepository.preferencesFlow,
        timerRepository.timerState,
      ) { prefs, timer ->
        prefs to timer
      }.collect { (prefs, timer) ->
        _uiState.update { current ->
          current.copy(
            preferences = prefs,
            isSessionActive = timer.isActive,
            isSessionLocked = timer.isActive && timer.isStrictLocked,
            activeRemainingSeconds = timer.remainingSeconds,
          )
        }
      }
    }
  }

  fun refreshPermissions() {
    _uiState.update { current ->
      current.copy(permissionSnapshot = permissionReader.readSnapshot())
    }
  }

  fun setThemePreference(theme: ThemePreference) {
    viewModelScope.launch { preferencesRepository.setThemePreference(theme) }
  }

  fun setDefaultFocusMode(mode: FocusMode) {
    if (timerRepository.timerState.value.isActive) return
    viewModelScope.launch { preferencesRepository.setActiveFocusMode(mode) }
  }

  fun setDefaultDurationMinutes(minutes: Int) {
    if (timerRepository.timerState.value.isActive) return
    viewModelScope.launch { preferencesRepository.setSelectedDurationMinutes(minutes) }
  }

  fun setDailyGoalMinutes(minutes: Int) {
    viewModelScope.launch { preferencesRepository.setDailyGoalMinutes(minutes) }
  }

  fun setModeAutoDnd(mode: FocusMode, enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.updateModeAutoDnd(mode, enabled) }
  }

  fun setDeepFocusAlarmsOnlyDnd(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.updateDeepFocusAlarmsOnlyDnd(enabled) }
  }

  fun setModeAcademicPriority(mode: FocusMode, enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.updateModeAcademicPriority(mode, enabled) }
  }

  fun setModeBlockApps(mode: FocusMode, enabled: Boolean) {
    val timer = timerRepository.timerState.value
    if (timer.isActive && timer.isStrictLocked) return
    viewModelScope.launch { preferencesRepository.updateModeBlockApps(mode, enabled) }
  }

  fun setModeStrictLock(mode: FocusMode, enabled: Boolean) {
    val timer = timerRepository.timerState.value
    if (timer.isActive && timer.isStrictLocked) return
    viewModelScope.launch { preferencesRepository.updateModeStrictLock(mode, enabled) }
  }

  fun resetAllNotificationRules() {
    val timer = timerRepository.timerState.value
    if (timer.isActive && timer.isStrictLocked) return
    viewModelScope.launch {
      notificationRuleRepository.resetAllRules()
      _uiState.update {
        it.copy(privacyActionMessage = "Custom app notification rules reset to defaults.")
      }
    }
  }

  fun clearAllStudyHistory() {
    viewModelScope.launch {
      sessionRepository.clearAllHistory()
      _uiState.update {
        it.copy(privacyActionMessage = "All saved study sessions and distraction logs cleared.")
      }
    }
  }

  fun dismissPrivacyMessage() {
    _uiState.update { it.copy(privacyActionMessage = null) }
  }

  class Factory(
    private val preferencesRepository: FocusPreferencesRepository,
    private val permissionReader: SystemPermissionReader,
    private val sessionRepository: SessionRepository,
    private val notificationRuleRepository: NotificationRuleRepository,
    private val timerRepository: FocusTimerRepository,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      require(modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
        "Unknown ViewModel class: ${modelClass.name}"
      }
      return SettingsViewModel(
        preferencesRepository = preferencesRepository,
        permissionReader = permissionReader,
        sessionRepository = sessionRepository,
        notificationRuleRepository = notificationRuleRepository,
        timerRepository = timerRepository,
      ) as T
    }
  }
}

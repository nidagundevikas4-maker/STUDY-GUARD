package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.analytics.MilestoneCalculator
import com.example.data.preferences.FocusPreferencesRepository
import com.example.data.session.SessionRepository
import com.example.data.timer.FocusTimerRepository
import com.example.model.FocusMode
import com.example.model.SessionOutcome
import com.example.model.ThemePreference
import com.example.permissions.SystemPermissionReader
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
  private val preferencesRepository: FocusPreferencesRepository,
  private val permissionReader: SystemPermissionReader,
  private val timerRepository: FocusTimerRepository,
  private val sessionRepository: SessionRepository,
  private val onWidgetDataChanged: () -> Unit = {},
) : ViewModel() {

  private val _uiState =
    MutableStateFlow(
      HomeUiState(
        greetingHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        permissionSnapshot = permissionReader.readSnapshot(),
      )
    )
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      combine(
        preferencesRepository.preferencesFlow,
        timerRepository.timerState,
        sessionRepository.observeTodaySessions(),
        sessionRepository.observeAllSessions(),
      ) { prefs, timer, todaySessions, allSessions ->
        HomeCombinedData(prefs, timer, todaySessions, allSessions)
      }.collect { (prefs, timer, todaySessions, allSessions) ->
        val totalMinutesToday = todaySessions.sumOf { it.elapsedSeconds / 60 }
        val completedCount = todaySessions.count { it.outcome == SessionOutcome.COMPLETED }
        val totalAllTimeMinutes = allSessions.sumOf { it.elapsedSeconds / 60 }

        _uiState.update { current ->
          current.copy(
            activeFocusMode = prefs.activeFocusMode,
            dailyGoalMinutes = prefs.dailyGoalMinutes,
            selectedDurationMinutes = prefs.selectedDurationMinutes,
            themePreference = prefs.themePreference,
            timerState = timer,
            todaySessions = todaySessions,
            todayStudyMinutes = totalMinutesToday,
            completedSessionsToday = completedCount,
            studyLevel = MilestoneCalculator.resolveLevel(totalAllTimeMinutes),
          )
        }
      }
    }
  }

  private data class HomeCombinedData(
    val prefs: com.example.model.UserPreferences,
    val timer: com.example.model.FocusTimerState,
    val todaySessions: List<com.example.model.FocusSessionSummary>,
    val allSessions: List<com.example.model.FocusSessionSummary>,
  )

  fun refreshSystemPermissions() {
    _uiState.update { current ->
      current.copy(permissionSnapshot = permissionReader.readSnapshot())
    }
  }

  fun selectFocusMode(mode: FocusMode) {
    if (timerRepository.timerState.value.isActive) return
    viewModelScope.launch {
      preferencesRepository.setActiveFocusMode(mode)
      onWidgetDataChanged()
    }
  }

  fun selectDurationMinutes(minutes: Int) {
    if (timerRepository.timerState.value.isActive) return
    viewModelScope.launch {
      preferencesRepository.setSelectedDurationMinutes(minutes)
      onWidgetDataChanged()
    }
  }

  fun updateDailyGoalMinutes(minutes: Int) {
    viewModelScope.launch {
      preferencesRepository.setDailyGoalMinutes(minutes)
      _uiState.update { current -> current.copy(isGoalSheetOpen = false) }
      onWidgetDataChanged()
    }
  }

  fun cycleThemePreference(currentlyDark: Boolean) {
    val nextTheme = if (currentlyDark) ThemePreference.LIGHT else ThemePreference.DARK
    viewModelScope.launch { preferencesRepository.setThemePreference(nextTheme) }
  }

  fun openGoalEditor() {
    _uiState.update { current -> current.copy(isGoalSheetOpen = true) }
  }

  fun dismissGoalEditor() {
    _uiState.update { current -> current.copy(isGoalSheetOpen = false) }
  }

  class Factory(
    private val preferencesRepository: FocusPreferencesRepository,
    private val permissionReader: SystemPermissionReader,
    private val timerRepository: FocusTimerRepository,
    private val sessionRepository: SessionRepository,
    private val onWidgetDataChanged: () -> Unit = {},
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      require(modelClass.isAssignableFrom(HomeViewModel::class.java)) {
        "Unknown ViewModel class: ${modelClass.name}"
      }
      return HomeViewModel(
        preferencesRepository = preferencesRepository,
        permissionReader = permissionReader,
        timerRepository = timerRepository,
        sessionRepository = sessionRepository,
        onWidgetDataChanged = onWidgetDataChanged,
      ) as T
    }
  }
}

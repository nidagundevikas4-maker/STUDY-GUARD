package com.example.ui.viewmodel

import com.example.model.FocusMode
import com.example.model.FocusSessionSummary
import com.example.model.FocusTimerState
import com.example.model.ThemePreference
import com.example.permissions.SystemPermissionSnapshot

data class HomeUiState(
  val greetingHour: Int = 9,
  val todayStudyMinutes: Int = 0,
  val dailyGoalMinutes: Int = 120,
  val completedSessionsToday: Int = 0,
  val todaySessions: List<FocusSessionSummary> = emptyList(),
  val activeFocusMode: FocusMode = FocusMode.STUDY_MODE,
  val selectedDurationMinutes: Int = 25,
  val timerState: FocusTimerState = FocusTimerState(),
  val themePreference: ThemePreference = ThemePreference.SYSTEM,
  val permissionSnapshot: SystemPermissionSnapshot = SystemPermissionSnapshot(),
  val isGoalSheetOpen: Boolean = false,
) {
  val dailyProgressFraction: Float
    get() =
      if (dailyGoalMinutes <= 0) 0f
      else (todayStudyMinutes.toFloat() / dailyGoalMinutes.toFloat()).coerceIn(0f, 1f)
}

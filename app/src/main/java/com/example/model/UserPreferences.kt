package com.example.model

enum class ThemePreference(val label: String) {
  SYSTEM("System"),
  LIGHT("Light"),
  DARK("Dark"),
}

data class FocusModeConfig(
  val autoEnableDnd: Boolean,
  val strictAlarmsOnlyDnd: Boolean,
  val prioritizeAcademicNotifications: Boolean,
  val blockSilencedApps: Boolean,
  val strictSessionLock: Boolean = true,
)

data class PersistedActiveSession(
  val mode: FocusMode,
  val plannedMinutes: Int,
  val isStrictLocked: Boolean,
  val startedAtEpochMs: Long,
  val endEpochMs: Long,
  val endElapsedRealtimeMs: Long,
  val bootTimeEstimateMs: Long,
)

data class UserPreferences(
  val activeFocusMode: FocusMode = FocusMode.STUDY_MODE,
  val dailyGoalMinutes: Int = 120,
  val selectedDurationMinutes: Int = 25,
  val themePreference: ThemePreference = ThemePreference.SYSTEM,
  val deepFocusConfig: FocusModeConfig =
    FocusModeConfig(
      autoEnableDnd = true,
      strictAlarmsOnlyDnd = false,
      prioritizeAcademicNotifications = true,
      blockSilencedApps = true,
      strictSessionLock = true,
    ),
  val studyModeConfig: FocusModeConfig =
    FocusModeConfig(
      autoEnableDnd = true,
      strictAlarmsOnlyDnd = false,
      prioritizeAcademicNotifications = true,
      blockSilencedApps = true,
      strictSessionLock = true,
    ),
  val lightFocusConfig: FocusModeConfig =
    FocusModeConfig(
      autoEnableDnd = false,
      strictAlarmsOnlyDnd = false,
      prioritizeAcademicNotifications = true,
      blockSilencedApps = false,
      strictSessionLock = false,
    ),
) {
  fun configFor(mode: FocusMode): FocusModeConfig =
    when (mode) {
      FocusMode.DEEP_FOCUS -> deepFocusConfig
      FocusMode.STUDY_MODE -> studyModeConfig
      FocusMode.LIGHT_FOCUS -> lightFocusConfig
    }
}

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

enum class EmergencyUnlockMethod(
  val label: String,
  val subtitle: String,
) {
  COOLDOWN_PHRASE(
    label = "10s Cooldown",
    subtitle = "Wait 10 seconds and type UNLOCK",
  ),
  MATH_CHALLENGE(
    label = "Math Challenge",
    subtitle = "Solve a mental math puzzle and type UNLOCK",
  ),
  ACCOUNTABILITY_PIN(
    label = "Partner PIN",
    subtitle = "Require a 4-digit PIN held by a parent, roommate, or friend",
  ),
}

data class UserPreferences(
  val activeFocusMode: FocusMode = FocusMode.STUDY_MODE,
  val dailyGoalMinutes: Int = 120,
  val selectedDurationMinutes: Int = 25,
  val themePreference: ThemePreference = ThemePreference.SYSTEM,
  val emergencyUnlockMethod: EmergencyUnlockMethod = EmergencyUnlockMethod.COOLDOWN_PHRASE,
  val accountabilityPinHash: String = "",
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
  val hasAccountabilityPin: Boolean
    get() = accountabilityPinHash.isNotBlank()

  fun configFor(mode: FocusMode): FocusModeConfig =
    when (mode) {
      FocusMode.DEEP_FOCUS -> deepFocusConfig
      FocusMode.STUDY_MODE -> studyModeConfig
      FocusMode.LIGHT_FOCUS -> lightFocusConfig
    }
}

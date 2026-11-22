package com.example.model

enum class TimerPhase {
  IDLE,
  RUNNING,
  PAUSED,
  COMPLETED,
}

enum class SessionOutcome {
  COMPLETED,
  INTERRUPTED,
}

data class FocusTimerState(
  val phase: TimerPhase = TimerPhase.IDLE,
  val mode: FocusMode = FocusMode.STUDY_MODE,
  val plannedDurationMinutes: Int = 25,
  val totalSeconds: Int = 25 * 60,
  val remainingSeconds: Int = 25 * 60,
  val dismissedDuringSession: Int = 0,
  val isStrictLocked: Boolean = false,
) {
  val progressFraction: Float
    get() =
      if (totalSeconds <= 0) 0f
      else ((totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)

  val isActive: Boolean
    get() = phase == TimerPhase.RUNNING || phase == TimerPhase.PAUSED
}

data class FocusSessionSummary(
  val id: Long,
  val mode: FocusMode,
  val plannedMinutes: Int,
  val elapsedSeconds: Int,
  val outcome: SessionOutcome,
  val completedAtEpochMs: Long,
  val dismissedNotificationsCount: Int = 0,
)

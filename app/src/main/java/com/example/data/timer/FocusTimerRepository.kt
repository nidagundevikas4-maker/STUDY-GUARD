package com.example.data.timer

import com.example.data.session.SessionRepository
import com.example.model.FocusMode
import com.example.model.FocusTimerState
import com.example.model.SessionOutcome
import com.example.model.TimerPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FocusTimerRepository(
  private val sessionRepository: SessionRepository,
  private val onSessionPersisted: () -> Unit = {},
) {
  private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val _timerState = MutableStateFlow(FocusTimerState())
  val timerState: StateFlow<FocusTimerState> = _timerState.asStateFlow()

  private var activeStartedAtEpochMs: Long = 0L

  fun syncIdleConfig(mode: FocusMode, durationMinutes: Int) {
    _timerState.update { current ->
      if (current.phase == TimerPhase.IDLE) {
        val totalSecs = durationMinutes.coerceIn(1, 180) * 60
        current.copy(
          mode = mode,
          plannedDurationMinutes = durationMinutes,
          totalSeconds = totalSecs,
          remainingSeconds = totalSecs,
          dismissedDuringSession = 0,
          isStrictLocked = false,
        )
      } else {
        current
      }
    }
  }

  fun onTimerStarted(
    mode: FocusMode,
    durationMinutes: Int,
    isStrictLocked: Boolean = false,
  ) {
    val totalSecs = durationMinutes.coerceIn(1, 180) * 60
    activeStartedAtEpochMs = System.currentTimeMillis()
    _timerState.value =
      FocusTimerState(
        phase = TimerPhase.RUNNING,
        mode = mode,
        plannedDurationMinutes = durationMinutes,
        totalSeconds = totalSecs,
        remainingSeconds = totalSecs,
        dismissedDuringSession = 0,
        isStrictLocked = isStrictLocked,
      )
  }

  fun restoreRunningSession(
    mode: FocusMode,
    durationMinutes: Int,
    remainingSeconds: Int,
    isStrictLocked: Boolean,
    startedAtEpochMs: Long,
  ) {
    val totalSecs = durationMinutes.coerceIn(1, 180) * 60
    activeStartedAtEpochMs = startedAtEpochMs
    _timerState.value =
      FocusTimerState(
        phase = TimerPhase.RUNNING,
        mode = mode,
        plannedDurationMinutes = durationMinutes,
        totalSeconds = totalSecs,
        remainingSeconds = remainingSeconds.coerceIn(0, totalSecs),
        dismissedDuringSession = 0,
        isStrictLocked = isStrictLocked,
      )
  }

  fun onTimerTick(remainingSeconds: Int) {
    _timerState.update { current ->
      if (current.phase == TimerPhase.RUNNING) {
        current.copy(remainingSeconds = remainingSeconds.coerceAtLeast(0))
      } else {
        current
      }
    }
  }

  fun onTimerPaused(remainingSeconds: Int) {
    _timerState.update { current ->
      if (current.isStrictLocked) {
        current
      } else {
        current.copy(
          phase = TimerPhase.PAUSED,
          remainingSeconds = remainingSeconds.coerceAtLeast(0),
        )
      }
    }
  }

  fun onTimerResumed() {
    _timerState.update { current ->
      if (current.phase == TimerPhase.PAUSED) {
        current.copy(phase = TimerPhase.RUNNING)
      } else {
        current
      }
    }
  }

  fun incrementDismissedNotificationCount() {
    _timerState.update { current ->
      if (current.isActive) {
        current.copy(dismissedDuringSession = current.dismissedDuringSession + 1)
      } else {
        current
      }
    }
  }

  fun onTimerCompleted() {
    val snapshot = _timerState.value
    val endedAt = System.currentTimeMillis()
    val startedAt =
      if (activeStartedAtEpochMs > 0L) activeStartedAtEpochMs
      else endedAt - snapshot.totalSeconds * 1000L

    ioScope.launch {
      sessionRepository.recordSession(
        mode = snapshot.mode,
        plannedDurationMinutes = snapshot.plannedDurationMinutes,
        actualDurationSeconds = snapshot.totalSeconds,
        startedAtEpochMs = startedAt,
        endedAtEpochMs = endedAt,
        outcome = SessionOutcome.COMPLETED,
        dismissedNotificationsCount = snapshot.dismissedDuringSession,
      )
      onSessionPersisted()
    }

    activeStartedAtEpochMs = 0L
    _timerState.update { current ->
      current.copy(
        phase = TimerPhase.COMPLETED,
        remainingSeconds = 0,
        isStrictLocked = false,
      )
    }
  }

  fun onTimerReset(
    mode: FocusMode,
    durationMinutes: Int,
    elapsedSeconds: Int,
    forceUnlock: Boolean = false,
  ) {
    val snapshot = _timerState.value
    if (snapshot.isStrictLocked && snapshot.isActive && !forceUnlock) {
      return
    }
    if (snapshot.isActive && elapsedSeconds >= 60) {
      val endedAt = System.currentTimeMillis()
      val startedAt =
        if (activeStartedAtEpochMs > 0L) activeStartedAtEpochMs
        else endedAt - elapsedSeconds * 1000L
      ioScope.launch {
        sessionRepository.recordSession(
          mode = snapshot.mode,
          plannedDurationMinutes = snapshot.plannedDurationMinutes,
          actualDurationSeconds = elapsedSeconds,
          startedAtEpochMs = startedAt,
          endedAtEpochMs = endedAt,
          outcome = SessionOutcome.INTERRUPTED,
          dismissedNotificationsCount = snapshot.dismissedDuringSession,
        )
        onSessionPersisted()
      }
    }
    activeStartedAtEpochMs = 0L
    val totalSecs = durationMinutes.coerceIn(1, 180) * 60
    _timerState.value =
      FocusTimerState(
        phase = TimerPhase.IDLE,
        mode = mode,
        plannedDurationMinutes = durationMinutes,
        totalSeconds = totalSecs,
        remainingSeconds = totalSecs,
        dismissedDuringSession = 0,
        isStrictLocked = false,
      )
  }

  fun acknowledgeCompletion(mode: FocusMode, durationMinutes: Int) {
    activeStartedAtEpochMs = 0L
    val totalSecs = durationMinutes.coerceIn(1, 180) * 60
    _timerState.value =
      FocusTimerState(
        phase = TimerPhase.IDLE,
        mode = mode,
        plannedDurationMinutes = durationMinutes,
        totalSeconds = totalSecs,
        remainingSeconds = totalSecs,
        dismissedDuringSession = 0,
        isStrictLocked = false,
      )
  }
}

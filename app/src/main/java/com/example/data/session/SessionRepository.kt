package com.example.data.session

import com.example.database.dao.DistractionEventDao
import com.example.database.dao.FocusSessionDao
import com.example.database.entities.FocusSessionEntity
import com.example.model.FocusMode
import com.example.model.FocusSessionSummary
import com.example.model.SessionOutcome
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SessionRepository(
  private val sessionDao: FocusSessionDao,
  private val distractionEventDao: DistractionEventDao,
) {

  fun observeAllSessions(): Flow<List<FocusSessionSummary>> =
    sessionDao.observeAllSessions().map { list -> list.map { it.toSummary() } }

  fun observeTodaySessions(): Flow<List<FocusSessionSummary>> {
    val startOfDayMs =
      Calendar.getInstance()
        .apply {
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
        .timeInMillis
    return sessionDao.observeSessionsSince(startOfDayMs).map { list -> list.map { it.toSummary() } }
  }

  suspend fun recordSession(
    mode: FocusMode,
    plannedDurationMinutes: Int,
    actualDurationSeconds: Int,
    startedAtEpochMs: Long,
    endedAtEpochMs: Long,
    outcome: SessionOutcome,
    dismissedNotificationsCount: Int = 0,
  ): Long {
    val entity =
      FocusSessionEntity(
        modeName = mode.name,
        plannedDurationMinutes = plannedDurationMinutes,
        actualDurationSeconds = actualDurationSeconds,
        startedAtEpochMs = startedAtEpochMs,
        endedAtEpochMs = endedAtEpochMs,
        outcome = outcome.name,
        dismissedNotificationsCount = dismissedNotificationsCount,
      )
    return sessionDao.insertSession(entity)
  }

  suspend fun deleteSession(sessionId: Long) {
    sessionDao.deleteSessionById(sessionId)
  }

  suspend fun clearAllHistory() {
    sessionDao.clearAllSessions()
    distractionEventDao.clearAllEvents()
  }

  private fun FocusSessionEntity.toSummary(): FocusSessionSummary {
    val mode = FocusMode.entries.firstOrNull { it.name == modeName } ?: FocusMode.STUDY_MODE
    val parsedOutcome =
      SessionOutcome.entries.firstOrNull { it.name == outcome } ?: SessionOutcome.COMPLETED
    return FocusSessionSummary(
      id = id,
      mode = mode,
      plannedMinutes = plannedDurationMinutes,
      elapsedSeconds = actualDurationSeconds,
      outcome = parsedOutcome,
      completedAtEpochMs = endedAtEpochMs,
      dismissedNotificationsCount = dismissedNotificationsCount,
    )
  }
}

package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.rules.NotificationRuleRepository
import com.example.data.session.SessionRepository
import com.example.data.timer.FocusTimerRepository
import com.example.database.FocusFlowDatabase
import com.example.model.AppCategory
import com.example.model.FocusMode
import com.example.model.RuleAction
import com.example.model.SessionOutcome
import com.example.model.TimerPhase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomRepositoriesTest {

  private lateinit var database: FocusFlowDatabase
  private lateinit var sessionRepository: SessionRepository
  private lateinit var ruleRepository: NotificationRuleRepository
  private lateinit var timerRepository: FocusTimerRepository

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database =
      Room.inMemoryDatabaseBuilder(context, FocusFlowDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    sessionRepository =
      SessionRepository(
        sessionDao = database.focusSessionDao(),
        distractionEventDao = database.distractionEventDao(),
      )
    ruleRepository =
      NotificationRuleRepository(
        ruleDao = database.appNotificationRuleDao(),
        distractionEventDao = database.distractionEventDao(),
      )
    timerRepository = FocusTimerRepository(sessionRepository)
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `session repository inserts queries deletes and clears sessions`() = runBlocking {
    val now = System.currentTimeMillis()
    val id1 =
      sessionRepository.recordSession(
        mode = FocusMode.STUDY_MODE,
        plannedDurationMinutes = 25,
        actualDurationSeconds = 1500,
        startedAtEpochMs = now - 1_500_000L,
        endedAtEpochMs = now,
        outcome = SessionOutcome.COMPLETED,
        dismissedNotificationsCount = 3,
      )
    sessionRepository.recordSession(
      mode = FocusMode.DEEP_FOCUS,
      plannedDurationMinutes = 45,
      actualDurationSeconds = 900,
      startedAtEpochMs = now - 900_000L,
      endedAtEpochMs = now - 10_000L,
      outcome = SessionOutcome.INTERRUPTED,
      dismissedNotificationsCount = 1,
    )

    val allBeforeDelete = sessionRepository.observeAllSessions().first()
    assertEquals(2, allBeforeDelete.size)

    sessionRepository.deleteSession(id1)
    val afterDelete = sessionRepository.observeAllSessions().first()
    assertEquals(1, afterDelete.size)
    assertEquals(SessionOutcome.INTERRUPTED, afterDelete.first().outcome)

    sessionRepository.clearAllHistory()
    assertTrue(sessionRepository.observeAllSessions().first().isEmpty())
  }

  @Test
  fun `notification rule override takes precedence and resets cleanly`() = runBlocking {
    val defaultAcademic =
      ruleRepository.resolveEffectiveRule(
        packageName = "com.google.android.apps.classroom",
        appLabel = "Classroom",
        focusMode = FocusMode.STUDY_MODE,
        academicPriorityEnabled = true,
      )
    assertEquals(RuleAction.ALLOW, defaultAcademic.action)
    assertEquals(AppCategory.ACADEMIC, defaultAcademic.category)
    assertFalse(defaultAcademic.isUserCustomized)

    ruleRepository.saveRule(
      packageName = "com.google.android.apps.classroom",
      appLabel = "Classroom",
      focusMode = FocusMode.STUDY_MODE,
      category = AppCategory.ACADEMIC,
      action = RuleAction.SILENCE,
    )

    val overridden =
      ruleRepository.resolveEffectiveRule(
        packageName = "com.google.android.apps.classroom",
        appLabel = "Classroom",
        focusMode = FocusMode.STUDY_MODE,
        academicPriorityEnabled = true,
      )
    assertEquals(RuleAction.SILENCE, overridden.action)
    assertTrue(overridden.isUserCustomized)

    ruleRepository.resetAllRules()
    val resetRule =
      ruleRepository.resolveEffectiveRule(
        packageName = "com.google.android.apps.classroom",
        appLabel = "Classroom",
        focusMode = FocusMode.STUDY_MODE,
        academicPriorityEnabled = true,
      )
    assertEquals(RuleAction.ALLOW, resetRule.action)
    assertFalse(resetRule.isUserCustomized)
  }

  @Test
  fun `notification rule repository logs distraction counts locally`() = runBlocking {
    ruleRepository.logDistractionEvent(
      packageName = "com.instagram.android",
      appLabel = "Instagram",
      focusMode = FocusMode.DEEP_FOCUS,
      outcome = "DISMISSED",
    )

    val logged = ruleRepository.observeRecentDistractions().first()
    assertEquals(1, logged.size)
    assertEquals("Instagram", logged.first().appLabel)
    assertEquals("DISMISSED", logged.first().outcome)
  }

  @Test
  fun `timer repository records completed and long interrupted sessions only`() = runBlocking {
    timerRepository.onTimerStarted(FocusMode.STUDY_MODE, durationMinutes = 25)
    timerRepository.incrementDismissedNotificationCount()
    timerRepository.onTimerPaused(remainingSeconds = 1200)
    assertEquals(TimerPhase.PAUSED, timerRepository.timerState.value.phase)

    timerRepository.onTimerResumed()
    assertEquals(TimerPhase.RUNNING, timerRepository.timerState.value.phase)

    // Reset with less than 60s elapsed should NOT record an interrupted session
    timerRepository.onTimerReset(FocusMode.STUDY_MODE, durationMinutes = 25, elapsedSeconds = 30)
    delay(150)
    assertTrue(sessionRepository.observeAllSessions().first().isEmpty())

    // Reset with >= 60s elapsed records INTERRUPTED
    timerRepository.onTimerStarted(FocusMode.DEEP_FOCUS, durationMinutes = 45)
    timerRepository.onTimerReset(FocusMode.DEEP_FOCUS, durationMinutes = 45, elapsedSeconds = 180)
    delay(150)
    val sessionsAfterInterrupt = sessionRepository.observeAllSessions().first()
    assertEquals(1, sessionsAfterInterrupt.size)
    assertEquals(SessionOutcome.INTERRUPTED, sessionsAfterInterrupt.first().outcome)

    // Completion records COMPLETED
    timerRepository.onTimerStarted(FocusMode.LIGHT_FOCUS, durationMinutes = 20)
    timerRepository.incrementDismissedNotificationCount()
    timerRepository.onTimerCompleted()
    delay(150)
    val sessionsAfterComplete = sessionRepository.observeAllSessions().first()
    assertEquals(2, sessionsAfterComplete.size)
    assertTrue(sessionsAfterComplete.any { it.outcome == SessionOutcome.COMPLETED && it.dismissedNotificationsCount == 1 })
  }
}

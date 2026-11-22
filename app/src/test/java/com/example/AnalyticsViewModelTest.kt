package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.rules.NotificationRuleRepository
import com.example.data.session.SessionRepository
import com.example.database.FocusFlowDatabase
import com.example.model.FocusMode
import com.example.model.SessionOutcome
import com.example.ui.viewmodel.AnalyticsPeriod
import com.example.ui.viewmodel.AnalyticsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AnalyticsViewModelTest {

  private val mainDispatcher = UnconfinedTestDispatcher()
  private lateinit var database: FocusFlowDatabase
  private lateinit var sessionRepository: SessionRepository
  private lateinit var ruleRepository: NotificationRuleRepository

  @Before
  fun setUp() {
    Dispatchers.setMain(mainDispatcher)
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
  }

  @After
  fun tearDown() {
    database.close()
    Dispatchers.resetMain()
  }

  @Test
  fun `computes completion rate streak seven day bars and top distractors`() = runBlocking {
    val now = System.currentTimeMillis()
    val oneDayMs = 24L * 60L * 60L * 1000L

    sessionRepository.recordSession(
      mode = FocusMode.STUDY_MODE,
      plannedDurationMinutes = 25,
      actualDurationSeconds = 1500,
      startedAtEpochMs = now - 2000L,
      endedAtEpochMs = now,
      outcome = SessionOutcome.COMPLETED,
    )
    sessionRepository.recordSession(
      mode = FocusMode.DEEP_FOCUS,
      plannedDurationMinutes = 45,
      actualDurationSeconds = 2700,
      startedAtEpochMs = now - oneDayMs - 3000L,
      endedAtEpochMs = now - oneDayMs,
      outcome = SessionOutcome.COMPLETED,
    )
    sessionRepository.recordSession(
      mode = FocusMode.LIGHT_FOCUS,
      plannedDurationMinutes = 20,
      actualDurationSeconds = 600,
      startedAtEpochMs = now - (10L * oneDayMs),
      endedAtEpochMs = now - (10L * oneDayMs) + 600_000L,
      outcome = SessionOutcome.INTERRUPTED,
    )

    ruleRepository.logDistractionEvent("com.instagram.android", "Instagram", FocusMode.STUDY_MODE, "DISMISSED")
    ruleRepository.logDistractionEvent("com.instagram.android", "Instagram", FocusMode.STUDY_MODE, "DISMISSED")
    ruleRepository.logDistractionEvent("com.google.android.youtube", "YouTube", FocusMode.DEEP_FOCUS, "DISMISSED")

    val viewModel = AnalyticsViewModel(sessionRepository, ruleRepository)

    val weekState =
      withTimeout(5000) {
        viewModel.uiState.first { it.completedCount == 2 && it.dismissedNotificationsCount == 3 }
      }
    assertEquals(AnalyticsPeriod.WEEK, weekState.selectedPeriod)
    assertEquals(70, weekState.focusMinutes)
    assertEquals(0, weekState.interruptedCount)
    assertEquals(100, weekState.completionRatePercent)
    assertEquals(2, weekState.streakDays)
    assertEquals(7, weekState.weeklyTrend.size)
    assertEquals("Instagram", weekState.topDistractingApps.first().appLabel)
    assertEquals(2, weekState.topDistractingApps.first().dismissedCount)

    // Switch to TODAY: only 25m session
    viewModel.selectPeriod(AnalyticsPeriod.TODAY)
    val todayState =
      withTimeout(5000) {
        viewModel.uiState.first { it.selectedPeriod == AnalyticsPeriod.TODAY && it.focusMinutes == 25 }
      }
    assertEquals(1, todayState.completedCount)

    // Switch to ALL_TIME: includes the 10-day-old interrupted 10m session
    viewModel.selectPeriod(AnalyticsPeriod.ALL_TIME)
    val allState =
      withTimeout(5000) {
        viewModel.uiState.first { it.selectedPeriod == AnalyticsPeriod.ALL_TIME && it.interruptedCount == 1 }
      }
    assertEquals(80, allState.focusMinutes)
    assertEquals(2, allState.completedCount)
    assertEquals(66, allState.completionRatePercent)

    // Clear history empties analytics state
    viewModel.clearAllHistory()
    val clearedState =
      withTimeout(5000) {
        viewModel.uiState.first {
          it.sessions.isEmpty() && it.focusMinutes == 0 && it.dismissedNotificationsCount == 0
        }
      }
    assertTrue(clearedState.sessions.isEmpty())
    assertEquals(0, clearedState.dismissedNotificationsCount)
  }
}

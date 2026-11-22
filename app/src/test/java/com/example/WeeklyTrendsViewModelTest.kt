package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.FocusPreferencesRepository
import com.example.data.rules.NotificationRuleRepository
import com.example.data.session.SessionRepository
import com.example.database.FocusFlowDatabase
import com.example.model.FocusMode
import com.example.model.SessionOutcome
import com.example.ui.viewmodel.TrendsRangeFilter
import com.example.ui.viewmodel.WeeklyTrendsViewModel
import java.util.Calendar
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WeeklyTrendsViewModelTest {

  private val mainDispatcher = UnconfinedTestDispatcher()
  private lateinit var context: Context
  private lateinit var database: FocusFlowDatabase
  private lateinit var sessionRepository: SessionRepository
  private lateinit var ruleRepository: NotificationRuleRepository
  private lateinit var preferencesRepository: FocusPreferencesRepository

  @Before
  fun setUp() {
    Dispatchers.setMain(mainDispatcher)
    context = ApplicationProvider.getApplicationContext()
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
    preferencesRepository = FocusPreferencesRepository(context)
  }

  @After
  fun tearDown() {
    database.close()
    Dispatchers.resetMain()
  }

  @Test
  fun `empty history exposes clean zero state and dash peak day`() = runBlocking {
    val viewModel =
      WeeklyTrendsViewModel(
        sessionRepository = sessionRepository,
        notificationRuleRepository = ruleRepository,
        preferencesRepository = preferencesRepository,
      )

    val state =
      withTimeout(5000) {
        viewModel.uiState.first { it.buckets.size == 4 }
      }

    assertFalse(state.hasHistoricalData)
    assertEquals(0, state.totalPeriodMinutes)
    assertEquals(0, state.averageProductivityScore)
    assertEquals("—", state.peakFocusDayLabel)
    assertEquals(4, state.buckets.size)
    assertTrue(state.buckets.all { it.studyMinutes == 0 && it.productivityScore == 0 })
  }

  @Test
  fun `aggregates multi week study hours productivity score and range filters`() = runBlocking {
    // Anchor at a fixed Wednesday 14:00 so Monday of this week is 2 days earlier
    val anchorCal =
      Calendar.getInstance().apply {
        set(2026, Calendar.MAY, 13, 14, 0, 0)
        set(Calendar.MILLISECOND, 0)
      }
    val nowMs = anchorCal.timeInMillis
    val oneDayMs = 24L * 60L * 60L * 1000L

    // This week: Wednesday (60m completed Deep Focus) + Monday (30m completed Study Mode) = 90m
    sessionRepository.recordSession(
      mode = FocusMode.DEEP_FOCUS,
      plannedDurationMinutes = 60,
      actualDurationSeconds = 3600,
      startedAtEpochMs = nowMs - 3_600_000L,
      endedAtEpochMs = nowMs,
      outcome = SessionOutcome.COMPLETED,
    )
    sessionRepository.recordSession(
      mode = FocusMode.STUDY_MODE,
      plannedDurationMinutes = 30,
      actualDurationSeconds = 1800,
      startedAtEpochMs = nowMs - (2L * oneDayMs) - 1_800_000L,
      endedAtEpochMs = nowMs - (2L * oneDayMs),
      outcome = SessionOutcome.COMPLETED,
    )

    // Last week (7 days ago, also Wednesday): 45m completed + 15m interrupted = 60m, 50% completion
    sessionRepository.recordSession(
      mode = FocusMode.STUDY_MODE,
      plannedDurationMinutes = 45,
      actualDurationSeconds = 2700,
      startedAtEpochMs = nowMs - (7L * oneDayMs) - 2_700_000L,
      endedAtEpochMs = nowMs - (7L * oneDayMs),
      outcome = SessionOutcome.COMPLETED,
    )
    sessionRepository.recordSession(
      mode = FocusMode.LIGHT_FOCUS,
      plannedDurationMinutes = 30,
      actualDurationSeconds = 900,
      startedAtEpochMs = nowMs - (7L * oneDayMs) - 5_000_000L,
      endedAtEpochMs = nowMs - (7L * oneDayMs) - 4_100_000L,
      outcome = SessionOutcome.INTERRUPTED,
    )

    // Log a distraction event inside the current window
    database.distractionEventDao().insertEvent(
      com.example.database.entities.DistractionEventEntity(
        sessionId = null,
        packageName = "com.instagram.android",
        appLabel = "Instagram",
        focusMode = FocusMode.STUDY_MODE.name,
        timestampEpochMs = nowMs - 60_000L,
        outcome = "APP_BLOCKED",
      )
    )

    val viewModel =
      WeeklyTrendsViewModel(
        sessionRepository = sessionRepository,
        notificationRuleRepository = ruleRepository,
        preferencesRepository = preferencesRepository,
        nowProvider = { nowMs },
      )

    val fourWeekState =
      withTimeout(5000) {
        viewModel.uiState.first {
          it.selectedRange == TrendsRangeFilter.LAST_4_WEEKS &&
            it.totalPeriodMinutes == 150 &&
            it.totalDistractionsPrevented == 1
        }
      }

    assertTrue(fourWeekState.hasHistoricalData)
    assertEquals(4, fourWeekState.buckets.size)
    assertEquals("This Wk", fourWeekState.buckets.last().label)
    assertEquals(90, fourWeekState.buckets[3].studyMinutes)
    assertEquals(100, fourWeekState.buckets[3].completionRatePercent)
    assertEquals(1, fourWeekState.buckets[3].distractionsPrevented)
    assertEquals(60, fourWeekState.buckets[2].studyMinutes)
    assertEquals(50, fourWeekState.buckets[2].completionRatePercent)

    // Current week (90m) vs previous week (60m) = +50%
    assertEquals(50, fourWeekState.studyTimeDeltaPercent)
    assertNotNull(fourWeekState.productivityDeltaPoints)
    assertTrue(fourWeekState.buckets[3].productivityScore > fourWeekState.buckets[2].productivityScore)
    // Wednesday had 60m + 45m + 15m = 120m across both weeks -> Wednesday is peak
    assertEquals("Wednesday", fourWeekState.peakFocusDayLabel)
    assertEquals(3, fourWeekState.totalCompletedSessions)

    // Selecting bucket index updates selectedBucketIndex
    viewModel.selectBucket(2)
    val selectedState =
      withTimeout(5000) {
        viewModel.uiState.first { it.selectedBucketIndex == 2 }
      }
    assertEquals(60, selectedState.buckets[selectedState.selectedBucketIndex].studyMinutes)

    // Switching to 8 Weeks creates 8 weekly buckets
    viewModel.selectRange(TrendsRangeFilter.LAST_8_WEEKS)
    val eightWeekState =
      withTimeout(5000) {
        viewModel.uiState.first {
          it.selectedRange == TrendsRangeFilter.LAST_8_WEEKS && it.buckets.size == 8
        }
      }
    assertEquals(7, eightWeekState.selectedBucketIndex)

    // Switching to 7 Days creates 7 daily buckets ending with Today
    viewModel.selectRange(TrendsRangeFilter.THIS_WEEK_DAILY)
    val dailyState =
      withTimeout(5000) {
        viewModel.uiState.first {
          it.selectedRange == TrendsRangeFilter.THIS_WEEK_DAILY && it.buckets.size == 7
        }
      }
    assertEquals("Today", dailyState.buckets.last().label)
    assertEquals(60, dailyState.buckets.last().studyMinutes)
  }
}

package com.example

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.action.action
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.assertHasText
import androidx.glance.testing.unit.hasTestTag
import com.example.data.analytics.MilestoneCalculator
import com.example.model.DistractionEventRecord
import com.example.model.FocusMode
import com.example.model.FocusSessionSummary
import com.example.model.MilestoneBadgeId
import com.example.model.SessionOutcome
import com.example.model.TimerPhase
import com.example.security.PinHashUtil
import com.example.widget.StudyGuardWidgetContent
import com.example.widget.StudyWidgetSnapshot
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MilestonesAndWidgetTest {

  @Test
  fun `pin hash utility validates 4-digit format and salted verification`() {
    assertFalse(PinHashUtil.isValidFourDigitPin("123"))
    assertFalse(PinHashUtil.isValidFourDigitPin("12a4"))
    assertTrue(PinHashUtil.isValidFourDigitPin("0429"))

    val hash = PinHashUtil.hashPin("0429")
    assertTrue(PinHashUtil.verifyPin("0429", hash))
    assertFalse(PinHashUtil.verifyPin("9999", hash))
  }

  @Test
  fun `milestone calculator tracks level progression and unlocks streak and clean week badges`() {
    // Anchor to a clean Monday in a calendar week so all 7 consecutive days stay cleanly verifiable
    val baseMonday =
      Calendar.getInstance().apply {
        set(Calendar.YEAR, 2026)
        set(Calendar.MONTH, Calendar.NOVEMBER)
        set(Calendar.DAY_OF_MONTH, 2) // Monday Nov 2, 2026
        set(Calendar.HOUR_OF_DAY, 10)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }.timeInMillis
    val oneDayMs = 86_400_000L

    // 7 consecutive days of Deep Focus / Study Mode, 90 minutes/day = 630 minutes (10.5 hours)
    val sessions =
      (0 until 7).map { dayOffset ->
        FocusSessionSummary(
          id = (dayOffset + 1).toLong(),
          mode = if (dayOffset < 5) FocusMode.DEEP_FOCUS else FocusMode.STUDY_MODE,
          plannedMinutes = 90,
          elapsedSeconds = 90 * 60,
          outcome = SessionOutcome.COMPLETED,
          completedAtEpochMs = baseMonday + (dayOffset * oneDayMs),
          dismissedNotificationsCount = 4,
        )
      }

    val distractions =
      (1..26).map { index ->
        DistractionEventRecord(
          id = index.toLong(),
          sessionId = null,
          packageName = "com.instagram.android",
          appLabel = "Instagram",
          focusMode = FocusMode.STUDY_MODE,
          timestampEpochMs = baseMonday + index * 1000L,
          outcome = if (index % 2 == 0) "APP_BLOCKED" else "DISMISSED",
        )
      }

    val summary = MilestoneCalculator.calculate(sessions, distractions)

    // Level 3: Deep Thinker (>= 600 min)
    assertEquals(3, summary.level.levelNumber)
    assertEquals("Deep Thinker", summary.level.title)
    assertEquals(6, summary.unlockedCount)

    val unlockedIds = summary.badges.filter { it.isUnlocked }.map { it.id }.toSet()
    assertTrue(unlockedIds.contains(MilestoneBadgeId.FIRST_FOCUS))
    assertTrue(unlockedIds.contains(MilestoneBadgeId.TEN_HOURS))
    assertTrue(unlockedIds.contains(MilestoneBadgeId.STRICT_STREAK_7))
    assertTrue(unlockedIds.contains(MilestoneBadgeId.ZERO_DISTRACTION_WEEK))
    assertTrue(unlockedIds.contains(MilestoneBadgeId.DEEP_SCHOLAR_5))
    assertTrue(unlockedIds.contains(MilestoneBadgeId.SHIELD_GUARDIAN))
  }

  @Test
  fun `glance widget renders today study progress remaining goal and quick action`() =
    runGlanceAppWidgetUnitTest {
      setAppWidgetSize(DpSize(250.dp, 130.dp))

      provideComposable {
        StudyGuardWidgetContent(
          snapshot =
            StudyWidgetSnapshot(
              todayMinutes = 45,
              dailyGoalMinutes = 120,
              timerPhase = TimerPhase.IDLE,
              activeModeTitle = "Study Mode",
              remainingSeconds = 25 * 60,
              isStrictLocked = false,
            ),
          onCardClick = action {},
          onActionClick = action {},
        )
      }

      onNode(hasTestTag("widget_status_label")).assertHasText("Ready")
      onNode(hasTestTag("widget_progress_summary")).assertHasText("45m / 2h")
      onNode(hasTestTag("widget_remaining_goal")).assertHasText("1h 15m left to goal")
      onNode(hasTestTag("widget_action_button")).assertHasText("Start Study")
    }
}

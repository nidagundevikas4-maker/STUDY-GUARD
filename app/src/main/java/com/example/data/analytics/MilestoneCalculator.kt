package com.example.data.analytics

import com.example.model.DistractionEventRecord
import com.example.model.FocusMode
import com.example.model.FocusSessionSummary
import com.example.model.MilestoneBadge
import com.example.model.MilestoneBadgeId
import com.example.model.SessionOutcome
import com.example.model.StudyLevelInfo
import com.example.model.StudyMilestoneSummary
import java.util.Calendar
import java.util.Locale

object MilestoneCalculator {

  private data class LevelTier(
    val number: Int,
    val title: String,
    val minMinutes: Int,
  )

  private val LevelTiers =
    listOf(
      LevelTier(number = 1, title = "Novice Scholar", minMinutes = 0),
      LevelTier(number = 2, title = "Steady Apprentice", minMinutes = 120),
      LevelTier(number = 3, title = "Deep Thinker", minMinutes = 600),
      LevelTier(number = 4, title = "Focus Fellow", minMinutes = 1500),
      LevelTier(number = 5, title = "Dean's Honor", minMinutes = 3600),
      LevelTier(number = 6, title = "Grand Scholar", minMinutes = 7200),
    )

  fun calculate(
    sessions: List<FocusSessionSummary>,
    distractions: List<DistractionEventRecord>,
  ): StudyMilestoneSummary {
    val completedSessions = sessions.filter { it.outcome == SessionOutcome.COMPLETED }
    val totalFocusMinutes = sessions.sumOf { it.elapsedSeconds / 60 }
    val completedCount = completedSessions.size
    val deepFocusCompletedCount = completedSessions.count { it.mode == FocusMode.DEEP_FOCUS }
    val bestStrictStreak = calculateBestStrictLockStreakDays(completedSessions)
    val bestCleanWeekSessions = calculateBestCleanWeekCompletedCount(sessions)
    val totalShielded = distractions.count { it.outcome == "DISMISSED" || it.outcome == "APP_BLOCKED" }

    val level = resolveLevel(totalFocusMinutes)

    val badges =
      listOf(
        MilestoneBadge(
          id = MilestoneBadgeId.FIRST_FOCUS,
          title = "First Focus",
          description = "Complete your first study session from start to finish.",
          isUnlocked = completedCount >= 1,
          currentProgress = completedCount.coerceAtMost(1),
          targetProgress = 1,
          progressText = if (completedCount >= 1) "Unlocked" else "0 / 1 session",
        ),
        MilestoneBadge(
          id = MilestoneBadgeId.TEN_HOURS,
          title = "First 10 Hours",
          description = "Accumulate 10 total hours of focused study time.",
          isUnlocked = totalFocusMinutes >= 600,
          currentProgress = totalFocusMinutes.coerceAtMost(600),
          targetProgress = 600,
          progressText =
            if (totalFocusMinutes >= 600) "Unlocked (10+ hrs)"
            else String.format(Locale.US, "%.1f / 10 hrs", totalFocusMinutes / 60f),
        ),
        MilestoneBadge(
          id = MilestoneBadgeId.STRICT_STREAK_7,
          title = "7-Day Strict Lock Streak",
          description = "Complete a Study Mode or Deep Focus session for 7 consecutive days.",
          isUnlocked = bestStrictStreak >= 7,
          currentProgress = bestStrictStreak.coerceAtMost(7),
          targetProgress = 7,
          progressText =
            if (bestStrictStreak >= 7) "Unlocked ($bestStrictStreak days)"
            else "$bestStrictStreak / 7 days",
        ),
        MilestoneBadge(
          id = MilestoneBadgeId.ZERO_DISTRACTION_WEEK,
          title = "Zero-Distraction Week",
          description = "Finish 5+ sessions in a calendar week with zero early interruptions.",
          isUnlocked = bestCleanWeekSessions >= 5,
          currentProgress = bestCleanWeekSessions.coerceAtMost(5),
          targetProgress = 5,
          progressText =
            if (bestCleanWeekSessions >= 5) "Unlocked ($bestCleanWeekSessions clean sessions)"
            else "$bestCleanWeekSessions / 5 clean sessions",
        ),
        MilestoneBadge(
          id = MilestoneBadgeId.DEEP_SCHOLAR_5,
          title = "Deep Scholar",
          description = "Complete 5 Deep Focus sessions with strict notification discipline.",
          isUnlocked = deepFocusCompletedCount >= 5,
          currentProgress = deepFocusCompletedCount.coerceAtMost(5),
          targetProgress = 5,
          progressText =
            if (deepFocusCompletedCount >= 5) "Unlocked ($deepFocusCompletedCount sessions)"
            else "$deepFocusCompletedCount / 5 sessions",
        ),
        MilestoneBadge(
          id = MilestoneBadgeId.SHIELD_GUARDIAN,
          title = "Shield Guardian",
          description = "Let Study Guard silence or block 25 distracting alerts or app launches.",
          isUnlocked = totalShielded >= 25,
          currentProgress = totalShielded.coerceAtMost(25),
          targetProgress = 25,
          progressText =
            if (totalShielded >= 25) "Unlocked ($totalShielded blocked)"
            else "$totalShielded / 25 blocked",
        ),
      )

    return StudyMilestoneSummary(level = level, badges = badges)
  }

  fun resolveLevel(totalMinutes: Int): StudyLevelInfo {
    val safeMinutes = totalMinutes.coerceAtLeast(0)
    val currentIndex =
      LevelTiers.indexOfLast { safeMinutes >= it.minMinutes }.coerceAtLeast(0)
    val currentTier = LevelTiers[currentIndex]
    val nextTier = LevelTiers.getOrNull(currentIndex + 1)
    return StudyLevelInfo(
      levelNumber = currentTier.number,
      title = currentTier.title,
      nextLevelTitle = nextTier?.title,
      totalCompletedMinutes = safeMinutes,
      levelStartMinutes = currentTier.minMinutes,
      nextLevelMinutes = nextTier?.minMinutes,
    )
  }

  private fun calculateBestStrictLockStreakDays(completedSessions: List<FocusSessionSummary>): Int {
    val strictEpochDays =
      completedSessions
        .filter { it.mode == FocusMode.STUDY_MODE || it.mode == FocusMode.DEEP_FOCUS }
        .map { localEpochDay(it.completedAtEpochMs) }
        .distinct()
        .sorted()

    if (strictEpochDays.isEmpty()) return 0

    var best = 1
    var current = 1
    for (i in 1 until strictEpochDays.size) {
      if (strictEpochDays[i] == strictEpochDays[i - 1] + 1L) {
        current++
        if (current > best) best = current
      } else {
        current = 1
      }
    }
    return best
  }

  private fun calculateBestCleanWeekCompletedCount(allSessions: List<FocusSessionSummary>): Int {
    if (allSessions.isEmpty()) return 0
    val byWeek = allSessions.groupBy { weekKey(it.completedAtEpochMs) }
    return byWeek.values
      .filter { weekSessions -> weekSessions.none { it.outcome == SessionOutcome.INTERRUPTED } }
      .maxOfOrNull { weekSessions -> weekSessions.count { it.outcome == SessionOutcome.COMPLETED } }
      ?: 0
  }

  private fun localEpochDay(epochMs: Long): Long {
    val cal =
      Calendar.getInstance().apply {
        timeInMillis = epochMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
    val offsetMs = cal.timeZone.getOffset(cal.timeInMillis)
    return (cal.timeInMillis + offsetMs) / 86_400_000L
  }

  private fun weekKey(epochMs: Long): String {
    val cal =
      Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        minimalDaysInFirstWeek = 4
        timeInMillis = epochMs
      }
    val weekYear =
      if (cal.isWeekDateSupported) cal.weekYear else cal.get(Calendar.YEAR)
    val weekOfYear = cal.get(Calendar.WEEK_OF_YEAR)
    return "$weekYear-W$weekOfYear"
  }
}

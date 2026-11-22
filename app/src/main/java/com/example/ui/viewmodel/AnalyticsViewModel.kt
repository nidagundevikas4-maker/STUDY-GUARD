package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.rules.NotificationRuleRepository
import com.example.data.session.SessionRepository
import com.example.model.DistractionEventRecord
import com.example.model.FocusMode
import com.example.model.FocusSessionSummary
import com.example.model.SessionOutcome
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AnalyticsPeriod(val label: String) {
  TODAY("Today"),
  WEEK("7 Days"),
  ALL_TIME("All Time"),
}

data class DailyFocusBar(
  val dayLabel: String,
  val minutes: Int,
  val isToday: Boolean,
)

data class ModeBreakdownItem(
  val mode: FocusMode,
  val minutes: Int,
  val sessionCount: Int,
  val fraction: Float,
)

data class TopDistractingApp(
  val packageName: String,
  val appLabel: String,
  val dismissedCount: Int,
)

data class AnalyticsUiState(
  val selectedPeriod: AnalyticsPeriod = AnalyticsPeriod.WEEK,
  val focusMinutes: Int = 0,
  val completedCount: Int = 0,
  val interruptedCount: Int = 0,
  val completionRatePercent: Int = 0,
  val streakDays: Int = 0,
  val dismissedNotificationsCount: Int = 0,
  val blockedAppAttemptsCount: Int = 0,
  val weeklyTrend: List<DailyFocusBar> = emptyList(),
  val modeBreakdown: List<ModeBreakdownItem> = emptyList(),
  val topDistractingApps: List<TopDistractingApp> = emptyList(),
  val sessions: List<FocusSessionSummary> = emptyList(),
)

class AnalyticsViewModel(
  private val sessionRepository: SessionRepository,
  private val notificationRuleRepository: NotificationRuleRepository,
) : ViewModel() {

  private val selectedPeriodFlow = MutableStateFlow(AnalyticsPeriod.WEEK)

  val uiState: StateFlow<AnalyticsUiState> =
    combine(
      sessionRepository.observeAllSessions(),
      notificationRuleRepository.observeRecentDistractions(),
      selectedPeriodFlow,
    ) { allSessions, allDistractions, period ->
      buildUiState(
        allSessions = allSessions,
        allDistractions = allDistractions,
        period = period,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = AnalyticsUiState(),
    )

  fun selectPeriod(period: AnalyticsPeriod) {
    selectedPeriodFlow.value = period
  }

  fun deleteSession(sessionId: Long) {
    viewModelScope.launch { sessionRepository.deleteSession(sessionId) }
  }

  fun clearAllHistory() {
    viewModelScope.launch { sessionRepository.clearAllHistory() }
  }

  private fun buildUiState(
    allSessions: List<FocusSessionSummary>,
    allDistractions: List<DistractionEventRecord>,
    period: AnalyticsPeriod,
  ): AnalyticsUiState {
    val startOfPeriodMs = startOfPeriodEpochMs(period)

    val filteredSessions =
      if (startOfPeriodMs == 0L) allSessions
      else allSessions.filter { it.completedAtEpochMs >= startOfPeriodMs }

    val filteredDistractions =
      if (startOfPeriodMs == 0L) allDistractions
      else allDistractions.filter { it.timestampEpochMs >= startOfPeriodMs }

    val focusMinutes = filteredSessions.sumOf { it.elapsedSeconds / 60 }
    val completedCount = filteredSessions.count { it.outcome == SessionOutcome.COMPLETED }
    val interruptedCount = filteredSessions.count { it.outcome == SessionOutcome.INTERRUPTED }
    val totalCount = completedCount + interruptedCount
    val completionRate =
      if (totalCount == 0) 0 else ((completedCount * 100f) / totalCount).toInt()

    val weeklyTrend = buildSevenDayBars(allSessions)

    val totalModeMinutes = focusMinutes.coerceAtLeast(1)
    val modeBreakdown =
      FocusMode.entries.map { mode ->
        val modeSessions = filteredSessions.filter { it.mode == mode }
        val modeMinutes = modeSessions.sumOf { it.elapsedSeconds / 60 }
        ModeBreakdownItem(
          mode = mode,
          minutes = modeMinutes,
          sessionCount = modeSessions.size,
          fraction = if (focusMinutes == 0) 0f else (modeMinutes.toFloat() / totalModeMinutes.toFloat()),
        )
      }

    val topApps =
      filteredDistractions
        .filter { it.outcome == "DISMISSED" || it.outcome == "APP_BLOCKED" }
        .groupBy { it.packageName }
        .map { (pkg, events) ->
          TopDistractingApp(
            packageName = pkg,
            appLabel = events.first().appLabel,
            dismissedCount = events.size,
          )
        }
        .sortedByDescending { it.dismissedCount }
        .take(5)

    return AnalyticsUiState(
      selectedPeriod = period,
      focusMinutes = focusMinutes,
      completedCount = completedCount,
      interruptedCount = interruptedCount,
      completionRatePercent = completionRate,
      streakDays = calculateStreakDays(allSessions),
      dismissedNotificationsCount = filteredDistractions.count { it.outcome == "DISMISSED" },
      blockedAppAttemptsCount = filteredDistractions.count { it.outcome == "APP_BLOCKED" },
      weeklyTrend = weeklyTrend,
      modeBreakdown = modeBreakdown,
      topDistractingApps = topApps,
      sessions = filteredSessions,
    )
  }

  private fun startOfPeriodEpochMs(period: AnalyticsPeriod): Long {
    if (period == AnalyticsPeriod.ALL_TIME) return 0L
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    if (period == AnalyticsPeriod.WEEK) {
      cal.add(Calendar.DAY_OF_YEAR, -6)
    }
    return cal.timeInMillis
  }

  private fun buildSevenDayBars(allSessions: List<FocusSessionSummary>): List<DailyFocusBar> {
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val minutesByDayKey =
      allSessions
        .groupBy { dayKeyFormat.format(Date(it.completedAtEpochMs)) }
        .mapValues { (_, list) -> list.sumOf { it.elapsedSeconds / 60 } }

    return (6 downTo 0).map { daysAgo ->
      val cal = Calendar.getInstance()
      cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
      val key = dayKeyFormat.format(cal.time)
      DailyFocusBar(
        dayLabel = dayFormat.format(cal.time).take(3),
        minutes = minutesByDayKey[key] ?: 0,
        isToday = daysAgo == 0,
      )
    }
  }

  private fun calculateStreakDays(allSessions: List<FocusSessionSummary>): Int {
    val completedDays =
      allSessions
        .filter { it.outcome == SessionOutcome.COMPLETED }
        .map { dayIndex(it.completedAtEpochMs) }
        .toSet()
    if (completedDays.isEmpty()) return 0

    val todayIndex = dayIndex(System.currentTimeMillis())
    var cursor = if (completedDays.contains(todayIndex)) todayIndex else todayIndex - 1
    var streak = 0
    while (completedDays.contains(cursor)) {
      streak++
      cursor--
    }
    return streak
  }

  private fun dayIndex(epochMs: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
    return (cal.get(Calendar.YEAR) * 1000L) + cal.get(Calendar.DAY_OF_YEAR)
  }

  class Factory(
    private val sessionRepository: SessionRepository,
    private val notificationRuleRepository: NotificationRuleRepository,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      require(modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
        "Unknown ViewModel class: ${modelClass.name}"
      }
      return AnalyticsViewModel(sessionRepository, notificationRuleRepository) as T
    }
  }
}

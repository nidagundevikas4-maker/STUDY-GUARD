package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.FocusPreferencesRepository
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
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class TrendsRangeFilter(val label: String) {
  LAST_4_WEEKS("4 Weeks"),
  LAST_8_WEEKS("8 Weeks"),
  THIS_WEEK_DAILY("7 Days"),
}

data class WeeklyTrendBucket(
  val label: String,
  val rangeSubtitle: String,
  val studyMinutes: Int,
  val goalTargetMinutes: Int,
  val completedSessions: Int,
  val interruptedSessions: Int,
  val completionRatePercent: Int,
  val distractionsPrevented: Int,
  val productivityScore: Int,
  val isCurrentPeriod: Boolean,
)

data class WeeklyTrendsUiState(
  val selectedRange: TrendsRangeFilter = TrendsRangeFilter.LAST_4_WEEKS,
  val buckets: List<WeeklyTrendBucket> = emptyList(),
  val selectedBucketIndex: Int = 0,
  val dailyGoalMinutes: Int = 120,
  val totalPeriodMinutes: Int = 0,
  val averageProductivityScore: Int = 0,
  val studyTimeDeltaPercent: Int? = null,
  val productivityDeltaPoints: Int? = null,
  val peakFocusDayLabel: String = "—",
  val averageSessionMinutes: Int = 0,
  val totalDistractionsPrevented: Int = 0,
  val totalCompletedSessions: Int = 0,
  val hasHistoricalData: Boolean = false,
)

class WeeklyTrendsViewModel(
  private val sessionRepository: SessionRepository,
  private val notificationRuleRepository: NotificationRuleRepository,
  private val preferencesRepository: FocusPreferencesRepository,
  private val nowProvider: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

  private val selectedRangeFlow = MutableStateFlow(TrendsRangeFilter.LAST_4_WEEKS)
  private val selectedIndexFlow = MutableStateFlow<Int?>(null)

  val uiState: StateFlow<WeeklyTrendsUiState> =
    combine(
      sessionRepository.observeAllSessions(),
      notificationRuleRepository.observeRecentDistractions(),
      preferencesRepository.preferencesFlow,
      selectedRangeFlow,
      selectedIndexFlow,
    ) { allSessions, allDistractions, prefs, range, chosenIndex ->
      buildUiState(
        allSessions = allSessions,
        allDistractions = allDistractions,
        dailyGoalMinutes = prefs.dailyGoalMinutes,
        range = range,
        chosenIndex = chosenIndex,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = WeeklyTrendsUiState(),
    )

  fun selectRange(range: TrendsRangeFilter) {
    selectedRangeFlow.value = range
    selectedIndexFlow.value = null
  }

  fun selectBucket(index: Int) {
    selectedIndexFlow.value = index
  }

  private fun buildUiState(
    allSessions: List<FocusSessionSummary>,
    allDistractions: List<DistractionEventRecord>,
    dailyGoalMinutes: Int,
    range: TrendsRangeFilter,
    chosenIndex: Int?,
  ): WeeklyTrendsUiState {
    val nowMs = nowProvider()
    val buckets =
      when (range) {
        TrendsRangeFilter.LAST_4_WEEKS ->
          buildWeeklyBuckets(allSessions, allDistractions, dailyGoalMinutes, weekCount = 4, nowMs = nowMs)
        TrendsRangeFilter.LAST_8_WEEKS ->
          buildWeeklyBuckets(allSessions, allDistractions, dailyGoalMinutes, weekCount = 8, nowMs = nowMs)
        TrendsRangeFilter.THIS_WEEK_DAILY ->
          buildDailyBuckets(allSessions, allDistractions, dailyGoalMinutes, dayCount = 7, nowMs = nowMs)
      }

    val safeIndex =
      if (buckets.isEmpty()) 0
      else (chosenIndex ?: buckets.lastIndex).coerceIn(0, buckets.lastIndex)

    val windowStartMs = windowStartEpochMs(range, nowMs)
    val windowSessions = allSessions.filter { it.completedAtEpochMs >= windowStartMs }
    val windowDistractions =
      allDistractions.filter {
        it.timestampEpochMs >= windowStartMs &&
          (it.outcome == "DISMISSED" || it.outcome == "APP_BLOCKED")
      }

    val totalPeriodMinutes = buckets.sumOf { it.studyMinutes }
    val activeBuckets = buckets.filter { it.completedSessions + it.interruptedSessions > 0 || it.studyMinutes > 0 }
    val avgProductivityScore =
      if (activeBuckets.isEmpty()) 0
      else activeBuckets.map { it.productivityScore }.average().roundToInt()

    val currentBucket = buckets.lastOrNull()
    val previousBucket = if (buckets.size >= 2) buckets[buckets.lastIndex - 1] else null

    val studyTimeDeltaPercent =
      if (currentBucket != null && previousBucket != null && previousBucket.studyMinutes > 0) {
        (((currentBucket.studyMinutes - previousBucket.studyMinutes) * 100f) / previousBucket.studyMinutes)
          .roundToInt()
      } else {
        null
      }

    val previousHadActivity =
      previousBucket != null &&
        (previousBucket.completedSessions + previousBucket.interruptedSessions > 0 ||
          previousBucket.studyMinutes > 0)
    val productivityDeltaPoints =
      if (currentBucket != null && previousBucket != null && previousHadActivity) {
        currentBucket.productivityScore - previousBucket.productivityScore
      } else {
        null
      }

    val peakFocusDayLabel = computePeakDayLabel(windowSessions)
    val totalCompletedSessions = windowSessions.count { it.outcome == SessionOutcome.COMPLETED }
    val avgSessionMinutes =
      if (windowSessions.isEmpty()) 0
      else (windowSessions.sumOf { it.elapsedSeconds / 60 }.toFloat() / windowSessions.size).roundToInt()

    return WeeklyTrendsUiState(
      selectedRange = range,
      buckets = buckets,
      selectedBucketIndex = safeIndex,
      dailyGoalMinutes = dailyGoalMinutes,
      totalPeriodMinutes = totalPeriodMinutes,
      averageProductivityScore = avgProductivityScore,
      studyTimeDeltaPercent = studyTimeDeltaPercent,
      productivityDeltaPoints = productivityDeltaPoints,
      peakFocusDayLabel = peakFocusDayLabel,
      averageSessionMinutes = avgSessionMinutes,
      totalDistractionsPrevented = windowDistractions.size,
      totalCompletedSessions = totalCompletedSessions,
      hasHistoricalData = windowSessions.isNotEmpty(),
    )
  }

  private fun windowStartEpochMs(range: TrendsRangeFilter, nowMs: Long): Long {
    val cal = startOfDayCalendar(nowMs)
    return when (range) {
      TrendsRangeFilter.THIS_WEEK_DAILY -> {
        cal.add(Calendar.DAY_OF_YEAR, -6)
        cal.timeInMillis
      }
      TrendsRangeFilter.LAST_4_WEEKS -> {
        val currentWeekStart = startOfWeekCalendar(nowMs)
        currentWeekStart.add(Calendar.WEEK_OF_YEAR, -3)
        currentWeekStart.timeInMillis
      }
      TrendsRangeFilter.LAST_8_WEEKS -> {
        val currentWeekStart = startOfWeekCalendar(nowMs)
        currentWeekStart.add(Calendar.WEEK_OF_YEAR, -7)
        currentWeekStart.timeInMillis
      }
    }
  }

  private fun buildWeeklyBuckets(
    allSessions: List<FocusSessionSummary>,
    allDistractions: List<DistractionEventRecord>,
    dailyGoalMinutes: Int,
    weekCount: Int,
    nowMs: Long,
  ): List<WeeklyTrendBucket> {
    val weekLabelFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    val weeklyGoalTarget = (dailyGoalMinutes * 7).coerceAtLeast(30)

    return (weekCount - 1 downTo 0).map { weeksAgo ->
      val weekStart = startOfWeekCalendar(nowMs).apply { add(Calendar.WEEK_OF_YEAR, -weeksAgo) }
      val weekEndExclusive = (weekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 7) }
      val weekEndInclusive = (weekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 6) }

      val startMs = weekStart.timeInMillis
      val endMs = weekEndExclusive.timeInMillis

      val sessions =
        allSessions.filter { it.completedAtEpochMs in startMs until endMs }
      val distractions =
        allDistractions.count {
          it.timestampEpochMs in startMs until endMs &&
            (it.outcome == "DISMISSED" || it.outcome == "APP_BLOCKED")
        }

      val label =
        if (weeksAgo == 0) "This Wk"
        else weekLabelFormat.format(weekStart.time)
      val subtitle =
        "${weekLabelFormat.format(weekStart.time)} – ${weekLabelFormat.format(weekEndInclusive.time)}"

      toBucket(
        label = label,
        rangeSubtitle = subtitle,
        sessions = sessions,
        distractionsCount = distractions,
        goalTargetMinutes = weeklyGoalTarget,
        isCurrentPeriod = weeksAgo == 0,
      )
    }
  }

  private fun buildDailyBuckets(
    allSessions: List<FocusSessionSummary>,
    allDistractions: List<DistractionEventRecord>,
    dailyGoalMinutes: Int,
    dayCount: Int,
    nowMs: Long,
  ): List<WeeklyTrendBucket> {
    val dayShortFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val dayFullFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    val targetGoal = dailyGoalMinutes.coerceAtLeast(15)

    return (dayCount - 1 downTo 0).map { daysAgo ->
      val dayStart = startOfDayCalendar(nowMs).apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
      val dayEndExclusive = (dayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }

      val startMs = dayStart.timeInMillis
      val endMs = dayEndExclusive.timeInMillis

      val sessions =
        allSessions.filter { it.completedAtEpochMs in startMs until endMs }
      val distractions =
        allDistractions.count {
          it.timestampEpochMs in startMs until endMs &&
            (it.outcome == "DISMISSED" || it.outcome == "APP_BLOCKED")
        }

      toBucket(
        label = if (daysAgo == 0) "Today" else dayShortFormat.format(dayStart.time).take(3),
        rangeSubtitle = dayFullFormat.format(dayStart.time),
        sessions = sessions,
        distractionsCount = distractions,
        goalTargetMinutes = targetGoal,
        isCurrentPeriod = daysAgo == 0,
      )
    }
  }

  private fun toBucket(
    label: String,
    rangeSubtitle: String,
    sessions: List<FocusSessionSummary>,
    distractionsCount: Int,
    goalTargetMinutes: Int,
    isCurrentPeriod: Boolean,
  ): WeeklyTrendBucket {
    val studyMinutes = sessions.sumOf { it.elapsedSeconds / 60 }
    val completed = sessions.count { it.outcome == SessionOutcome.COMPLETED }
    val interrupted = sessions.count { it.outcome == SessionOutcome.INTERRUPTED }
    val totalSessions = completed + interrupted

    val completionRatePercent =
      if (totalSessions == 0) 0
      else ((completed * 100f) / totalSessions).roundToInt().coerceIn(0, 100)

    val productivityScore =
      computeProductivityScore(
        sessions = sessions,
        studyMinutes = studyMinutes,
        goalTargetMinutes = goalTargetMinutes,
        completionRatePercent = completionRatePercent,
      )

    return WeeklyTrendBucket(
      label = label,
      rangeSubtitle = rangeSubtitle,
      studyMinutes = studyMinutes,
      goalTargetMinutes = goalTargetMinutes,
      completedSessions = completed,
      interruptedSessions = interrupted,
      completionRatePercent = completionRatePercent,
      distractionsPrevented = distractionsCount,
      productivityScore = productivityScore,
      isCurrentPeriod = isCurrentPeriod,
    )
  }

  private fun computeProductivityScore(
    sessions: List<FocusSessionSummary>,
    studyMinutes: Int,
    goalTargetMinutes: Int,
    completionRatePercent: Int,
  ): Int {
    if (sessions.isEmpty() && studyMinutes == 0) return 0

    // 50% completion consistency + 35% study goal pace + 15% deep/study focus depth
    val completionComponent = (completionRatePercent / 100f) * 50f
    val goalRatio = (studyMinutes.toFloat() / goalTargetMinutes.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val goalComponent = goalRatio * 35f

    val deepOrStudyMinutes =
      sessions
        .filter { it.mode == FocusMode.DEEP_FOCUS || it.mode == FocusMode.STUDY_MODE }
        .sumOf { it.elapsedSeconds / 60 }
    val depthRatio =
      if (studyMinutes <= 0) 0f
      else (deepOrStudyMinutes.toFloat() / studyMinutes.toFloat()).coerceIn(0f, 1f)
    val depthComponent = depthRatio * 15f

    return (completionComponent + goalComponent + depthComponent).roundToInt().coerceIn(0, 100)
  }

  private fun computePeakDayLabel(sessions: List<FocusSessionSummary>): String {
    if (sessions.isEmpty()) return "—"
    val fullDayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    val bestEntry =
      sessions
        .groupBy { session ->
          val cal = Calendar.getInstance().apply { timeInMillis = session.completedAtEpochMs }
          cal.get(Calendar.DAY_OF_WEEK)
        }
        .maxByOrNull { (_, daySessions) -> daySessions.sumOf { it.elapsedSeconds } }
        ?: return "—"

    val representativeEpoch = bestEntry.value.first().completedAtEpochMs
    return fullDayFormat.format(Date(representativeEpoch))
  }

  private fun startOfDayCalendar(epochMs: Long): Calendar =
    Calendar.getInstance().apply {
      timeInMillis = epochMs
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }

  private fun startOfWeekCalendar(epochMs: Long): Calendar =
    startOfDayCalendar(epochMs).apply {
      firstDayOfWeek = Calendar.MONDAY
      val currentDow = get(Calendar.DAY_OF_WEEK)
      val daysSinceMonday = (currentDow - Calendar.MONDAY + 7) % 7
      add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
    }

  class Factory(
    private val sessionRepository: SessionRepository,
    private val notificationRuleRepository: NotificationRuleRepository,
    private val preferencesRepository: FocusPreferencesRepository,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      require(modelClass.isAssignableFrom(WeeklyTrendsViewModel::class.java)) {
        "Unknown ViewModel class: ${modelClass.name}"
      }
      return WeeklyTrendsViewModel(
        sessionRepository = sessionRepository,
        notificationRuleRepository = notificationRuleRepository,
        preferencesRepository = preferencesRepository,
      ) as T
    }
  }
}

package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.FocusFlowApplication
import com.example.MainActivity
import com.example.R
import com.example.model.FocusMode
import com.example.model.TimerPhase
import com.example.services.FocusTimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class StudyWidgetSnapshot(
  val todayMinutes: Int,
  val dailyGoalMinutes: Int,
  val timerPhase: TimerPhase,
  val activeModeTitle: String,
  val remainingSeconds: Int,
  val isStrictLocked: Boolean,
) {
  val remainingGoalMinutes: Int
    get() = (dailyGoalMinutes - todayMinutes).coerceAtLeast(0)

  val progressFraction: Float
    get() =
      if (dailyGoalMinutes <= 0) 0f
      else (todayMinutes.toFloat() / dailyGoalMinutes.toFloat()).coerceIn(0f, 1f)

  val isSessionActive: Boolean
    get() = timerPhase == TimerPhase.RUNNING || timerPhase == TimerPhase.PAUSED
}

class StudyGuardWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = StudyGuardGlanceWidget()
}

class StudyGuardGlanceWidget : GlanceAppWidget() {

  override val sizeMode: SizeMode = SizeMode.Responsive(RESPONSIVE_BUCKETS)

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val appContainer = (context.applicationContext as FocusFlowApplication).appContainer
    val prefs = appContainer.preferencesRepository.preferencesFlow.first()
    val todayMinutes = appContainer.sessionRepository.getTodayStudyMinutesSnapshot()
    val timerState = appContainer.focusTimerRepository.timerState.value

    val snapshot =
      StudyWidgetSnapshot(
        todayMinutes = todayMinutes,
        dailyGoalMinutes = prefs.dailyGoalMinutes,
        timerPhase = timerState.phase,
        activeModeTitle =
          if (timerState.isActive) timerState.mode.title else FocusMode.STUDY_MODE.title,
        remainingSeconds = timerState.remainingSeconds,
        isStrictLocked = timerState.isStrictLocked,
      )

    val openAppIntent = MainActivity.createOpenHomeIntent(context)
    val ctaIntent =
      if (snapshot.isSessionActive) MainActivity.createOpenTimerIntent(context)
      else MainActivity.createQuickStudyIntent(context)

    provideContent {
      GlanceTheme {
        StudyGuardWidgetContent(
          snapshot = snapshot,
          onCardClick = actionStartActivity(openAppIntent),
          onActionClick = actionStartActivity(ctaIntent),
        )
      }
    }
  }

  override fun onCompositionError(
    context: Context,
    glanceId: GlanceId,
    appWidgetId: Int,
    throwable: Throwable,
  ) {
    val fallback =
      RemoteViews(context.packageName, android.R.layout.simple_list_item_1).apply {
        setTextViewText(android.R.id.text1, "Study Guard — Tap to open")
      }
    AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, fallback)
  }

  companion object {
    private val RESPONSIVE_BUCKETS =
      setOf(
        DpSize(180.dp, 110.dp),
        DpSize(250.dp, 130.dp),
        DpSize(320.dp, 180.dp),
      )
  }
}

@Composable
fun StudyGuardWidgetContent(
  snapshot: StudyWidgetSnapshot,
  onCardClick: androidx.glance.action.Action,
  onActionClick: androidx.glance.action.Action,
) {
  val size = LocalSize.current
  val isCompactHeight = size.height < 125.dp

  val statusLabel =
    when {
      snapshot.timerPhase == TimerPhase.RUNNING && snapshot.isStrictLocked ->
        "Locked · ${FocusTimerService.formatCountdown(snapshot.remainingSeconds)}"
      snapshot.timerPhase == TimerPhase.RUNNING ->
        "Focusing · ${FocusTimerService.formatCountdown(snapshot.remainingSeconds)}"
      snapshot.timerPhase == TimerPhase.PAUSED ->
        "Paused · ${FocusTimerService.formatCountdown(snapshot.remainingSeconds)}"
      else -> "Ready"
    }

  val remainingGoalLabel =
    if (snapshot.remainingGoalMinutes == 0) "Daily goal complete!"
    else "${formatWidgetDuration(snapshot.remainingGoalMinutes)} left to goal"

  val buttonText = if (snapshot.isSessionActive) "Open Timer" else "Start Study"

  Column(
    modifier =
      GlanceModifier
        .fillMaxSize()
        .background(GlanceTheme.colors.widgetBackground)
        .cornerRadius(20.dp)
        .clickable(onCardClick)
        .padding(14.dp)
        .semantics { testTag = "widget_root" },
    verticalAlignment = Alignment.Vertical.CenterVertically,
  ) {
    Row(
      modifier = GlanceModifier.fillMaxWidth(),
      verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
      Image(
        provider = ImageProvider(R.drawable.ic_qs_study_guard),
        contentDescription = "Study Guard icon",
        colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
        modifier = GlanceModifier.size(18.dp),
      )
      Spacer(modifier = GlanceModifier.width(6.dp))
      Text(
        text = "Study Guard",
        style =
          TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
          ),
      )
      Spacer(modifier = GlanceModifier.defaultWeight())
      Text(
        text = statusLabel,
        style =
          TextStyle(
            color = GlanceTheme.colors.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
          ),
        modifier = GlanceModifier.semantics { testTag = "widget_status_label" },
      )
    }

    Spacer(modifier = GlanceModifier.height(if (isCompactHeight) 6.dp else 10.dp))

    Row(
      modifier = GlanceModifier.fillMaxWidth(),
      verticalAlignment = Alignment.Vertical.Bottom,
    ) {
      Text(
        text = "${formatWidgetDuration(snapshot.todayMinutes)} / ${formatWidgetDuration(snapshot.dailyGoalMinutes)}",
        style =
          TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
          ),
        modifier = GlanceModifier.semantics { testTag = "widget_progress_summary" },
      )
      Spacer(modifier = GlanceModifier.defaultWeight())
      Text(
        text = remainingGoalLabel,
        style =
          TextStyle(
            color = GlanceTheme.colors.onSurfaceVariant,
            fontSize = 12.sp,
          ),
        modifier = GlanceModifier.semantics { testTag = "widget_remaining_goal" },
      )
    }

    Spacer(modifier = GlanceModifier.height(6.dp))

    LinearProgressIndicator(
      progress = snapshot.progressFraction,
      modifier = GlanceModifier.fillMaxWidth().height(8.dp),
      color = GlanceTheme.colors.primary,
      backgroundColor = GlanceTheme.colors.surfaceVariant,
    )

    Spacer(modifier = GlanceModifier.height(if (isCompactHeight) 8.dp else 12.dp))

    Row(
      modifier = GlanceModifier.fillMaxWidth(),
      horizontalAlignment = Alignment.Horizontal.End,
      verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
      Button(
        text = buttonText,
        onClick = onActionClick,
        modifier = GlanceModifier.semantics { testTag = "widget_action_button" },
      )
    }
  }
}

private fun formatWidgetDuration(minutes: Int): String {
  val safe = minutes.coerceAtLeast(0)
  val hrs = safe / 60
  val mins = safe % 60
  return when {
    hrs > 0 && mins > 0 -> "${hrs}h ${mins}m"
    hrs > 0 -> "${hrs}h"
    else -> "${mins}m"
  }
}

object StudyWidgetUpdater {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  fun requestUpdateAsync(context: Context) {
    val appContext = context.applicationContext
    scope.launch {
      runCatching { StudyGuardGlanceWidget().updateAll(appContext) }
    }
  }
}

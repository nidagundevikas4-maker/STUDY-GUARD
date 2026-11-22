package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.TimerPhase
import com.example.services.FocusTimerService
import com.example.ui.components.DailyProgressCard
import com.example.ui.components.DailyTimelineSection
import com.example.ui.components.FocusModeSelector
import com.example.ui.components.QuickAccessGrid
import com.example.ui.components.SessionDurationPicker
import com.example.ui.components.ShieldStatusCard
import com.example.ui.components.formatStudyDuration
import com.example.ui.theme.MetricNumericStyle
import com.example.ui.viewmodel.HomeUiState
import com.example.ui.viewmodel.HomeViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  uiState: HomeUiState,
  isDarkTheme: Boolean,
  onToggleTheme: () -> Unit,
  onNavigateToTimer: () -> Unit,
  onNavigateToAnalytics: () -> Unit,
  onNavigateToWeeklyTrends: () -> Unit,
  onNavigateToRules: () -> Unit,
  onNavigateToSettings: () -> Unit,
  homeViewModel: HomeViewModel,
  modifier: Modifier = Modifier,
) {
  BoxWithConstraints(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    val isWideLayout = maxWidth >= 640.dp
    val scrollState = rememberScrollState()

    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .verticalScroll(scrollState)
          .padding(horizontal = 20.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier.widthIn(max = if (isWideLayout) 960.dp else 600.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        HomeHeader(
          greetingHour = uiState.greetingHour,
          isDarkTheme = isDarkTheme,
          onToggleTheme = onToggleTheme,
        )

        if (isWideLayout) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
              DailyProgressCard(
                todayStudyMinutes = uiState.todayStudyMinutes,
                dailyGoalMinutes = uiState.dailyGoalMinutes,
                completedSessionsToday = uiState.completedSessionsToday,
                progressFraction = uiState.dailyProgressFraction,
                onEditGoalClick = homeViewModel::openGoalEditor,
              )
              SessionDurationPicker(
                selectedDurationMinutes = uiState.selectedDurationMinutes,
                onDurationSelected = homeViewModel::selectDurationMinutes,
                isLocked = uiState.timerState.isActive,
              )
              StartFocusCtaButton(
                uiState = uiState,
                onClick = onNavigateToTimer,
              )
              DailyTimelineSection(sessions = uiState.todaySessions)
            }

            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
              FocusModeSelector(
                activeMode = uiState.activeFocusMode,
                onModeSelected = homeViewModel::selectFocusMode,
                isLocked = uiState.timerState.isActive,
              )
              QuickAccessGrid(
                onAnalyticsClick = onNavigateToAnalytics,
                onWeeklyTrendsClick = onNavigateToWeeklyTrends,
                onRulesClick = onNavigateToRules,
                onSettingsClick = onNavigateToSettings,
              )
              ShieldStatusCard(
                permissionSnapshot = uiState.permissionSnapshot,
                onOpenRulesPreview = onNavigateToRules,
              )
            }
          }
        } else {
          DailyProgressCard(
            todayStudyMinutes = uiState.todayStudyMinutes,
            dailyGoalMinutes = uiState.dailyGoalMinutes,
            completedSessionsToday = uiState.completedSessionsToday,
            progressFraction = uiState.dailyProgressFraction,
            onEditGoalClick = homeViewModel::openGoalEditor,
          )

          FocusModeSelector(
            activeMode = uiState.activeFocusMode,
            onModeSelected = homeViewModel::selectFocusMode,
            isLocked = uiState.timerState.isActive,
          )

          SessionDurationPicker(
            selectedDurationMinutes = uiState.selectedDurationMinutes,
            onDurationSelected = homeViewModel::selectDurationMinutes,
            isLocked = uiState.timerState.isActive,
          )

          StartFocusCtaButton(
            uiState = uiState,
            onClick = onNavigateToTimer,
          )

          QuickAccessGrid(
            onAnalyticsClick = onNavigateToAnalytics,
            onWeeklyTrendsClick = onNavigateToWeeklyTrends,
            onRulesClick = onNavigateToRules,
            onSettingsClick = onNavigateToSettings,
          )

          DailyTimelineSection(sessions = uiState.todaySessions)

          ShieldStatusCard(
            permissionSnapshot = uiState.permissionSnapshot,
            onOpenRulesPreview = onNavigateToRules,
          )
        }
      }
    }
  }

  if (uiState.isGoalSheetOpen) {
    val goalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = homeViewModel::dismissGoalEditor,
      sheetState = goalSheetState,
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
      DailyGoalSheetContent(
        initialMinutes = uiState.dailyGoalMinutes,
        onSave = homeViewModel::updateDailyGoalMinutes,
        onCancel = homeViewModel::dismissGoalEditor,
      )
    }
  }
}

@Composable
private fun HomeHeader(
  greetingHour: Int,
  isDarkTheme: Boolean,
  onToggleTheme: () -> Unit,
) {
  val greeting =
    when {
      greetingHour < 12 -> stringResource(R.string.home_greeting_morning)
      greetingHour < 17 -> stringResource(R.string.home_greeting_afternoon)
      else -> stringResource(R.string.home_greeting_evening)
    }

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Spa,
            contentDescription = stringResource(R.string.app_name),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(15.dp),
          )
          Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = greeting,
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = stringResource(R.string.home_tagline),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Surface(
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surfaceContainerLow,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
      IconButton(
        onClick = onToggleTheme,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("theme_toggle_button"),
      ) {
        Icon(
          imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
          contentDescription = if (isDarkTheme) "Switch to light theme" else "Switch to dark theme",
          tint = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

@Composable
private fun StartFocusCtaButton(
  uiState: HomeUiState,
  onClick: () -> Unit,
) {
  val isRunningOrPaused = uiState.timerState.isActive
  val ctaLabel =
    when (uiState.timerState.phase) {
      TimerPhase.RUNNING ->
        if (uiState.timerState.isStrictLocked) {
          "Locked (${uiState.timerState.mode.title}) · ${FocusTimerService.formatCountdown(uiState.timerState.remainingSeconds)}"
        } else {
          "Focusing (${uiState.timerState.mode.title}) · ${FocusTimerService.formatCountdown(uiState.timerState.remainingSeconds)}"
        }
      TimerPhase.PAUSED ->
        "Paused (${uiState.timerState.mode.title}) · ${FocusTimerService.formatCountdown(uiState.timerState.remainingSeconds)}"
      else ->
        "Start ${uiState.activeFocusMode.title} · ${uiState.selectedDurationMinutes}m"
    }

  Button(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("start_focus_button"),
    shape = MaterialTheme.shapes.medium,
    contentPadding = PaddingValues(horizontal = 20.dp),
    colors =
      ButtonDefaults.buttonColors(
        containerColor =
          if (isRunningOrPaused) MaterialTheme.colorScheme.secondary
          else MaterialTheme.colorScheme.primary,
        contentColor =
          if (isRunningOrPaused) MaterialTheme.colorScheme.onSecondary
          else MaterialTheme.colorScheme.onPrimary,
      ),
  ) {
    Icon(
      imageVector = if (isRunningOrPaused) Icons.Rounded.Timer else Icons.Rounded.PlayArrow,
      contentDescription = stringResource(R.string.home_start_focus),
      modifier = Modifier.size(22.dp),
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = ctaLabel,
      style = MaterialTheme.typography.labelLarge,
    )
  }
}

@Composable
private fun DailyGoalSheetContent(
  initialMinutes: Int,
  onSave: (Int) -> Unit,
  onCancel: () -> Unit,
) {
  var draftMinutes by remember(initialMinutes) { mutableIntStateOf(initialMinutes) }
  val presets = listOf(60, 90, 120, 180)

  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Text(
      text = stringResource(R.string.home_goal_sheet_title),
      style = MaterialTheme.typography.headlineSmall,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
      text = stringResource(R.string.home_goal_sheet_subtitle),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Text(
      text = formatStudyDuration(draftMinutes),
      style = MetricNumericStyle,
      color = MaterialTheme.colorScheme.primary,
    )

    Slider(
      value = draftMinutes.toFloat(),
      onValueChange = { raw ->
        draftMinutes = ((raw / 15f).roundToInt() * 15).coerceIn(30, 360)
      },
      valueRange = 30f..360f,
      modifier = Modifier.fillMaxWidth().testTag("daily_goal_slider"),
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      presets.forEach { preset ->
        FilterChip(
          selected = draftMinutes == preset,
          onClick = { draftMinutes = preset },
          label = { Text(formatStudyDuration(preset)) },
          modifier = Modifier.weight(1f).testTag("goal_preset_$preset"),
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.End,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TextButton(
        onClick = onCancel,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("goal_cancel_button"),
      ) {
        Text("Cancel")
      }
      Spacer(modifier = Modifier.width(8.dp))
      Button(
        onClick = { onSave(draftMinutes) },
        modifier = Modifier.minimumInteractiveComponentSize().testTag("goal_save_button"),
      ) {
        Text("Save Goal")
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}

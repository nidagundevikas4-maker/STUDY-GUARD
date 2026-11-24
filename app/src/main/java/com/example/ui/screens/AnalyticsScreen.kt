package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.FocusSessionSummary
import com.example.model.SessionOutcome
import com.example.ui.components.FocusModeBreakdownCard
import com.example.ui.components.FocusTrendChartCard
import com.example.ui.components.MilestoneBadgesCard
import com.example.ui.components.StudyLevelCard
import com.example.ui.components.formatStudyDuration
import com.example.ui.theme.MetricNumericStyle
import com.example.ui.viewmodel.AnalyticsPeriod
import com.example.ui.viewmodel.AnalyticsUiState
import com.example.ui.viewmodel.TopDistractingApp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(
  uiState: AnalyticsUiState,
  onPeriodSelected: (AnalyticsPeriod) -> Unit,
  onDeleteSession: (Long) -> Unit,
  onClearHistory: () -> Unit,
  onOpenWeeklyTrends: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  var showClearDialog by remember { mutableStateOf(false) }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 600.dp).fillMaxSize().testTag("analytics_list"),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top,
        ) {
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = "Analytics",
              style = MaterialTheme.typography.displaySmall,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
              text = "Private on-device study trends & distraction insights.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.LocalFireDepartment,
                contentDescription = "Study streak",
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = "${uiState.streakDays}d streak",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
              )
            }
          }
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          AnalyticsPeriod.entries.forEach { period ->
            FilterChip(
              selected = uiState.selectedPeriod == period,
              onClick = { onPeriodSelected(period) },
              label = { Text(period.label) },
              modifier =
                Modifier
                  .weight(1f)
                  .minimumInteractiveComponentSize()
                  .testTag("analytics_period_${period.name.lowercase()}"),
            )
          }
        }
      }

      item {
        WeeklyTrendsEntryCard(onClick = onOpenWeeklyTrends)
      }

      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            AnalyticsStatTile(
              value = formatStudyDuration(uiState.focusMinutes),
              label = "Focus Time",
              modifier = Modifier.weight(1f),
            )
            AnalyticsStatTile(
              value = "${uiState.completionRatePercent}%",
              label = "Completion Rate",
              modifier = Modifier.weight(1f),
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            AnalyticsStatTile(
              value = uiState.completedCount.toString(),
              label = "Completed",
              modifier = Modifier.weight(1f),
            )
            AnalyticsStatTile(
              value = uiState.dismissedNotificationsCount.toString(),
              label = "Alerts Silenced",
              modifier = Modifier.weight(1f),
            )
            AnalyticsStatTile(
              value = uiState.blockedAppAttemptsCount.toString(),
              label = "Apps Blocked",
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      item {
        StudyLevelCard(
          level = uiState.milestoneSummary.level,
          unlockedBadgesCount = uiState.milestoneSummary.unlockedCount,
          totalBadgesCount = uiState.milestoneSummary.badges.size,
        )
      }

      item {
        MilestoneBadgesCard(badges = uiState.milestoneSummary.badges)
      }

      item {
        FocusTrendChartCard(bars = uiState.weeklyTrend)
      }

      item {
        FocusModeBreakdownCard(breakdown = uiState.modeBreakdown)
      }

      item {
        TopDistractorsCard(apps = uiState.topDistractingApps)
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Session History (${uiState.selectedPeriod.label})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          if (uiState.sessions.isNotEmpty()) {
            TextButton(
              onClick = { showClearDialog = true },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("clear_history_button"),
              colors =
                ButtonDefaults.textButtonColors(
                  contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
              Text("Clear All")
            }
          }
        }
      }

      if (uiState.sessions.isEmpty()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth().testTag("analytics_empty_state_card"),
            shape = MaterialTheme.shapes.large,
            colors =
              CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
              ) {
                Icon(
                  imageVector = Icons.Outlined.BarChart,
                  contentDescription = "No study sessions logged yet",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(14.dp).size(28.dp),
                )
              }
              Text(
                text = "No sessions in this timeframe",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text =
                  "Complete a focus session to populate your daily trend chart, mode breakdown, and local study log.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        items(uiState.sessions, key = { it.id }) { session ->
          SessionHistoryCard(
            session = session,
            onDelete = { onDeleteSession(session.id) },
          )
        }
      }
    }
  }

  if (showClearDialog) {
    AlertDialog(
      onDismissRequest = { showClearDialog = false },
      title = { Text("Clear Study History?") },
      text = {
        Text("This permanently deletes all saved focus sessions and local distraction counts from this device.")
      },
      confirmButton = {
        TextButton(
          onClick = {
            onClearHistory()
            showClearDialog = false
          },
          colors =
            ButtonDefaults.textButtonColors(
              contentColor = MaterialTheme.colorScheme.error,
            ),
          modifier = Modifier.testTag("confirm_clear_history_button"),
        ) {
          Text("Clear All")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearDialog = false }) {
          Text("Cancel")
        }
      },
    )
  }
}

@Composable
private fun WeeklyTrendsEntryCard(
  onClick: () -> Unit,
) {
  Card(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("open_weekly_trends_button"),
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primary,
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ShowChart,
          contentDescription = "Weekly Study and Productivity Trends",
          tint = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.padding(10.dp).size(20.dp),
        )
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = "Weekly Study & Productivity Trends",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = "Interactive multi-week study hours & productivity score charts",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
        contentDescription = "Open Weekly Trends",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

@Composable
private fun TopDistractorsCard(apps: List<TopDistractingApp>) {
  Card(
    modifier = Modifier.fillMaxWidth().testTag("top_distractors_card"),
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "Most Frequent Distractions",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )

      if (apps.isEmpty()) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Outlined.NotificationsOff,
            contentDescription = "No distractions recorded",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
          )
          Text(
            text =
              "No silenced notifications recorded in this period. Counts appear locally as Study Guard filters alerts during sessions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        apps.forEach { app ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = app.appLabel,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.weight(1f),
            )
            Surface(
              shape = MaterialTheme.shapes.small,
              color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
              Text(
                text = "${app.dismissedCount} blocked",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SessionHistoryCard(
  session: FocusSessionSummary,
  onDelete: () -> Unit,
) {
  val isCompleted = session.outcome == SessionOutcome.COMPLETED
  val dateFormatter = remember { SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()) }
  val formattedDate = remember(session.completedAtEpochMs) {
    dateFormatter.format(Date(session.completedAtEpochMs))
  }
  val focusedMinutes = (session.elapsedSeconds / 60).coerceAtLeast(1)

  Card(
    modifier = Modifier.fillMaxWidth().testTag("session_history_card_${session.id}"),
    shape = MaterialTheme.shapes.medium,
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = if (isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.PauseCircle,
        contentDescription = session.outcome.name,
        tint =
          if (isCompleted) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.size(22.dp),
      )

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "${session.mode.title} · ${focusedMinutes}m",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text =
            "${if (isCompleted) "Completed" else "Interrupted"} · $formattedDate",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      IconButton(
        onClick = onDelete,
        modifier =
          Modifier
            .minimumInteractiveComponentSize()
            .testTag("delete_session_${session.id}"),
      ) {
        Icon(
          imageVector = Icons.Outlined.DeleteOutline,
          contentDescription = "Delete session",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun AnalyticsStatTile(
  value: String,
  label: String,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    shape = MaterialTheme.shapes.medium,
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Text(
        text = value,
        style = MetricNumericStyle,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

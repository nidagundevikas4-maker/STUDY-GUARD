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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.ProductivityTrendChartCard
import com.example.ui.components.WeeklyStudyTimeChartCard
import com.example.ui.components.formatStudyDuration
import com.example.ui.theme.MetricNumericStyle
import com.example.ui.viewmodel.TrendsRangeFilter
import com.example.ui.viewmodel.WeeklyTrendsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyTrendsScreen(
  uiState: WeeklyTrendsUiState,
  onRangeSelected: (TrendsRangeFilter) -> Unit,
  onSelectBucket: (Int) -> Unit,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize()) {
    TopAppBar(
      title = {
        Column {
          Text(
            text = "Weekly Study Trends",
            style = MaterialTheme.typography.titleLarge,
          )
          Text(
            text = "Study Guard productivity history",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      },
      navigationIcon = {
        IconButton(
          onClick = onBackClick,
          modifier =
            Modifier
              .minimumInteractiveComponentSize()
              .testTag("weekly_trends_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back to previous screen",
          )
        }
      },
      colors =
        TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
          titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )

    Box(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier.widthIn(max = 640.dp).fillMaxSize().testTag("weekly_trends_list"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            TrendsRangeFilter.entries.forEach { range ->
              FilterChip(
                selected = uiState.selectedRange == range,
                onClick = { onRangeSelected(range) },
                label = { Text(range.label) },
                modifier =
                  Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("trends_range_${range.name.lowercase()}"),
              )
            }
          }
        }

        if (!uiState.hasHistoricalData) {
          item {
            TrendsEmptyStateCard()
          }
        }

        item {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              TrendKpiCard(
                icon = Icons.Rounded.Timer,
                label = "Total Study Time",
                value = formatStudyDuration(uiState.totalPeriodMinutes),
                badgeText =
                  uiState.studyTimeDeltaPercent?.let { delta ->
                    if (delta >= 0) "+$delta% vs prev" else "$delta% vs prev"
                  },
                isPositiveDelta = (uiState.studyTimeDeltaPercent ?: 0) >= 0,
                modifier = Modifier.weight(1f).testTag("kpi_total_study_time"),
              )
              TrendKpiCard(
                icon = Icons.Rounded.AutoGraph,
                label = "Avg Productivity",
                value = "${uiState.averageProductivityScore}/100",
                badgeText =
                  uiState.productivityDeltaPoints?.let { pts ->
                    if (pts >= 0) "+$pts pts" else "$pts pts"
                  },
                isPositiveDelta = (uiState.productivityDeltaPoints ?: 0) >= 0,
                modifier = Modifier.weight(1f).testTag("kpi_avg_productivity"),
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              TrendKpiCard(
                icon = Icons.Rounded.CalendarMonth,
                label = "Peak Study Day",
                value = uiState.peakFocusDayLabel,
                badgeText = null,
                isPositiveDelta = true,
                modifier = Modifier.weight(1f).testTag("kpi_peak_day"),
              )
              TrendKpiCard(
                icon = Icons.AutoMirrored.Rounded.TrendingUp,
                label = "Avg Session",
                value =
                  if (uiState.averageSessionMinutes > 0) "${uiState.averageSessionMinutes}m"
                  else "0m",
                badgeText = "${uiState.totalCompletedSessions} done",
                isPositiveDelta = true,
                modifier = Modifier.weight(1f).testTag("kpi_avg_session"),
              )
            }
          }
        }

        item {
          WeeklyStudyTimeChartCard(
            buckets = uiState.buckets,
            selectedIndex = uiState.selectedBucketIndex,
            rangeFilter = uiState.selectedRange,
            onSelectBucket = onSelectBucket,
          )
        }

        item {
          ProductivityTrendChartCard(
            buckets = uiState.buckets,
            selectedIndex = uiState.selectedBucketIndex,
            onSelectBucket = onSelectBucket,
          )
        }

        item {
          ProductivityMethodologyCard(
            dailyGoalMinutes = uiState.dailyGoalMinutes,
            totalCompletedSessions = uiState.totalCompletedSessions,
            totalDistractionsPrevented = uiState.totalDistractionsPrevented,
          )
        }
      }
    }
  }
}

@Composable
private fun TrendKpiCard(
  icon: ImageVector,
  label: String,
  value: String,
  badgeText: String?,
  isPositiveDelta: Boolean,
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
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
        if (!badgeText.isNullOrBlank()) {
          Surface(
            shape = MaterialTheme.shapes.small,
            color =
              if (isPositiveDelta) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
              else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
          ) {
            Text(
              text = badgeText,
              style = MaterialTheme.typography.labelSmall,
              color =
                if (isPositiveDelta) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            )
          }
        }
      }
      Text(
        text = value,
        style = MetricNumericStyle,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun TrendsEmptyStateCard() {
  Card(
    modifier = Modifier.fillMaxWidth().testTag("weekly_trends_empty_card"),
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
      ) {
        Icon(
          imageVector = Icons.Outlined.Insights,
          contentDescription = "No study history in this window",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(12.dp).size(24.dp),
        )
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "No historical study sessions yet",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text =
            "Complete focus sessions in Study Guard to populate multi-week study hours and your 0–100 productivity score curve.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun ProductivityMethodologyCard(
  dailyGoalMinutes: Int,
  totalCompletedSessions: Int,
  totalDistractionsPrevented: Int,
) {
  Card(
    modifier = Modifier.fillMaxWidth().testTag("productivity_methodology_card"),
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = "Study Habit Summary",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        HabitRowItem(
          icon = Icons.Rounded.CheckCircle,
          label = "Completed sessions",
          value = totalCompletedSessions.toString(),
        )
        HabitRowItem(
          icon = Icons.Outlined.Shield,
          label = "Distractions shielded",
          value = totalDistractionsPrevented.toString(),
        )
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      Text(
        text =
          "Productivity Score (0–100) combines 50% session completion rate, 35% progress toward your ${formatStudyDuration(dailyGoalMinutes)}/day target, and 15% Deep/Study focus depth — computed strictly on-device from your Study Guard database.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(2.dp))
    }
  }
}

@Composable
private fun HabitRowItem(
  icon: ImageVector,
  label: String,
  value: String,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(18.dp),
    )
    Column {
      Text(
        text = value,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

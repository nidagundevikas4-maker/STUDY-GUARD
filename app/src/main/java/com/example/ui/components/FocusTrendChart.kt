package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.DailyFocusBar
import com.example.ui.viewmodel.ModeBreakdownItem

@Composable
fun FocusTrendChartCard(
  bars: List<DailyFocusBar>,
  modifier: Modifier = Modifier,
) {
  val maxMinutes = (bars.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(30)
  val totalWeekMinutes = bars.sumOf { it.minutes }

  Card(
    modifier = modifier.fillMaxWidth().testTag("weekly_trend_chart_card"),
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "7-Day Focus Trend",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "Daily study minutes over the past week",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Surface(
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          Text(
            text = formatStudyDuration(totalWeekMinutes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
      ) {
        bars.forEach { bar ->
          val targetFraction = (bar.minutes.toFloat() / maxMinutes.toFloat()).coerceIn(0f, 1f)
          val animatedFraction by
            animateFloatAsState(
              targetValue = targetFraction,
              animationSpec = spring(stiffness = Spring.StiffnessLow),
              label = "bar_${bar.dayLabel}",
            )

          val barColor =
            if (bar.isToday) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f)
          val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)

          Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
          ) {
            Text(
              text = if (bar.minutes > 0) "${bar.minutes}m" else "",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
              val barWidth = size.width * 0.52f
              val left = (size.width - barWidth) / 2f
              val corner = CornerRadius(8.dp.toPx(), 8.dp.toPx())

              drawRoundRect(
                color = trackColor,
                topLeft = Offset(left, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = corner,
              )

              if (animatedFraction > 0f) {
                val fillHeight = (size.height * animatedFraction).coerceAtLeast(6.dp.toPx())
                drawRoundRect(
                  color = barColor,
                  topLeft = Offset(left, size.height - fillHeight),
                  size = Size(barWidth, fillHeight),
                  cornerRadius = corner,
                )
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = bar.dayLabel,
              style = MaterialTheme.typography.labelSmall,
              color =
                if (bar.isToday) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@Composable
fun FocusModeBreakdownCard(
  breakdown: List<ModeBreakdownItem>,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth().testTag("mode_breakdown_card"),
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
        text = "Focus Mode Breakdown",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )

      breakdown.forEach { item ->
        val animatedFraction by
          animateFloatAsState(
            targetValue = item.fraction,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "mode_bar_${item.mode.name}",
          )

        val trackColor = MaterialTheme.colorScheme.surfaceVariant
        val fillColor = MaterialTheme.colorScheme.primary

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = item.mode.title,
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "${formatStudyDuration(item.minutes)} · ${item.sessionCount} sessions",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
            val radius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            drawRoundRect(
              color = trackColor,
              size = size,
              cornerRadius = radius,
            )
            if (animatedFraction > 0f) {
              drawRoundRect(
                color = fillColor,
                size = Size((size.width * animatedFraction).coerceAtLeast(8.dp.toPx()), size.height),
                cornerRadius = radius,
              )
            }
          }
        }
      }
    }
  }
}

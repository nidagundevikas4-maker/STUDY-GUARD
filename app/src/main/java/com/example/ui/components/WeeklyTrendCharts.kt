package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.TrendsRangeFilter
import com.example.ui.viewmodel.WeeklyTrendBucket
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun WeeklyStudyTimeChartCard(
  buckets: List<WeeklyTrendBucket>,
  selectedIndex: Int,
  rangeFilter: TrendsRangeFilter,
  onSelectBucket: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val safeSelectedIndex =
    if (buckets.isEmpty()) 0 else selectedIndex.coerceIn(0, buckets.lastIndex)
  val selectedBucket = buckets.getOrNull(safeSelectedIndex)
  val goalTarget = selectedBucket?.goalTargetMinutes?.coerceAtLeast(30) ?: 120
  val maxMinutes =
    maxOf(
      buckets.maxOfOrNull { it.studyMinutes } ?: 0,
      goalTarget,
      60,
    )
  val goalFraction = (goalTarget.toFloat() / maxMinutes.toFloat()).coerceIn(0.05f, 1f)
  val totalMinutes = buckets.sumOf { it.studyMinutes }

  val primaryColor = MaterialTheme.colorScheme.primary
  val secondaryBarColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.58f)
  val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
  val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
  val goalLineColor = MaterialTheme.colorScheme.tertiary

  val animatedFractions =
    buckets.mapIndexed { idx, bucket ->
      val target = (bucket.studyMinutes.toFloat() / maxMinutes.toFloat()).coerceIn(0f, 1f)
      animateFloatAsState(
        targetValue = target,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "study_bar_${rangeFilter.name}_$idx",
      ).value
    }

  Card(
    modifier = modifier.fillMaxWidth().testTag("weekly_study_time_chart_card"),
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
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text =
              if (rangeFilter == TrendsRangeFilter.THIS_WEEK_DAILY) "Daily Study Time"
              else "Weekly Study Time",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "Tap a bar to inspect study hours & goal pace",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          Text(
            text = formatStudyDuration(totalMinutes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      if (selectedBucket != null) {
        val goalPct =
          ((selectedBucket.studyMinutes * 100f) / selectedBucket.goalTargetMinutes.coerceAtLeast(1))
            .roundToInt()
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surfaceContainerLowest,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth().testTag("study_chart_selection_pill"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text = selectedBucket.rangeSubtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text =
                  "${selectedBucket.completedSessions} completed · ${selectedBucket.distractionsPrevented} shielded",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = formatStudyDuration(selectedBucket.studyMinutes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "$goalPct% of ${formatStudyDuration(selectedBucket.goalTargetMinutes)} goal",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().height(176.dp),
        verticalAlignment = Alignment.Bottom,
      ) {
        Column(
          modifier = Modifier.width(38.dp).height(176.dp),
          verticalArrangement = Arrangement.SpaceBetween,
          horizontalAlignment = Alignment.End,
        ) {
          Text(
            text = formatCompactAxisDuration(maxMinutes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = formatCompactAxisDuration(maxMinutes / 2),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "0m",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Canvas(
          modifier =
            Modifier
              .weight(1f)
              .height(176.dp)
              .semantics {
                contentDescription = "Weekly study time bar chart showing ${buckets.size} periods"
              }
              .pointerInput(buckets) {
                detectTapGestures { offset ->
                  if (buckets.isNotEmpty() && size.width > 0) {
                    val slotWidth = size.width.toFloat() / buckets.size.toFloat()
                    val tapped = (offset.x / slotWidth).toInt().coerceIn(0, buckets.lastIndex)
                    onSelectBucket(tapped)
                  }
                }
              },
        ) {
          val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
          // Horizontal reference grid lines (top, mid, baseline)
          listOf(0f, 0.5f, 1f).forEach { ratio ->
            val y = size.height * ratio
            drawLine(
              color = gridLineColor,
              start = Offset(0f, y),
              end = Offset(size.width, y),
              strokeWidth = 1.dp.toPx(),
              pathEffect = if (ratio < 1f) dashEffect else null,
            )
          }

          if (buckets.isNotEmpty()) {
            val slotWidth = size.width / buckets.size.toFloat()
            val barWidth = (slotWidth * 0.54f).coerceIn(10.dp.toPx(), 34.dp.toPx())
            val corner = CornerRadius(7.dp.toPx(), 7.dp.toPx())

            buckets.forEachIndexed { idx, bucket ->
              val centerX = slotWidth * idx + slotWidth / 2f
              val left = centerX - barWidth / 2f
              val isSelected = idx == safeSelectedIndex

              drawRoundRect(
                color = trackColor,
                topLeft = Offset(left, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = corner,
              )

              val fraction = animatedFractions.getOrElse(idx) { 0f }
              if (fraction > 0f) {
                val barHeight = (size.height * fraction).coerceAtLeast(6.dp.toPx())
                val barColor =
                  when {
                    isSelected -> primaryColor
                    bucket.isCurrentPeriod -> primaryColor.copy(alpha = 0.85f)
                    else -> secondaryBarColor
                  }
                drawRoundRect(
                  color = barColor,
                  topLeft = Offset(left, size.height - barHeight),
                  size = Size(barWidth, barHeight),
                  cornerRadius = corner,
                )
              }

              if (isSelected) {
                drawRoundRect(
                  color = primaryColor,
                  topLeft = Offset(left - 2.dp.toPx(), -2.dp.toPx()),
                  size = Size(barWidth + 4.dp.toPx(), size.height + 4.dp.toPx()),
                  cornerRadius = CornerRadius(9.dp.toPx(), 9.dp.toPx()),
                  style = Stroke(width = 1.5.dp.toPx()),
                )
              }
            }
          }

          // Goal benchmark dashed line
          val goalY = size.height * (1f - goalFraction)
          drawLine(
            color = goalLineColor,
            start = Offset(0f, goalY),
            end = Offset(size.width, goalY),
            strokeWidth = 1.75.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
          )
        }
      }

      // X-axis bucket labels
      if (buckets.isNotEmpty()) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(start = 46.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          buckets.forEachIndexed { idx, bucket ->
            val isSelected = idx == safeSelectedIndex
            Text(
              text = bucket.label,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              color =
                when {
                  isSelected -> MaterialTheme.colorScheme.primary
                  bucket.isCurrentPeriod -> MaterialTheme.colorScheme.onSurface
                  else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
              textAlign = TextAlign.Center,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      // Chart legend
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ChartLegendItem(
          color = primaryColor,
          label = "Selected / Current",
        )
        ChartLegendItem(
          color = goalLineColor,
          label = "Target Goal (${formatStudyDuration(goalTarget)})",
          isDashed = true,
        )
      }
    }
  }
}

@Composable
fun ProductivityTrendChartCard(
  buckets: List<WeeklyTrendBucket>,
  selectedIndex: Int,
  onSelectBucket: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val safeSelectedIndex =
    if (buckets.isEmpty()) 0 else selectedIndex.coerceIn(0, buckets.lastIndex)
  val selectedBucket = buckets.getOrNull(safeSelectedIndex)

  val primaryColor = MaterialTheme.colorScheme.primary
  val secondaryColor = MaterialTheme.colorScheme.secondary
  val surfaceContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
  val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)

  val animatedScores =
    buckets.mapIndexed { idx, bucket ->
      animateFloatAsState(
        targetValue = (bucket.productivityScore / 100f).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "prod_score_$idx",
      ).value
    }
  val animatedCompletion =
    buckets.mapIndexed { idx, bucket ->
      animateFloatAsState(
        targetValue = (bucket.completionRatePercent / 100f).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "comp_rate_$idx",
      ).value
    }

  Card(
    modifier = modifier.fillMaxWidth().testTag("productivity_trend_chart_card"),
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(
        containerColor = surfaceContainerColor,
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Productivity Score Trend",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "0–100 score from completion rate, goal pace & focus depth",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (selectedBucket != null) {
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.secondaryContainer,
          ) {
            Text(
              text = "${selectedBucket.productivityScore} / 100",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }
      }

      if (selectedBucket != null) {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surfaceContainerLowest,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth().testTag("productivity_chart_selection_pill"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = selectedBucket.rangeSubtitle,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text =
                "Score ${selectedBucket.productivityScore} · ${selectedBucket.completionRatePercent}% completion",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().height(176.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(
          modifier = Modifier.width(32.dp).height(176.dp),
          verticalArrangement = Arrangement.SpaceBetween,
          horizontalAlignment = Alignment.End,
        ) {
          Text(
            text = "100",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "50",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "0",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Canvas(
          modifier =
            Modifier
              .weight(1f)
              .height(176.dp)
              .semantics {
                contentDescription = "Productivity score line and area chart"
              }
              .pointerInput(buckets) {
                detectTapGestures { offset ->
                  if (buckets.isNotEmpty() && size.width > 0) {
                    val slotWidth = size.width.toFloat() / buckets.size.toFloat()
                    val tapped = (offset.x / slotWidth).toInt().coerceIn(0, buckets.lastIndex)
                    onSelectBucket(tapped)
                  }
                }
              },
        ) {
          val verticalPadding = 10.dp.toPx()
          val chartHeight = (size.height - verticalPadding * 2f).coerceAtLeast(1f)
          val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)

          listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { fraction ->
            val y = verticalPadding + chartHeight * (1f - fraction)
            drawLine(
              color = gridLineColor,
              start = Offset(0f, y),
              end = Offset(size.width, y),
              strokeWidth = 1.dp.toPx(),
              pathEffect = if (fraction > 0f) dashEffect else null,
            )
          }

          if (buckets.isEmpty()) return@Canvas

          val slotWidth = size.width / buckets.size.toFloat()
          val scorePoints =
            animatedScores.mapIndexed { idx, fraction ->
              Offset(
                x = slotWidth * idx + slotWidth / 2f,
                y = verticalPadding + chartHeight * (1f - fraction),
              )
            }
          val completionPoints =
            animatedCompletion.mapIndexed { idx, fraction ->
              Offset(
                x = slotWidth * idx + slotWidth / 2f,
                y = verticalPadding + chartHeight * (1f - fraction),
              )
            }

          // Vertical guide line for selected point
          scorePoints.getOrNull(safeSelectedIndex)?.let { selectedPt ->
            drawLine(
              color = primaryColor.copy(alpha = 0.28f),
              start = Offset(selectedPt.x, verticalPadding),
              end = Offset(selectedPt.x, verticalPadding + chartHeight),
              strokeWidth = 1.5.dp.toPx(),
              pathEffect = dashEffect,
            )
          }

          // Secondary dashed line: Completion Rate
          if (completionPoints.size >= 2) {
            val completionPath = buildSmoothPath(completionPoints)
            drawPath(
              path = completionPath,
              color = secondaryColor.copy(alpha = 0.75f),
              style =
                Stroke(
                  width = 2.dp.toPx(),
                  cap = StrokeCap.Round,
                  join = StrokeJoin.Round,
                  pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 7f), 0f),
                ),
            )
          }

          // Area fill + smooth line: Productivity Score
          if (scorePoints.size >= 2) {
            val linePath = buildSmoothPath(scorePoints)
            val areaPath =
              Path().apply {
                addPath(linePath)
                lineTo(scorePoints.last().x, verticalPadding + chartHeight)
                lineTo(scorePoints.first().x, verticalPadding + chartHeight)
                close()
              }

            drawPath(
              path = areaPath,
              brush =
                Brush.verticalGradient(
                  colors =
                    listOf(
                      primaryColor.copy(alpha = 0.28f),
                      primaryColor.copy(alpha = 0.02f),
                    ),
                  startY = verticalPadding,
                  endY = verticalPadding + chartHeight,
                ),
            )

            drawPath(
              path = linePath,
              color = primaryColor,
              style =
                Stroke(
                  width = 3.dp.toPx(),
                  cap = StrokeCap.Round,
                  join = StrokeJoin.Round,
                ),
            )
          }

          // Draw data point nodes
          scorePoints.forEachIndexed { idx, pt ->
            val isSelected = idx == safeSelectedIndex
            if (isSelected) {
              drawCircle(
                color = primaryColor.copy(alpha = 0.22f),
                radius = 10.dp.toPx(),
                center = pt,
              )
            }
            drawCircle(
              color = surfaceContainerColor,
              radius = if (isSelected) 5.5.dp.toPx() else 4.dp.toPx(),
              center = pt,
            )
            drawCircle(
              color = primaryColor,
              radius = if (isSelected) 4.dp.toPx() else 2.75.dp.toPx(),
              center = pt,
            )
          }
        }
      }

      // X-axis bucket labels
      if (buckets.isNotEmpty()) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(start = 40.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          buckets.forEachIndexed { idx, bucket ->
            val isSelected = idx == safeSelectedIndex
            Text(
              text = bucket.label,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              color =
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      // Legend
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ChartLegendItem(
          color = primaryColor,
          label = "Productivity Score",
        )
        ChartLegendItem(
          color = secondaryColor,
          label = "Completion Rate",
          isDashed = true,
        )
      }
    }
  }
}

@Composable
private fun ChartLegendItem(
  color: Color,
  label: String,
  isDashed: Boolean = false,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    if (isDashed) {
      Canvas(modifier = Modifier.width(16.dp).height(4.dp)) {
        drawLine(
          color = color,
          start = Offset(0f, size.height / 2f),
          end = Offset(size.width, size.height / 2f),
          strokeWidth = 2.dp.toPx(),
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f),
        )
      }
    } else {
      Box(
        modifier = Modifier.size(9.dp).clip(CircleShape).background(color),
      )
    }
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

private fun buildSmoothPath(points: List<Offset>): Path =
  Path().apply {
    if (points.isEmpty()) return@apply
    moveTo(points.first().x, points.first().y)
    for (i in 0 until points.lastIndex) {
      val current = points[i]
      val next = points[i + 1]
      val midX = (current.x + next.x) / 2f
      cubicTo(
        midX,
        current.y,
        midX,
        next.y,
        next.x,
        next.y,
      )
    }
  }

private fun formatCompactAxisDuration(minutes: Int): String =
  when {
    minutes <= 0 -> "0m"
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> String.format(Locale.US, "%.1fh", minutes / 60f)
  }

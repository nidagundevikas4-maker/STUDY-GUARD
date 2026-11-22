package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.FocusSessionSummary
import com.example.model.SessionOutcome
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DailyTimelineSection(
  sessions: List<FocusSessionSummary>,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Text(
      text = stringResource(R.string.home_timeline_title),
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onBackground,
    )

    if (sessions.isEmpty()) {
      Card(
        modifier = Modifier.fillMaxWidth().testTag("daily_timeline_empty_card"),
        shape = MaterialTheme.shapes.medium,
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
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
          ) {
            Icon(
              imageVector = Icons.Outlined.HistoryEdu,
              contentDescription = stringResource(R.string.home_timeline_empty_title),
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(12.dp).size(24.dp),
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.home_timeline_empty_title),
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Start a focus session above to build today's study log.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      sessions.take(5).forEach { session ->
        TimelineSessionRow(session = session)
      }
    }
  }
}

@Composable
private fun TimelineSessionRow(session: FocusSessionSummary) {
  val isCompleted = session.outcome == SessionOutcome.COMPLETED
  val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
  val formattedTime = remember(session.completedAtEpochMs) {
    timeFormatter.format(Date(session.completedAtEpochMs))
  }
  val minutesFocused = (session.elapsedSeconds / 60).coerceAtLeast(1)

  Card(
    modifier = Modifier.fillMaxWidth().testTag("timeline_session_${session.id}"),
    shape = MaterialTheme.shapes.medium,
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
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
          text = session.mode.title,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text =
            if (isCompleted) "Completed · $formattedTime"
            else "Ended early · $formattedTime",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Surface(
        shape = MaterialTheme.shapes.small,
        color =
          if (isCompleted) MaterialTheme.colorScheme.primaryContainer
          else MaterialTheme.colorScheme.tertiaryContainer,
      ) {
        Text(
          text = "${minutesFocused}m",
          style = MaterialTheme.typography.labelMedium,
          color =
            if (isCompleted) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onTertiaryContainer,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }
    }
  }
}

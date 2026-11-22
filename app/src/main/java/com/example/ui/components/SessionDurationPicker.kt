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
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R

private val DurationPresets = listOf(15, 25, 45, 60)

@Composable
fun SessionDurationPicker(
  selectedDurationMinutes: Int,
  onDurationSelected: (Int) -> Unit,
  modifier: Modifier = Modifier,
  isLocked: Boolean = false,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.home_duration_label),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        if (isLocked) {
          Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = "Duration locked during active session",
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(14.dp),
          )
        }
        Text(
          text = if (isLocked) "$selectedDurationMinutes min · Locked" else "$selectedDurationMinutes min",
          style = MaterialTheme.typography.labelLarge,
          color = if (isLocked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      DurationPresets.forEach { minutes ->
        val selected = minutes == selectedDurationMinutes
        Surface(
          onClick = { onDurationSelected(minutes) },
          enabled = !isLocked,
          modifier =
            Modifier
              .weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("duration_chip_$minutes"),
          shape = MaterialTheme.shapes.small,
          color =
            when {
              selected -> MaterialTheme.colorScheme.secondaryContainer
              isLocked -> MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.55f)
              else -> MaterialTheme.colorScheme.surfaceContainerLow
            },
          border =
            BorderStroke(
              width = if (selected) 1.5.dp else 1.dp,
              color =
                when {
                  selected -> MaterialTheme.colorScheme.secondary
                  isLocked -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                  else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                },
            ),
        ) {
          Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(
              text = "${minutes}m",
              style = MaterialTheme.typography.labelLarge,
              color =
                when {
                  selected -> MaterialTheme.colorScheme.onSecondaryContainer
                  isLocked -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                  else -> MaterialTheme.colorScheme.onSurface
                },
            )
          }
        }
      }
    }
  }
}

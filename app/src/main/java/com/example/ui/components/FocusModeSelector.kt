package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.FocusMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusModeSelector(
  activeMode: FocusMode,
  onModeSelected: (FocusMode) -> Unit,
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
        text = stringResource(R.string.home_current_mode_label),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Surface(
        shape = MaterialTheme.shapes.small,
        color =
          if (isLocked) MaterialTheme.colorScheme.tertiaryContainer
          else MaterialTheme.colorScheme.primaryContainer,
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          if (isLocked) {
            Icon(
              imageVector = Icons.Rounded.Lock,
              contentDescription = "Mode locked during active session",
              tint = MaterialTheme.colorScheme.onTertiaryContainer,
              modifier = Modifier.size(13.dp),
            )
          }
          Text(
            text = if (isLocked) "Locked" else "${activeMode.defaultDurationMinutes} min preset",
            style = MaterialTheme.typography.labelMedium,
            color =
              if (isLocked) MaterialTheme.colorScheme.onTertiaryContainer
              else MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      FocusMode.entries.forEach { mode ->
        FocusModeChipCard(
          mode = mode,
          selected = mode == activeMode,
          enabled = !isLocked,
          onClick = { onModeSelected(mode) },
          modifier = Modifier.weight(1f),
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Card(
      modifier = Modifier.fillMaxWidth().testTag("active_mode_summary_card"),
      shape = MaterialTheme.shapes.medium,
      colors =
        CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
      AnimatedContent(
        targetState = activeMode,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "active_mode_details",
      ) { mode ->
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              shape = MaterialTheme.shapes.small,
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            ) {
              Icon(
                imageVector = modeIcon(mode),
                contentDescription = mode.title,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(10.dp).size(20.dp),
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${mode.title} · ${mode.subtitle}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = mode.policySummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            RuleSummaryBadge(
              icon = Icons.Rounded.CheckCircleOutline,
              text = "Allow: ${mode.allowedRuleLabel}",
              isAllowed = true,
            )
            RuleSummaryBadge(
              icon = Icons.Rounded.NotificationsOff,
              text = "Silence: ${mode.silencedRuleLabel}",
              isAllowed = false,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun RuleSummaryBadge(
  icon: ImageVector,
  text: String,
  isAllowed: Boolean,
) {
  val container =
    if (isAllowed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
  val content =
    if (isAllowed) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSecondaryContainer

  Surface(
    shape = MaterialTheme.shapes.small,
    color = container,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = content,
        modifier = Modifier.size(14.dp),
      )
      Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = content,
      )
    }
  }
}

@Composable
private fun FocusModeChipCard(
  mode: FocusMode,
  selected: Boolean,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val containerColor by
    animateColorAsState(
      targetValue =
        when {
          selected -> MaterialTheme.colorScheme.primaryContainer
          !enabled -> MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.55f)
          else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      label = "mode_chip_bg",
    )
  val borderColor by
    animateColorAsState(
      targetValue =
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (enabled) 0.6f else 0.3f),
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      label = "mode_chip_border",
    )
  val contentColor =
    when {
      selected -> MaterialTheme.colorScheme.onPrimaryContainer
      !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
      else -> MaterialTheme.colorScheme.onSurface
    }

  Surface(
    onClick = onClick,
    enabled = enabled,
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .testTag("mode_pill_${mode.name.lowercase()}"),
    shape = MaterialTheme.shapes.medium,
    color = containerColor,
    border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = modeIcon(mode),
        contentDescription = mode.title,
        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
      )
      Text(
        text = mode.title,
        style = MaterialTheme.typography.labelLarge,
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

private fun modeIcon(mode: FocusMode): ImageVector =
  when (mode) {
    FocusMode.DEEP_FOCUS -> Icons.Rounded.Shield
    FocusMode.STUDY_MODE -> Icons.AutoMirrored.Rounded.MenuBook
    FocusMode.LIGHT_FOCUS -> Icons.Rounded.AutoAwesome
  }

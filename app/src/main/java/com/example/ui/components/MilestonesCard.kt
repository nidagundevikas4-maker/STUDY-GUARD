package com.example.ui.components

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.MilestoneBadge
import com.example.model.MilestoneBadgeId
import com.example.model.StudyLevelInfo

@Composable
fun StudyLevelCard(
  level: StudyLevelInfo,
  unlockedBadgesCount: Int,
  totalBadgesCount: Int,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth().testTag("study_level_card"),
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
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f),
        ) {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Icon(
              imageVector = Icons.Rounded.WorkspacePremium,
              contentDescription = "Study Level ${level.levelNumber}",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(10.dp).size(24.dp),
            )
          }
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = "Level ${level.levelNumber} · ${level.title}",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "${formatStudyDuration(level.totalCompletedMinutes)} total focus logged",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Surface(
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
          Text(
            text = "$unlockedBadgesCount/$totalBadgesCount Badges",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          )
        }
      }

      LinearProgressIndicator(
        progress = { level.progressToNextLevel },
        modifier = Modifier.fillMaxWidth().height(8.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round,
      )

      val nextLabel =
        if (level.nextLevelTitle != null) {
          "${formatStudyDuration(level.minutesToNextLevel)} to Level ${level.levelNumber + 1} (${level.nextLevelTitle})"
        } else {
          "Highest scholar rank achieved"
        }
      Text(
        text = nextLabel,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MilestoneBadgesCard(
  badges: List<MilestoneBadge>,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth().testTag("milestone_badges_card"),
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
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = "Milestone Badges",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = "Earned through real study hours, strict lock streaks, and calm focus.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = 2,
      ) {
        badges.forEach { badge ->
          MilestoneBadgeTile(
            badge = badge,
            modifier = Modifier.weight(1f).widthIn(min = 140.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun MilestoneBadgeTile(
  badge: MilestoneBadge,
  modifier: Modifier = Modifier,
) {
  val containerColor =
    if (badge.isUnlocked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    else MaterialTheme.colorScheme.surfaceContainerLowest
  val borderColor =
    if (badge.isUnlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

  Surface(
    modifier = modifier.testTag("milestone_badge_${badge.id.name.lowercase()}"),
    shape = MaterialTheme.shapes.medium,
    color = containerColor,
    border = BorderStroke(1.dp, borderColor),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = MaterialTheme.shapes.small,
          color =
            if (badge.isUnlocked) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
        ) {
          Icon(
            imageVector = badgeIcon(badge.id),
            contentDescription = badge.title,
            tint =
              if (badge.isUnlocked) MaterialTheme.colorScheme.onPrimary
              else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(6.dp).size(16.dp),
          )
        }

        Icon(
          imageVector = if (badge.isUnlocked) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
          contentDescription = if (badge.isUnlocked) "Badge unlocked" else "Badge locked",
          tint =
            if (badge.isUnlocked) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
          modifier = Modifier.size(16.dp),
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = badge.title,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = badge.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.height(2.dp))

      LinearProgressIndicator(
        progress = { badge.progressFraction },
        modifier = Modifier.fillMaxWidth().height(5.dp),
        color =
          if (badge.isUnlocked) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.secondary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round,
      )

      Text(
        text = badge.progressText,
        style = MaterialTheme.typography.labelSmall,
        color =
          if (badge.isUnlocked) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

private fun badgeIcon(id: MilestoneBadgeId): ImageVector =
  when (id) {
    MilestoneBadgeId.FIRST_FOCUS -> Icons.Rounded.AutoAwesome
    MilestoneBadgeId.TEN_HOURS -> Icons.Rounded.Schedule
    MilestoneBadgeId.STRICT_STREAK_7 -> Icons.Rounded.LocalFireDepartment
    MilestoneBadgeId.ZERO_DISTRACTION_WEEK -> Icons.Rounded.EmojiEvents
    MilestoneBadgeId.DEEP_SCHOLAR_5 -> Icons.Rounded.WorkspacePremium
    MilestoneBadgeId.SHIELD_GUARDIAN -> Icons.Rounded.Shield
  }

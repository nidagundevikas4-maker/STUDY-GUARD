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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.permissions.SystemPermissionSnapshot

@Composable
fun ShieldStatusCard(
  permissionSnapshot: SystemPermissionSnapshot,
  modifier: Modifier = Modifier,
  onOpenRulesPreview: (() -> Unit)? = null,
) {
  val cardModifier = modifier.fillMaxWidth().testTag("shield_status_card")
  val cardColors =
    CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    )
  val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))

  if (onOpenRulesPreview != null) {
    Card(
      onClick = onOpenRulesPreview,
      modifier = cardModifier,
      shape = MaterialTheme.shapes.medium,
      colors = cardColors,
      border = cardBorder,
    ) {
      ShieldStatusCardContent(permissionSnapshot = permissionSnapshot)
    }
  } else {
    Card(
      modifier = cardModifier,
      shape = MaterialTheme.shapes.medium,
      colors = cardColors,
      border = cardBorder,
    ) {
      ShieldStatusCardContent(permissionSnapshot = permissionSnapshot)
    }
  }
}

@Composable
private fun ShieldStatusCardContent(permissionSnapshot: SystemPermissionSnapshot) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.home_shield_title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = stringResource(R.string.home_architecture_note_title),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(13.dp),
          )
          Text(
            text = "On-device",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }
      }
    }

    PermissionStatusRow(
      label = stringResource(R.string.home_shield_notif_access),
      granted = permissionSnapshot.notificationAccessGranted,
    )

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

    PermissionStatusRow(
      label = stringResource(R.string.home_shield_dnd_access),
      granted = permissionSnapshot.dndAccessGranted,
    )

    Spacer(modifier = Modifier.height(2.dp))

    Text(
      text = stringResource(R.string.home_architecture_note_body),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun PermissionStatusRow(
  label: String,
  granted: Boolean,
  statusOverride: String? = null,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.Schedule,
        contentDescription = null,
        tint =
          if (granted) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.size(16.dp),
      )
      Text(
        text =
          statusOverride
            ?: if (granted) stringResource(R.string.home_shield_status_ready)
            else stringResource(R.string.home_shield_status_pending),
        style = MaterialTheme.typography.labelMedium,
        color =
          if (granted) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

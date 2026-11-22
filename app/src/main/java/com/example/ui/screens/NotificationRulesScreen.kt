package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AppCategory
import com.example.model.DistractionEventRecord
import com.example.model.FocusMode
import com.example.model.FocusModeConfig
import com.example.model.RuleAction
import com.example.permissions.SystemPermissionSnapshot
import com.example.services.FocusTimerService
import com.example.ui.components.FocusModeSelector
import com.example.ui.viewmodel.AppRuleItem
import com.example.ui.viewmodel.NotificationRulesUiState
import com.example.ui.viewmodel.RuleFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationRulesScreen(
  uiState: NotificationRulesUiState,
  onModeSelected: (FocusMode) -> Unit,
  onSearchQueryChanged: (String) -> Unit,
  onFilterSelected: (RuleFilter) -> Unit,
  onToggleAppRule: (AppRuleItem) -> Unit,
  onResetModeDefaults: () -> Unit,
  onModeAutoDndChanged: (FocusMode, Boolean) -> Unit,
  onDeepAlarmsOnlyChanged: (Boolean) -> Unit,
  onModeAcademicPriorityChanged: (FocusMode, Boolean) -> Unit,
  onModeBlockAppsChanged: (FocusMode, Boolean) -> Unit = { _, _ -> },
  onModeStrictLockChanged: (FocusMode, Boolean) -> Unit = { _, _ -> },
  onOpenNotificationAccessSettings: () -> Unit,
  onOpenDndAccessSettings: () -> Unit,
  onOpenAppNotificationSettings: () -> Unit,
  onOpenUsageAccessSettings: () -> Unit = {},
  onOpenOverlaySettings: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val activeMode = uiState.selectedMode
  val modeConfig = uiState.preferences.configFor(activeMode)

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 600.dp).fillMaxSize().testTag("notification_rules_list"),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Focus Modes & Rules",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = "Configure per-mode notification filtering, DND, and installed app rules.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      item {
        NotificationListenerPermissionCard(
          permissionSnapshot = uiState.permissionSnapshot,
          onOpenNotificationAccessSettings = onOpenNotificationAccessSettings,
          onOpenAppNotificationSettings = onOpenAppNotificationSettings,
        )
      }

      item {
        DndPermissionCard(
          dndGranted = uiState.permissionSnapshot.dndAccessGranted,
          onOpenDndAccessSettings = onOpenDndAccessSettings,
        )
      }

      item {
        AppBlockerPermissionCard(
          usageAccessGranted = uiState.permissionSnapshot.usageAccessGranted,
          overlayGranted = uiState.permissionSnapshot.overlayPermissionGranted,
          onOpenUsageAccessSettings = onOpenUsageAccessSettings,
          onOpenOverlaySettings = onOpenOverlaySettings,
        )
      }

      if (uiState.isSessionLocked) {
        item {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.fillMaxWidth().testTag("rules_strict_lock_banner"),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = "Rules locked during study session",
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(20.dp),
              )
              Text(
                text =
                  "Strict Study Lock active (${FocusTimerService.formatCountdown(uiState.activeRemainingSeconds)} left). Mode switching, app unblocking, and rule resets are locked until completion.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
              )
            }
          }
        }
      }

      item {
        FocusModeSelector(
          activeMode = activeMode,
          onModeSelected = onModeSelected,
          isLocked = uiState.isSessionActive,
        )
      }

      item {
        ModeConfigurationCard(
          mode = activeMode,
          config = modeConfig,
          dndGranted = uiState.permissionSnapshot.dndAccessGranted,
          blockerReady = uiState.permissionSnapshot.appBlockerReady,
          isSessionLocked = uiState.isSessionLocked,
          onAutoDndChanged = { enabled -> onModeAutoDndChanged(activeMode, enabled) },
          onStrictAlarmsOnlyChanged = onDeepAlarmsOnlyChanged,
          onAcademicPriorityChanged = { enabled ->
            onModeAcademicPriorityChanged(activeMode, enabled)
          },
          onBlockAppsChanged = { enabled ->
            onModeBlockAppsChanged(activeMode, enabled)
          },
          onStrictLockChanged = { enabled ->
            onModeStrictLockChanged(activeMode, enabled)
          },
        )
      }

      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "App Rules · ${activeMode.title}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
              )
              val restrictedWord = if (modeConfig.blockSilencedApps) "blocked" else "silenced"
              Text(
                text = "${uiState.allowedCount} allowed · ${uiState.silencedCount} $restrictedWord",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            if (uiState.hasCustomizedRulesInMode) {
              TextButton(
                onClick = onResetModeDefaults,
                enabled = !uiState.isSessionLocked,
                modifier =
                  Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("reset_mode_rules_button"),
              ) {
                Text("Reset Defaults")
              }
            }
          }

          OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier.fillMaxWidth().testTag("app_rules_search_input"),
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            placeholder = { Text("Search installed apps...") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search apps",
              )
            },
            trailingIcon = {
              if (uiState.searchQuery.isNotEmpty()) {
                IconButton(
                  onClick = { onSearchQueryChanged("") },
                  modifier = Modifier.testTag("app_rules_search_clear"),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Clear,
                    contentDescription = "Clear search",
                  )
                }
              }
            },
          )

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            RuleFilter.entries.forEach { filter ->
              FilterChip(
                selected = uiState.activeFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label) },
                modifier = Modifier.testTag("rule_filter_${filter.name.lowercase()}"),
              )
            }
          }
        }
      }

      if (uiState.isLoadingApps) {
        item {
          Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
          }
        }
      } else if (uiState.appRules.isEmpty()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth().testTag("app_rules_empty_card"),
            shape = MaterialTheme.shapes.medium,
            colors =
              CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = "No matching apps found",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "Try clearing your search query or selecting the All filter.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        items(uiState.appRules, key = { it.packageName }) { item ->
          AppRuleRowCard(
            item = item,
            blockAppsEnabled = modeConfig.blockSilencedApps,
            isRuleLocked = uiState.isSessionLocked,
            onToggle = { onToggleAppRule(item) },
          )
        }
      }

      item {
        Card(
          modifier = Modifier.fillMaxWidth().testTag("rules_android_limits_card"),
          shape = MaterialTheme.shapes.medium,
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Icon(
              imageVector = Icons.Outlined.Info,
              contentDescription = "Android notification behavior",
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "How DND, Notification Filter & App Blocker Work",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text =
                  "Do Not Disturb suppresses sounds during active sessions. Notification Listener dismisses alerts from silenced apps. When App Blocker is enabled with Usage Access and Overlay permission, opening any blocked app displays a full-screen Study Guard Shield until your session ends.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      item {
        Text(
          text = "Recent Silenced & Blocked Activity",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }

      if (uiState.recentDistractions.isEmpty()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth().testTag("distractions_empty_card"),
            shape = MaterialTheme.shapes.medium,
            colors =
              CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Outlined.NotificationsOff,
                contentDescription = "No silenced notifications yet",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
              )
              Text(
                text =
                  "No silenced notifications logged yet. Grant Notification Access and start a focus session to filter distractions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        items(uiState.recentDistractions.take(15), key = { it.id }) { event ->
          DistractionEventRow(event = event)
        }
      }
    }
  }
}

@Composable
private fun AppRuleRowCard(
  item: AppRuleItem,
  blockAppsEnabled: Boolean,
  isRuleLocked: Boolean,
  onToggle: () -> Unit,
) {
  val isAllowed = item.action == RuleAction.ALLOW

  Card(
    modifier = Modifier.fillMaxWidth().testTag("app_rule_card_${item.packageName}"),
    shape = MaterialTheme.shapes.medium,
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (item.icon != null) {
        Image(
          bitmap = item.icon,
          contentDescription = "${item.appLabel} icon",
          modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)),
        )
      } else {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.size(40.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Apps,
            contentDescription = "${item.appLabel} icon",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(8.dp),
          )
        }
      }

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = item.appLabel,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color =
              if (item.category == AppCategory.ACADEMIC) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
              } else {
                MaterialTheme.colorScheme.surfaceVariant
              },
          ) {
            Text(
              text = item.category.label,
              style = MaterialTheme.typography.labelSmall,
              color =
                if (item.category == AppCategory.ACADEMIC) {
                  MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                },
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }

          if (item.isUserCustomized) {
            Surface(
              shape = MaterialTheme.shapes.extraSmall,
              color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
              Text(
                text = "Custom",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }
      }

      FilledTonalButton(
        onClick = onToggle,
        enabled = !isRuleLocked,
        modifier =
          Modifier
            .minimumInteractiveComponentSize()
            .testTag("app_rule_toggle_${item.packageName}"),
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
      ) {
        Text(
          text =
            when {
              isAllowed -> "Allowed"
              blockAppsEnabled -> "Blocked"
              else -> "Silenced"
            },
          style = MaterialTheme.typography.labelMedium,
          color =
            if (isAllowed) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error,
        )
      }
    }
  }
}

@Composable
private fun ModeConfigurationCard(
  mode: FocusMode,
  config: FocusModeConfig,
  dndGranted: Boolean,
  blockerReady: Boolean,
  isSessionLocked: Boolean,
  onAutoDndChanged: (Boolean) -> Unit,
  onStrictAlarmsOnlyChanged: (Boolean) -> Unit,
  onAcademicPriorityChanged: (Boolean) -> Unit,
  onBlockAppsChanged: (Boolean) -> Unit,
  onStrictLockChanged: (Boolean) -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth().testTag("mode_configuration_card"),
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
        text = "${mode.title} Configuration",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )

      ModeToggleRow(
        title = "Block opening silenced apps",
        subtitle =
          if (blockerReady) {
            "Prevents opening any app marked Blocked while ${mode.title} is running."
          } else {
            "Grant Usage Access and Display Over Apps above so Study Guard can block launching disabled apps."
          },
        checked = config.blockSilencedApps,
        enabled = !isSessionLocked,
        onCheckedChange = onBlockAppsChanged,
        testTag = "toggle_mode_app_blocking",
      )

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

      ModeToggleRow(
        title = "Strict session lock",
        subtitle =
          "Prevents pausing, shortening time, or unblocking apps until the timer completes (requires 10s cooldown + UNLOCK phrase for emergencies).",
        checked = config.strictSessionLock,
        enabled = !isSessionLocked,
        onCheckedChange = onStrictLockChanged,
        testTag = "toggle_mode_strict_lock",
      )

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

      ModeToggleRow(
        title = "Auto-enable Do Not Disturb",
        subtitle =
          if (dndGranted) "Engages system DND when a ${mode.title} session starts and restores it when done."
          else "Grant Do Not Disturb Access above so Study Guard can toggle system DND automatically.",
        checked = config.autoEnableDnd,
        enabled = true,
        onCheckedChange = onAutoDndChanged,
        testTag = "toggle_mode_auto_dnd",
      )

      if (mode == FocusMode.DEEP_FOCUS) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ModeToggleRow(
          title = "Alarms-only DND",
          subtitle = "Restricts system interruptions strictly to alarms instead of priority contacts.",
          checked = config.strictAlarmsOnlyDnd,
          enabled = true,
          onCheckedChange = onStrictAlarmsOnlyChanged,
          testTag = "toggle_deep_alarms_only",
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

      ModeToggleRow(
        title = "Prioritize Academic Alerts",
        subtitle = "Allows pre-classified LMS, calendar, and student email notifications during ${mode.title}.",
        checked = config.prioritizeAcademicNotifications,
        enabled = true,
        onCheckedChange = onAcademicPriorityChanged,
        testTag = "toggle_mode_academic_priority",
      )
    }
  }
}

@Composable
private fun ModeToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  enabled: Boolean = true,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(
      checked = checked,
      enabled = enabled,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.testTag(testTag),
    )
  }
}

@Composable
private fun DndPermissionCard(
  dndGranted: Boolean,
  onOpenDndAccessSettings: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth().testTag("dnd_access_card"),
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (dndGranted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
          else MaterialTheme.colorScheme.surfaceContainerLow,
      ),
    border =
      BorderStroke(
        width = 1.dp,
        color =
          if (dndGranted) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
          else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
      ),
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
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = if (dndGranted) Icons.Rounded.CheckCircle else Icons.Outlined.DoNotDisturbOn,
            contentDescription = if (dndGranted) "DND Access Granted" else "DND Access Needed",
            tint =
              if (dndGranted) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(22.dp),
          )
          Text(
            text = "Do Not Disturb Access",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        Surface(
          shape = MaterialTheme.shapes.small,
          color =
            if (dndGranted) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.tertiaryContainer,
        ) {
          Text(
            text = if (dndGranted) "Granted" else "Optional Setup",
            style = MaterialTheme.typography.labelSmall,
            color =
              if (dndGranted) MaterialTheme.colorScheme.onPrimaryContainer
              else MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      Text(
        text =
          if (dndGranted) {
            "Do Not Disturb access is enabled. Study Guard will automatically silence sounds/vibrations for modes with Auto-DND turned on."
          } else {
            "Grant Do Not Disturb Access so Deep Focus and Study Mode can suppress system sound & vibration automatically while focusing."
          },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Button(
        onClick = onOpenDndAccessSettings,
        modifier =
          Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("grant_dnd_access_button"),
      ) {
        Text(if (dndGranted) "Manage DND Access" else "Enable DND Access")
      }
    }
  }
}

@Composable
private fun NotificationListenerPermissionCard(
  permissionSnapshot: SystemPermissionSnapshot,
  onOpenNotificationAccessSettings: () -> Unit,
  onOpenAppNotificationSettings: () -> Unit,
) {
  val granted = permissionSnapshot.notificationAccessGranted
  val statusText =
    when {
      granted && permissionSnapshot.notificationListenerBound -> "Active & Connected"
      granted -> "Permission Granted"
      else -> "Action Required"
    }

  Card(
    modifier = Modifier.fillMaxWidth().testTag("notification_access_card"),
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (granted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
          else MaterialTheme.colorScheme.surfaceContainerLow,
      ),
    border =
      BorderStroke(
        width = 1.dp,
        color =
          if (granted) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
          else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
      ),
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
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector =
              if (granted) Icons.Rounded.CheckCircle else Icons.Outlined.NotificationsActive,
            contentDescription = statusText,
            tint =
              if (granted) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(22.dp),
          )
          Text(
            text = "Notification Access",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        Surface(
          shape = MaterialTheme.shapes.small,
          color =
            if (granted) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.tertiaryContainer,
        ) {
          Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall,
            color =
              if (granted) MaterialTheme.colorScheme.onPrimaryContainer
              else MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      Text(
        text =
          if (granted) {
            "Study Guard's NotificationListenerService is enabled. Incoming notifications from silenced apps will be dismissed during active sessions."
          } else {
            "Enable Study Guard under Android Notification Access so it can dismiss distracting notifications during active study sessions."
          },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Button(
          onClick = onOpenNotificationAccessSettings,
          modifier =
            Modifier
              .weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("grant_notification_access_button"),
        ) {
          Text(if (granted) "Manage Access" else "Enable Access")
        }

        FilledTonalButton(
          onClick = onOpenAppNotificationSettings,
          modifier =
            Modifier
              .minimumInteractiveComponentSize()
              .testTag("open_app_notification_settings_button"),
        ) {
          Text("Channels")
        }
      }
    }
  }
}

@Composable
private fun AppBlockerPermissionCard(
  usageAccessGranted: Boolean,
  overlayGranted: Boolean,
  onOpenUsageAccessSettings: () -> Unit,
  onOpenOverlaySettings: () -> Unit,
) {
  val bothReady = usageAccessGranted && overlayGranted
  val statusText =
    when {
      bothReady -> "Active"
      usageAccessGranted || overlayGranted -> "1 of 2 Granted"
      else -> "Setup Required"
    }

  Card(
    modifier = Modifier.fillMaxWidth().testTag("app_blocker_permission_card"),
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (bothReady) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
          else MaterialTheme.colorScheme.surfaceContainerLow,
      ),
    border =
      BorderStroke(
        width = 1.dp,
        color =
          if (bothReady) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
          else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
      ),
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
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector =
              if (bothReady) Icons.Rounded.CheckCircle else Icons.Outlined.NotificationsOff,
            contentDescription = "App Launch Blocker",
            tint =
              if (bothReady) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(22.dp),
          )
          Text(
            text = "App Launch Blocker",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        Surface(
          shape = MaterialTheme.shapes.small,
          color =
            if (bothReady) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.tertiaryContainer,
        ) {
          Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall,
            color =
              if (bothReady) MaterialTheme.colorScheme.onPrimaryContainer
              else MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      Text(
        text =
          if (bothReady) {
            "App Launch Blocker is ready. Opening any blocked app during Study Mode or Deep Focus will show the Study Guard Shield."
          } else {
            "To prevent opening blocked apps during active sessions, grant both Usage Access (detects blocked app launches) and Display Over Other Apps (raises the Focus Shield)."
          },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Button(
          onClick = onOpenUsageAccessSettings,
          modifier =
            Modifier
              .weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("grant_usage_access_button"),
        ) {
          Text(if (usageAccessGranted) "Usage: On" else "Usage Access")
        }

        FilledTonalButton(
          onClick = onOpenOverlaySettings,
          modifier =
            Modifier
              .weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("grant_overlay_access_button"),
        ) {
          Text(if (overlayGranted) "Overlay: On" else "Allow Overlay")
        }
      }
    }
  }
}

@Composable
private fun DistractionEventRow(event: DistractionEventRecord) {
  val formatter = remember { SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()) }
  val formattedTime = remember(event.timestampEpochMs) {
    formatter.format(Date(event.timestampEpochMs))
  }
  val isAppBlock = event.outcome == "APP_BLOCKED"

  Card(
    modifier = Modifier.fillMaxWidth().testTag("distraction_event_${event.id}"),
    shape = MaterialTheme.shapes.medium,
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = event.appLabel,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = "${event.focusMode.title} · $formattedTime",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Surface(
        shape = MaterialTheme.shapes.small,
        color =
          if (isAppBlock) MaterialTheme.colorScheme.errorContainer
          else MaterialTheme.colorScheme.secondaryContainer,
      ) {
        Text(
          text = if (isAppBlock) "App Blocked" else "Dismissed",
          style = MaterialTheme.typography.labelSmall,
          color =
            if (isAppBlock) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
      }
    }
  }
}

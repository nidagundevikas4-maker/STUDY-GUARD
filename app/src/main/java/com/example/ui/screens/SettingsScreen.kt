package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.FocusMode
import com.example.model.ThemePreference
import com.example.services.FocusTimerService
import com.example.ui.components.ShieldStatusCard
import com.example.ui.components.formatStudyDuration
import com.example.ui.viewmodel.SettingsUiState

@Composable
fun SettingsScreen(
  uiState: SettingsUiState,
  onThemeSelected: (ThemePreference) -> Unit,
  onDefaultModeSelected: (FocusMode) -> Unit,
  onDailyGoalSelected: (Int) -> Unit,
  onDefaultDurationSelected: (Int) -> Unit,
  onOpenNotificationAccessSettings: () -> Unit,
  onOpenDndAccessSettings: () -> Unit,
  onOpenAppNotificationSettings: () -> Unit,
  onOpenUsageAccessSettings: () -> Unit = {},
  onOpenOverlaySettings: () -> Unit = {},
  onResetAllRules: () -> Unit = {},
  onClearStudyHistory: () -> Unit = {},
  onDismissPrivacyMessage: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val scrollState = rememberScrollState()
  val goalPresets = listOf(60, 90, 120, 180)
  val durationPresets = listOf(15, 25, 45, 60)

  var showResetRulesDialog by remember { mutableStateOf(false) }
  var showClearHistoryDialog by remember { mutableStateOf(false) }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier =
        Modifier
          .widthIn(max = 600.dp)
          .fillMaxSize()
          .verticalScroll(scrollState)
          .padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = "Settings",
          style = MaterialTheme.typography.displaySmall,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = "Customize timer defaults, appearance, permissions, and privacy.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (uiState.isSessionActive) {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.tertiaryContainer,
          modifier = Modifier.fillMaxWidth().testTag("settings_lock_banner"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Outlined.Lock,
              contentDescription = "Session lock active",
              tint = MaterialTheme.colorScheme.onTertiaryContainer,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text =
                "Focus session active (${FocusTimerService.formatCountdown(uiState.activeRemainingSeconds)} left). Mode, duration, and rule resets are locked until completion.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
          }
        }
      }

      SettingsSectionCard(title = "Appearance") {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ThemePreference.entries.forEach { option ->
            FilterChip(
              selected = uiState.preferences.themePreference == option,
              onClick = { onThemeSelected(option) },
              label = {
                Text(
                  text = option.label,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              },
              modifier =
                Modifier
                  .weight(1f)
                  .minimumInteractiveComponentSize()
                  .testTag("settings_theme_${option.name.lowercase()}"),
            )
          }
        }
      }

      SettingsSectionCard(title = "Default Focus Mode") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FocusMode.entries.forEach { mode ->
              FilterChip(
                selected = uiState.preferences.activeFocusMode == mode,
                enabled = !uiState.isSessionActive,
                onClick = { onDefaultModeSelected(mode) },
                label = {
                  Text(
                    text = mode.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                },
                modifier =
                  Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("settings_mode_${mode.name.lowercase()}"),
              )
            }
          }
          Text(
            text = uiState.preferences.activeFocusMode.policySummary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      SettingsSectionCard(title = "Default Session Duration") {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          durationPresets.forEach { minutes ->
            FilterChip(
              selected = uiState.preferences.selectedDurationMinutes == minutes,
              enabled = !uiState.isSessionActive,
              onClick = { onDefaultDurationSelected(minutes) },
              label = {
                Text(
                  text = "${minutes}m",
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              },
              modifier =
                Modifier
                  .weight(1f)
                  .minimumInteractiveComponentSize()
                  .testTag("settings_duration_$minutes"),
            )
          }
        }
      }

      SettingsSectionCard(title = "Daily Study Target") {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          goalPresets.forEach { minutes ->
            FilterChip(
              selected = uiState.preferences.dailyGoalMinutes == minutes,
              onClick = { onDailyGoalSelected(minutes) },
              label = {
                Text(
                  text = formatStudyDuration(minutes),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              },
              modifier =
                Modifier
                  .weight(1f)
                  .minimumInteractiveComponentSize()
                  .testTag("settings_goal_$minutes"),
            )
          }
        }
      }

      SettingsSectionCard(title = "Permissions Management") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilledTonalButton(
              onClick = onOpenNotificationAccessSettings,
              modifier =
                Modifier
                  .weight(1f)
                  .minimumInteractiveComponentSize()
                  .testTag("settings_notification_access_button"),
            ) {
              Text("Notification Access", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            FilledTonalButton(
              onClick = onOpenDndAccessSettings,
              modifier =
                Modifier
                  .weight(1f)
                  .minimumInteractiveComponentSize()
                  .testTag("settings_dnd_access_button"),
            ) {
              Text("DND Access", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilledTonalButton(
              onClick = onOpenUsageAccessSettings,
              modifier =
                Modifier
                  .weight(1f)
                  .minimumInteractiveComponentSize()
                  .testTag("settings_usage_access_button"),
            ) {
              Text("Usage Access", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            FilledTonalButton(
              onClick = onOpenOverlaySettings,
              modifier =
                Modifier
                  .weight(1f)
                  .minimumInteractiveComponentSize()
                  .testTag("settings_overlay_access_button"),
            ) {
              Text("Display Over Apps", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
          FilledTonalButton(
            onClick = onOpenAppNotificationSettings,
            modifier =
              Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("settings_app_channels_button"),
          ) {
            Text("Study Guard Notification Channels")
          }
        }
      }

      SettingsSectionCard(title = "Data & Privacy") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Icon(
              imageVector = Icons.Outlined.Lock,
              contentDescription = "Local-first privacy",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text =
                "100% local storage via Room & DataStore. Study Guard never records notification message contents or transmits study logs off your device.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          AnimatedVisibility(visible = uiState.privacyActionMessage != null) {
            Surface(
              shape = MaterialTheme.shapes.small,
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.fillMaxWidth().testTag("settings_privacy_status_banner"),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = uiState.privacyActionMessage.orEmpty(),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.weight(1f),
                )
                TextButton(
                  onClick = onDismissPrivacyMessage,
                  modifier = Modifier.testTag("settings_dismiss_privacy_message"),
                ) {
                  Text("Dismiss")
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = { showResetRulesDialog = true },
              enabled = !uiState.isSessionLocked,
              modifier =
                Modifier
                  .weight(1f)
                  .minimumInteractiveComponentSize()
                  .testTag("settings_reset_rules_button"),
            ) {
              Text("Reset Rules", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
              onClick = { showClearHistoryDialog = true },
              modifier =
                Modifier
                  .weight(1f)
                  .minimumInteractiveComponentSize()
                  .testTag("settings_clear_history_button"),
              colors =
                ButtonDefaults.outlinedButtonColors(
                  contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
              Text("Clear History", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
        }
      }

      ShieldStatusCard(permissionSnapshot = uiState.permissionSnapshot)
    }
  }

  if (showResetRulesDialog) {
    AlertDialog(
      onDismissRequest = { showResetRulesDialog = false },
      title = { Text("Reset Notification Rules?") },
      text = {
        Text("All custom app allow/silence overrides across Deep Focus, Study Mode, and Light Focus will return to smart academic defaults.")
      },
      confirmButton = {
        TextButton(
          onClick = {
            onResetAllRules()
            showResetRulesDialog = false
          },
          modifier = Modifier.testTag("confirm_settings_reset_rules"),
        ) {
          Text("Reset Rules")
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetRulesDialog = false }) {
          Text("Cancel")
        }
      },
    )
  }

  if (showClearHistoryDialog) {
    AlertDialog(
      onDismissRequest = { showClearHistoryDialog = false },
      title = { Text("Clear Study History?") },
      text = {
        Text("This permanently deletes all saved focus sessions and local distraction counts from this device.")
      },
      confirmButton = {
        TextButton(
          onClick = {
            onClearStudyHistory()
            showClearHistoryDialog = false
          },
          colors =
            ButtonDefaults.textButtonColors(
              contentColor = MaterialTheme.colorScheme.error,
            ),
          modifier = Modifier.testTag("confirm_settings_clear_history"),
        ) {
          Text("Clear All")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearHistoryDialog = false }) {
          Text("Cancel")
        }
      },
    )
  }
}

@Composable
private fun SettingsSectionCard(
  title: String,
  content: @Composable () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(10.dp))
      content()
    }
  }
}

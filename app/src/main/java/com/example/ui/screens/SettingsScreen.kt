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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.EmergencyUnlockMethod
import com.example.model.FocusMode
import com.example.model.ThemePreference
import com.example.services.FocusTimerService
import com.example.tile.StudyTileHelper
import com.example.ui.components.ShieldStatusCard
import com.example.ui.components.formatStudyDuration
import com.example.ui.viewmodel.SettingsUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
  uiState: SettingsUiState,
  onThemeSelected: (ThemePreference) -> Unit,
  onDefaultModeSelected: (FocusMode) -> Unit,
  onDailyGoalSelected: (Int) -> Unit,
  onDefaultDurationSelected: (Int) -> Unit,
  onEmergencyUnlockMethodSelected: (EmergencyUnlockMethod) -> Unit = {},
  onOpenPinSetup: () -> Unit = {},
  onDismissPinSetup: () -> Unit = {},
  onSaveAccountabilityPin: (String, String, String) -> Unit = { _, _, _ -> },
  onRemoveAccountabilityPin: (String) -> Unit = {},
  onShortcutFeedback: (String) -> Unit = {},
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
  val context = LocalContext.current
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

      SettingsSectionCard(title = "Strict Lock Emergency Unlock") {
        Column(
          modifier = Modifier.fillMaxWidth().testTag("settings_emergency_unlock_card"),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            EmergencyUnlockMethod.entries.forEach { method ->
              FilterChip(
                selected = uiState.preferences.emergencyUnlockMethod == method,
                enabled = !uiState.isSessionActive,
                onClick = { onEmergencyUnlockMethodSelected(method) },
                label = {
                  Text(
                    text = method.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                },
                modifier =
                  Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("settings_unlock_method_${method.name.lowercase()}"),
              )
            }
          }

          Text(
            text = uiState.preferences.emergencyUnlockMethod.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              shape = MaterialTheme.shapes.small,
              color =
                if (uiState.preferences.hasAccountabilityPin) {
                  MaterialTheme.colorScheme.primaryContainer
                } else {
                  MaterialTheme.colorScheme.surfaceVariant
                },
            ) {
              Text(
                text =
                  if (uiState.preferences.hasAccountabilityPin) "Partner PIN: Configured"
                  else "Partner PIN: Not Set",
                style = MaterialTheme.typography.labelSmall,
                color =
                  if (uiState.preferences.hasAccountabilityPin) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                  } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                  },
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              )
            }

            FilledTonalButton(
              onClick = onOpenPinSetup,
              enabled = !uiState.isSessionActive,
              modifier =
                Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("settings_manage_pin_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Key,
                contentDescription = "Manage Accountability Partner PIN",
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.size(6.dp))
              Text(
                if (uiState.preferences.hasAccountabilityPin) "Manage Partner PIN"
                else "Set Partner PIN",
              )
            }
          }
        }
      }

      SettingsSectionCard(title = "Home Widget & Quick Settings Tile") {
        Column(
          modifier = Modifier.fillMaxWidth().testTag("settings_shortcuts_card"),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text =
              "Add the Study Guard Home Screen Widget to monitor today's focus progress & remaining goal, or add the Study Mode Quick Settings tile to start a locked session from your status bar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Button(
            onClick = { StudyTileHelper.requestAddTile(context, onShortcutFeedback) },
            modifier =
              Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("settings_add_qs_tile_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.AddToHomeScreen,
              contentDescription = "Add Study Mode Quick Settings tile",
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Add Study Mode Quick Tile")
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

  if (uiState.isPinDialogOpen) {
    AccountabilityPinDialog(
      hasExistingPin = uiState.preferences.hasAccountabilityPin,
      errorMessage = uiState.pinDialogError,
      onSave = onSaveAccountabilityPin,
      onRemove = onRemoveAccountabilityPin,
      onDismiss = onDismissPinSetup,
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
private fun AccountabilityPinDialog(
  hasExistingPin: Boolean,
  errorMessage: String?,
  onSave: (String, String, String) -> Unit,
  onRemove: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var currentPin by remember { mutableStateOf("") }
  var newPin by remember { mutableStateOf("") }
  var confirmPin by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier.testTag("accountability_pin_dialog"),
    title = {
      Text(if (hasExistingPin) "Manage Partner PIN" else "Set Accountability Partner PIN")
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text =
            "Have a parent, roommate, or friend choose a 4-digit PIN so early emergency unlocks require their confirmation.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (hasExistingPin) {
          OutlinedTextField(
            value = currentPin,
            onValueChange = { currentPin = it.filter { ch -> ch.isDigit() }.take(4) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            label = { Text("Current 4-Digit PIN") },
            modifier = Modifier.fillMaxWidth().testTag("pin_current_input"),
          )
        }

        OutlinedTextField(
          value = newPin,
          onValueChange = { newPin = it.filter { ch -> ch.isDigit() }.take(4) },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          label = { Text(if (hasExistingPin) "New 4-Digit PIN" else "4-Digit PIN") },
          modifier = Modifier.fillMaxWidth().testTag("pin_new_input"),
        )

        OutlinedTextField(
          value = confirmPin,
          onValueChange = { confirmPin = it.filter { ch -> ch.isDigit() }.take(4) },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          label = { Text("Confirm 4-Digit PIN") },
          modifier = Modifier.fillMaxWidth().testTag("pin_confirm_input"),
        )

        if (errorMessage != null) {
          Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag("pin_dialog_error"),
          )
        }

        if (hasExistingPin) {
          TextButton(
            onClick = { onRemove(currentPin) },
            colors =
              ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
              ),
            modifier = Modifier.testTag("pin_remove_button"),
          ) {
            Text("Remove Partner PIN")
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(currentPin, newPin, confirmPin) },
        modifier = Modifier.testTag("pin_save_button"),
      ) {
        Text("Save PIN")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("pin_cancel_button"),
      ) {
        Text("Cancel")
      }
    },
  )
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

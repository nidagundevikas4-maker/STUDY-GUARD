package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.model.EmergencyUnlockMethod
import com.example.model.TimerPhase
import com.example.services.FocusTimerService
import com.example.ui.components.SessionDurationPicker
import com.example.ui.theme.FocusFlowTypography
import com.example.ui.theme.MetricNumericStyle
import com.example.ui.viewmodel.FocusTimerUiState
import com.example.ui.viewmodel.MathChallenge
import kotlin.math.roundToInt

@Composable
fun FocusTimerScreen(
  uiState: FocusTimerUiState,
  onDurationSelected: (Int) -> Unit,
  onOpenCustomDuration: () -> Unit,
  onDismissCustomDuration: () -> Unit,
  onStartClick: () -> Unit,
  onPauseClick: () -> Unit,
  onResumeClick: () -> Unit,
  onResetClick: () -> Unit,
  onDismissCompletion: () -> Unit,
  onNotificationPermissionResult: (Boolean) -> Unit,
  onOpenEmergencyUnlock: () -> Unit = {},
  onDismissEmergencyUnlock: () -> Unit = {},
  onEmergencyPhraseChanged: (String) -> Unit = {},
  onEmergencyMathChanged: (String) -> Unit = {},
  onEmergencyPinChanged: (String) -> Unit = {},
  onConfirmEmergencyUnlock: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()
  val timerState = uiState.timerState

  val animatedProgress by
    animateFloatAsState(
      targetValue =
        when (timerState.phase) {
          TimerPhase.IDLE -> 0f
          TimerPhase.COMPLETED -> 1f
          else -> timerState.progressFraction
        },
      animationSpec = spring(stiffness = Spring.StiffnessLow),
      label = "timer_ring_progress",
    )

  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
      onNotificationPermissionResult(granted)
      onStartClick()
    }

  val handleStartWithPermission = {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      if (granted) {
        onNotificationPermissionResult(true)
        onStartClick()
      } else {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    } else {
      onStartClick()
    }
  }

  val trackColor = MaterialTheme.colorScheme.surfaceVariant
  val activeColor =
    if (timerState.phase == TimerPhase.PAUSED) MaterialTheme.colorScheme.tertiary
    else MaterialTheme.colorScheme.primary

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
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = "Focus Timer",
          style = MaterialTheme.typography.displaySmall,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = "${timerState.mode.title} · ${timerState.mode.subtitle}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      AnimatedVisibility(visible = uiState.showNotificationDeniedBanner) {
        Card(
          modifier = Modifier.fillMaxWidth().testTag("notification_denied_banner"),
          shape = MaterialTheme.shapes.medium,
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
            ),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Outlined.Info,
              contentDescription = "Notification status",
              tint = MaterialTheme.colorScheme.onTertiaryContainer,
              modifier = Modifier.size(20.dp),
            )
            Text(
              text =
                "Notification permission was denied. Your timer still runs accurately in the background, though lock-screen countdown alerts won't appear.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
          }
        }
      }

      Box(
        modifier = Modifier.size(248.dp).testTag("timer_dial"),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val strokeWidth = 14.dp.toPx()
          drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
          )
          if (animatedProgress > 0f) {
            drawArc(
              color = activeColor,
              startAngle = -90f,
              sweepAngle = 360f * animatedProgress,
              useCenter = false,
              style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
          }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = FocusTimerService.formatCountdown(timerState.remainingSeconds),
            style = FocusFlowTypography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.testTag("timer_countdown_text"),
          )
          Spacer(modifier = Modifier.height(6.dp))
          Surface(
            shape = MaterialTheme.shapes.small,
            color =
              if (timerState.isStrictLocked && timerState.isActive) {
                MaterialTheme.colorScheme.tertiaryContainer
              } else {
                MaterialTheme.colorScheme.primaryContainer
              },
          ) {
            val statusLabel =
              when (timerState.phase) {
                TimerPhase.IDLE -> "${timerState.mode.title} · Ready"
                TimerPhase.RUNNING ->
                  if (timerState.isStrictLocked) "${timerState.mode.title} · Strict Lock"
                  else "${timerState.mode.title} · Focusing"
                TimerPhase.PAUSED -> "${timerState.mode.title} · Paused"
                TimerPhase.COMPLETED -> "${timerState.mode.title} · Complete"
              }
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              if (timerState.isStrictLocked && timerState.isActive) {
                Icon(
                  imageVector = Icons.Rounded.Lock,
                  contentDescription = "Strict session lock active",
                  tint = MaterialTheme.colorScheme.onTertiaryContainer,
                  modifier = Modifier.size(14.dp),
                )
              }
              Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelMedium,
                color =
                  if (timerState.isStrictLocked && timerState.isActive) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                  } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                  },
              )
            }
          }
        }
      }

      when (timerState.phase) {
        TimerPhase.IDLE -> {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            SessionDurationPicker(
              selectedDurationMinutes = uiState.configuredDurationMinutes,
              onDurationSelected = onDurationSelected,
            )

            OutlinedButton(
              onClick = onOpenCustomDuration,
              modifier =
                Modifier
                  .fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .testTag("custom_duration_button"),
              shape = MaterialTheme.shapes.medium,
            ) {
              Icon(
                imageVector = Icons.Outlined.Tune,
                contentDescription = "Set custom duration",
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Custom Duration (${uiState.configuredDurationMinutes}m)")
            }

            if (uiState.isStrictLockConfiguredForMode) {
              Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth().testTag("strict_lock_preview_banner"),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Strict Lock enabled",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(18.dp),
                  )
                  val unlockSummary =
                    when (uiState.effectiveUnlockMethod) {
                      EmergencyUnlockMethod.ACCOUNTABILITY_PIN ->
                        "Early unlock requires your 4-digit Partner PIN."
                      EmergencyUnlockMethod.MATH_CHALLENGE ->
                        "Early unlock requires solving a math challenge + typing UNLOCK."
                      EmergencyUnlockMethod.COOLDOWN_PHRASE ->
                        "Early unlock requires a 10s cooldown + typing UNLOCK."
                    }
                  Text(
                    text =
                      "Strict Lock enabled for ${uiState.selectedMode.title}: pausing, shortening time, and blocked apps stay locked for ${uiState.configuredDurationMinutes}m. $unlockSummary",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                  )
                }
              }
            }

            Button(
              onClick = handleStartWithPermission,
              modifier =
                Modifier
                  .fillMaxWidth()
                  .height(56.dp)
                  .testTag("timer_start_button"),
              shape = MaterialTheme.shapes.medium,
              contentPadding = PaddingValues(horizontal = 24.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Start focus timer",
                modifier = Modifier.size(22.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Start ${uiState.configuredDurationMinutes}m Session",
                style = MaterialTheme.typography.labelLarge,
              )
            }
          }
        }

        TimerPhase.RUNNING -> {
          if (timerState.isStrictLocked) {
            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Card(
                modifier = Modifier.fillMaxWidth().testTag("strict_lock_active_card"),
                shape = MaterialTheme.shapes.large,
                colors =
                  CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
                  ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(16.dp),
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Strict study lock engaged",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(22.dp),
                  )
                  Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                      text = "Strict Study Lock Engaged",
                      style = MaterialTheme.typography.titleSmall,
                      color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                      text =
                        "Pausing, changing duration, and opening blocked apps stay locked until 00:00.",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                  }
                }
              }

              OutlinedButton(
                onClick = onOpenEmergencyUnlock,
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("timer_emergency_unlock_button"),
                shape = MaterialTheme.shapes.medium,
                colors =
                  ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                  ),
              ) {
                Text("Emergency Unlock", style = MaterialTheme.typography.labelLarge)
              }
            }
          } else {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              FilledTonalButton(
                onClick = onResetClick,
                modifier =
                  Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("timer_reset_button"),
                shape = MaterialTheme.shapes.medium,
              ) {
                Icon(
                  imageVector = Icons.Rounded.Refresh,
                  contentDescription = "Reset focus timer",
                  modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset", style = MaterialTheme.typography.labelLarge)
              }

              Button(
                onClick = onPauseClick,
                modifier =
                  Modifier
                    .weight(1.4f)
                    .height(56.dp)
                    .testTag("timer_pause_button"),
                shape = MaterialTheme.shapes.medium,
              ) {
                Icon(
                  imageVector = Icons.Rounded.Pause,
                  contentDescription = "Pause focus timer",
                  modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pause", style = MaterialTheme.typography.labelLarge)
              }
            }
          }
        }

        TimerPhase.PAUSED -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            OutlinedButton(
              onClick = onResetClick,
              modifier =
                Modifier
                  .weight(1f)
                  .height(56.dp)
                  .testTag("timer_reset_button"),
              shape = MaterialTheme.shapes.medium,
            ) {
              Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Reset focus timer",
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Reset", style = MaterialTheme.typography.labelLarge)
            }

            Button(
              onClick = onResumeClick,
              modifier =
                Modifier
                  .weight(1.4f)
                  .height(56.dp)
                  .testTag("timer_resume_button"),
              shape = MaterialTheme.shapes.medium,
            ) {
              Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Resume focus timer",
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Resume", style = MaterialTheme.typography.labelLarge)
            }
          }
        }

        TimerPhase.COMPLETED -> {
          Card(
            modifier = Modifier.fillMaxWidth().testTag("timer_completed_card"),
            shape = MaterialTheme.shapes.large,
            colors =
              CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
              ),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(20.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.CheckCircle,
                  contentDescription = "Session completed",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(26.dp),
                )
                Text(
                  text = "Session Complete",
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }
              Text(
                text =
                  "You completed ${timerState.plannedDurationMinutes} minutes in ${timerState.mode.title}. Your study time has been added to today's progress.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                OutlinedButton(
                  onClick = onDismissCompletion,
                  modifier =
                    Modifier
                      .weight(1f)
                      .minimumInteractiveComponentSize()
                      .testTag("completion_done_button"),
                ) {
                  Text("Reset Dial")
                }
                Button(
                  onClick = {
                    onDismissCompletion()
                    handleStartWithPermission()
                  },
                  modifier =
                    Modifier
                      .weight(1f)
                      .minimumInteractiveComponentSize()
                      .testTag("completion_restart_button"),
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.primary,
                      contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                  Text("Start Again")
                }
              }
            }
          }
        }
      }

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = "Background-Safe Countdown",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text =
              "Sessions run in an Android Foreground Service anchored to elapsed system time, keeping your countdown accurate if you switch apps or lock your screen.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }

  if (uiState.isCustomDurationDialogOpen) {
    CustomDurationDialog(
      initialMinutes = uiState.configuredDurationMinutes,
      onConfirm = onDurationSelected,
      onDismiss = onDismissCustomDuration,
    )
  }

  if (uiState.isEmergencyUnlockDialogOpen) {
    EmergencyUnlockDialog(
      remainingSeconds = timerState.remainingSeconds,
      unlockMethod = uiState.effectiveUnlockMethod,
      countdownSeconds = uiState.unlockCountdownSeconds,
      phraseInput = uiState.unlockPhraseInput,
      mathChallenge = uiState.mathChallenge,
      mathInput = uiState.unlockMathInput,
      pinInput = uiState.unlockPinInput,
      isPinIncorrect = uiState.isPinIncorrectAfterFourDigits,
      canConfirm = uiState.canConfirmEmergencyUnlock,
      onPhraseChanged = onEmergencyPhraseChanged,
      onMathChanged = onEmergencyMathChanged,
      onPinChanged = onEmergencyPinChanged,
      onConfirm = onConfirmEmergencyUnlock,
      onDismiss = onDismissEmergencyUnlock,
    )
  }
}

@Composable
private fun EmergencyUnlockDialog(
  remainingSeconds: Int,
  unlockMethod: EmergencyUnlockMethod,
  countdownSeconds: Int,
  phraseInput: String,
  mathChallenge: MathChallenge,
  mathInput: String,
  pinInput: String,
  isPinIncorrect: Boolean,
  canConfirm: Boolean,
  onPhraseChanged: (String) -> Unit,
  onMathChanged: (String) -> Unit,
  onPinChanged: (String) -> Unit,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier.testTag("emergency_unlock_dialog"),
    title = {
      Text(
        when (unlockMethod) {
          EmergencyUnlockMethod.ACCOUNTABILITY_PIN -> "Partner PIN Unlock"
          EmergencyUnlockMethod.MATH_CHALLENGE -> "Challenge Unlock"
          EmergencyUnlockMethod.COOLDOWN_PHRASE -> "Emergency Unlock"
        }
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text =
            "You still have ${FocusTimerService.formatCountdown(remainingSeconds)} left in this locked session. Unlocking early ends your lock, marks the session Interrupted, and lowers your Productivity Score.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when (unlockMethod) {
          EmergencyUnlockMethod.COOLDOWN_PHRASE -> {
            Surface(
              shape = MaterialTheme.shapes.small,
              color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
              modifier = Modifier.fillMaxWidth().testTag("emergency_unlock_countdown"),
            ) {
              Text(
                text =
                  if (countdownSeconds > 0) "Cooldown check: please wait ${countdownSeconds}s..."
                  else "Cooldown complete. Type UNLOCK below to end session.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              )
            }

            OutlinedTextField(
              value = phraseInput,
              onValueChange = onPhraseChanged,
              singleLine = true,
              label = { Text("Type UNLOCK to confirm") },
              placeholder = { Text("UNLOCK") },
              modifier = Modifier.fillMaxWidth().testTag("emergency_unlock_phrase_input"),
            )
          }

          EmergencyUnlockMethod.MATH_CHALLENGE -> {
            Surface(
              shape = MaterialTheme.shapes.small,
              color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f),
              modifier = Modifier.fillMaxWidth().testTag("emergency_unlock_math_prompt"),
            ) {
              Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Text(
                  text = "Solve to verify focus check:",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                  text = "${mathChallenge.prompt} = ?",
                  style = MetricNumericStyle,
                  color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
              }
            }

            OutlinedTextField(
              value = mathInput,
              onValueChange = onMathChanged,
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              label = { Text("Answer") },
              placeholder = { Text("Enter result") },
              modifier = Modifier.fillMaxWidth().testTag("emergency_unlock_math_input"),
            )

            OutlinedTextField(
              value = phraseInput,
              onValueChange = onPhraseChanged,
              singleLine = true,
              label = { Text("Type UNLOCK to confirm") },
              placeholder = { Text("UNLOCK") },
              modifier = Modifier.fillMaxWidth().testTag("emergency_unlock_phrase_input"),
            )
          }

          EmergencyUnlockMethod.ACCOUNTABILITY_PIN -> {
            Surface(
              shape = MaterialTheme.shapes.small,
              color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
              modifier = Modifier.fillMaxWidth().testTag("emergency_unlock_pin_banner"),
            ) {
              Text(
                text = "Ask your accountability partner (parent, roommate, or friend) to enter the 4-digit unlock PIN.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              )
            }

            OutlinedTextField(
              value = pinInput,
              onValueChange = onPinChanged,
              singleLine = true,
              isError = isPinIncorrect,
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
              label = { Text("4-Digit Partner PIN") },
              placeholder = { Text("••••") },
              supportingText =
                if (isPinIncorrect) {
                  { Text("Incorrect 4-digit PIN. Try again or keep studying.") }
                } else {
                  null
                },
              modifier = Modifier.fillMaxWidth().testTag("emergency_unlock_pin_input"),
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        enabled = canConfirm,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
          ),
        modifier = Modifier.testTag("emergency_unlock_confirm_button"),
      ) {
        Text("End & Unlock")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("emergency_unlock_cancel_button"),
      ) {
        Text("Keep Studying")
      }
    },
  )
}

@Composable
private fun CustomDurationDialog(
  initialMinutes: Int,
  onConfirm: (Int) -> Unit,
  onDismiss: () -> Unit,
) {
  var minutes by remember(initialMinutes) { mutableIntStateOf(initialMinutes.coerceIn(5, 180)) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Custom Focus Duration") },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          IconButton(
            onClick = { minutes = (minutes - 1).coerceAtLeast(5) },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("custom_duration_minus"),
          ) {
            Icon(Icons.Rounded.Remove, contentDescription = "Decrease duration")
          }
          Text(
            text = "$minutes min",
            style = MetricNumericStyle,
            color = MaterialTheme.colorScheme.primary,
          )
          IconButton(
            onClick = { minutes = (minutes + 1).coerceAtMost(180) },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("custom_duration_plus"),
          ) {
            Icon(Icons.Rounded.Add, contentDescription = "Increase duration")
          }
        }

        Slider(
          value = minutes.toFloat(),
          onValueChange = { raw -> minutes = raw.roundToInt().coerceIn(5, 180) },
          valueRange = 5f..180f,
          modifier = Modifier.fillMaxWidth().testTag("custom_duration_slider"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(minutes) },
        modifier = Modifier.testTag("custom_duration_confirm"),
      ) {
        Text("Apply")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("custom_duration_cancel"),
      ) {
        Text("Cancel")
      }
    },
  )
}

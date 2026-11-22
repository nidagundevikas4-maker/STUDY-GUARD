package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.FocusMode
import com.example.model.FocusTimerState
import com.example.model.ThemePreference
import com.example.model.TimerPhase
import com.example.services.FocusTimerService
import com.example.ui.theme.FocusFlowTheme
import com.example.ui.theme.MetricNumericStyle

class BlockedAppShieldActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val blockedAppLabel = intent.getStringExtra(EXTRA_BLOCKED_APP_LABEL).orEmpty().ifBlank { "This app" }
    val modeName = intent.getStringExtra(EXTRA_FOCUS_MODE)
    val initialMode = FocusMode.entries.firstOrNull { it.name == modeName } ?: FocusMode.STUDY_MODE

    val appContainer = (application as FocusFlowApplication).appContainer

    setContent {
      val prefs by
        appContainer.preferencesRepository.preferencesFlow.collectAsStateWithLifecycle(
          initialValue = com.example.model.UserPreferences()
        )
      val timerState by appContainer.focusTimerRepository.timerState.collectAsStateWithLifecycle()

      val isDarkTheme =
        when (prefs.themePreference) {
          ThemePreference.SYSTEM -> isSystemInDarkTheme()
          ThemePreference.LIGHT -> false
          ThemePreference.DARK -> true
        }

      LaunchedEffect(timerState.phase, timerState.isStrictLocked) {
        val shouldStayBlocked =
          timerState.phase == TimerPhase.RUNNING || (timerState.isStrictLocked && timerState.isActive)
        if (!shouldStayBlocked) {
          finishAndRemoveTask()
        }
      }

      BackHandler {
        navigateToHomeLauncher()
      }

      FocusFlowTheme(darkTheme = isDarkTheme) {
        BlockedAppShieldScreen(
          blockedAppLabel = blockedAppLabel,
          activeMode = if (timerState.isActive) timerState.mode else initialMode,
          timerState = timerState,
          onReturnToFocusFlow = ::navigateToFocusFlow,
          onGoHome = ::navigateToHomeLauncher,
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
  }

  private fun navigateToFocusFlow() {
    val mainIntent =
      Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      }
    startActivity(mainIntent)
    finishAndRemoveTask()
  }

  private fun navigateToHomeLauncher() {
    val homeIntent =
      Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
    startActivity(homeIntent)
    finishAndRemoveTask()
  }

  companion object {
    private const val EXTRA_BLOCKED_APP_LABEL = "extra_blocked_app_label"
    private const val EXTRA_FOCUS_MODE = "extra_focus_mode"

    fun createIntent(
      context: Context,
      blockedAppLabel: String,
      focusMode: FocusMode,
    ): Intent =
      Intent(context, BlockedAppShieldActivity::class.java).apply {
        putExtra(EXTRA_BLOCKED_APP_LABEL, blockedAppLabel)
        putExtra(EXTRA_FOCUS_MODE, focusMode.name)
        flags =
          Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
      }
  }
}

@Composable
fun BlockedAppShieldScreen(
  blockedAppLabel: String,
  activeMode: FocusMode,
  timerState: FocusTimerState,
  onReturnToFocusFlow: () -> Unit,
  onGoHome: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .safeDrawingPadding()
          .padding(horizontal = 24.dp, vertical = 20.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        Surface(
          shape = MaterialTheme.shapes.extraLarge,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(84.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.Shield,
              contentDescription = "Focus Shield active",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(42.dp),
            )
          }
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(
            shape = MaterialTheme.shapes.small,
            color =
              if (timerState.isStrictLocked) MaterialTheme.colorScheme.tertiaryContainer
              else MaterialTheme.colorScheme.secondaryContainer,
          ) {
            Text(
              text =
                if (timerState.isStrictLocked) "${activeMode.title} · Strict Lock Active"
                else "${activeMode.title} Active",
              style = MaterialTheme.typography.labelMedium,
              color =
                if (timerState.isStrictLocked) MaterialTheme.colorScheme.onTertiaryContainer
                else MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            )
          }

          Text(
            text = "$blockedAppLabel is Blocked",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("blocked_shield_title"),
          )

          Text(
            text =
              if (timerState.isStrictLocked) {
                "Strict Study Lock is active for ${activeMode.title}. Pausing and switching apps are locked until the countdown finishes. Academic apps remain accessible."
              } else {
                "You blocked distracting apps during ${activeMode.title}. Academic and allowed apps remain accessible so you can finish your study block calmly."
              },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
        }

        Card(
          modifier = Modifier.fillMaxWidth().testTag("blocked_shield_countdown_card"),
          shape = MaterialTheme.shapes.large,
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = "Time Remaining",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = FocusTimerService.formatCountdown(timerState.remainingSeconds),
              style = MetricNumericStyle,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }

        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = onReturnToFocusFlow,
            modifier =
              Modifier
                .fillMaxWidth()
                .height(54.dp)
                .minimumInteractiveComponentSize()
                .testTag("blocked_shield_return_button"),
            shape = MaterialTheme.shapes.medium,
            contentPadding = PaddingValues(horizontal = 20.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Spa,
              contentDescription = "Return to Study Guard",
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Return to Study Guard", style = MaterialTheme.typography.labelLarge)
          }

          OutlinedButton(
            onClick = onGoHome,
            modifier =
              Modifier
                .fillMaxWidth()
                .height(52.dp)
                .minimumInteractiveComponentSize()
                .testTag("blocked_shield_home_button"),
            shape = MaterialTheme.shapes.medium,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.Home,
                contentDescription = "Go to Home screen",
                modifier = Modifier.size(20.dp),
              )
              Text("Go to Home Screen", style = MaterialTheme.typography.labelLarge)
            }
          }
        }
      }
    }
  }
}

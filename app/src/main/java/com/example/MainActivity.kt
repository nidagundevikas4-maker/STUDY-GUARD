package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.ThemePreference
import com.example.navigation.FocusFlowApp
import com.example.ui.theme.FocusFlowTheme
import com.example.ui.viewmodel.HomeViewModel
import com.example.widget.StudyWidgetUpdater

enum class MainLaunchRequest {
  NONE,
  OPEN_TIMER,
  START_STUDY_MODE,
}

class MainActivity : ComponentActivity() {

  private var pendingLaunchRequest by mutableStateOf(MainLaunchRequest.NONE)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    pendingLaunchRequest = parseLaunchRequest(intent)

    val appContainer = (application as FocusFlowApplication).appContainer
    val homeViewModelFactory =
      HomeViewModel.Factory(
        appContainer.preferencesRepository,
        appContainer.permissionReader,
        appContainer.focusTimerRepository,
        appContainer.sessionRepository,
        onWidgetDataChanged = { StudyWidgetUpdater.requestUpdateAsync(applicationContext) },
      )

    setContent {
      val homeViewModel: HomeViewModel = viewModel(factory = homeViewModelFactory)
      val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()

      val isDarkTheme =
        when (homeUiState.themePreference) {
          ThemePreference.SYSTEM -> isSystemInDarkTheme()
          ThemePreference.LIGHT -> false
          ThemePreference.DARK -> true
        }

      FocusFlowTheme(darkTheme = isDarkTheme) {
        FocusFlowApp(
          appContext = applicationContext,
          appContainer = appContainer,
          isDarkTheme = isDarkTheme,
          homeViewModel = homeViewModel,
          launchRequest = pendingLaunchRequest,
          onLaunchRequestConsumed = { pendingLaunchRequest = MainLaunchRequest.NONE },
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    val parsed = parseLaunchRequest(intent)
    if (parsed != MainLaunchRequest.NONE) {
      pendingLaunchRequest = parsed
    }
  }

  private fun parseLaunchRequest(intent: Intent?): MainLaunchRequest =
    when (intent?.getStringExtra(EXTRA_LAUNCH_ACTION)) {
      LAUNCH_ACTION_OPEN_TIMER -> MainLaunchRequest.OPEN_TIMER
      LAUNCH_ACTION_START_STUDY -> MainLaunchRequest.START_STUDY_MODE
      else -> MainLaunchRequest.NONE
    }

  companion object {
    const val EXTRA_LAUNCH_ACTION = "extra_launch_action"
    const val LAUNCH_ACTION_OPEN_TIMER = "open_timer"
    const val LAUNCH_ACTION_START_STUDY = "start_study_mode"

    fun createOpenHomeIntent(context: Context): Intent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      }

    fun createOpenTimerIntent(context: Context): Intent =
      createOpenHomeIntent(context).apply {
        putExtra(EXTRA_LAUNCH_ACTION, LAUNCH_ACTION_OPEN_TIMER)
      }

    fun createQuickStudyIntent(context: Context): Intent =
      createOpenHomeIntent(context).apply {
        putExtra(EXTRA_LAUNCH_ACTION, LAUNCH_ACTION_START_STUDY)
      }
  }
}

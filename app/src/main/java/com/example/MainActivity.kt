package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.ThemePreference
import com.example.navigation.FocusFlowApp
import com.example.ui.theme.FocusFlowTheme
import com.example.ui.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val appContainer = (application as FocusFlowApplication).appContainer
    val homeViewModelFactory =
      HomeViewModel.Factory(
        appContainer.preferencesRepository,
        appContainer.permissionReader,
        appContainer.focusTimerRepository,
        appContainer.sessionRepository,
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
        )
      }
    }
  }
}

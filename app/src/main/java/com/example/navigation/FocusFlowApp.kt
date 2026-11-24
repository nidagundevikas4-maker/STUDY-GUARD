package com.example.navigation

import android.content.Context
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.MainLaunchRequest
import com.example.di.AppContainer
import com.example.model.FocusMode
import com.example.services.FocusTimerService
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.FocusTimerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NotificationRulesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WeeklyTrendsScreen
import com.example.ui.viewmodel.AnalyticsViewModel
import com.example.ui.viewmodel.FocusTimerViewModel
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.NotificationRulesViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.WeeklyTrendsViewModel
import com.example.widget.StudyWidgetUpdater
import kotlinx.coroutines.flow.first

@Composable
fun FocusFlowApp(
  appContext: Context,
  appContainer: AppContainer,
  isDarkTheme: Boolean,
  homeViewModel: HomeViewModel,
  launchRequest: MainLaunchRequest = MainLaunchRequest.NONE,
  onLaunchRequestConsumed: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination

  val focusTimerViewModel: FocusTimerViewModel =
    viewModel(
      factory =
        FocusTimerViewModel.Factory(
          appContext = appContext,
          preferencesRepository = appContainer.preferencesRepository,
          timerRepository = appContainer.focusTimerRepository,
        )
    )

  val notificationRulesViewModel: NotificationRulesViewModel =
    viewModel(
      factory =
        NotificationRulesViewModel.Factory(
          preferencesRepository = appContainer.preferencesRepository,
          installedAppsRepository = appContainer.installedAppsRepository,
          notificationRuleRepository = appContainer.notificationRuleRepository,
          permissionReader = appContainer.permissionReader,
          timerRepository = appContainer.focusTimerRepository,
        )
    )

  val analyticsViewModel: AnalyticsViewModel =
    viewModel(
      factory =
        AnalyticsViewModel.Factory(
          appContainer.sessionRepository,
          appContainer.notificationRuleRepository,
        )
    )

  val settingsViewModel: SettingsViewModel =
    viewModel(
      factory =
        SettingsViewModel.Factory(
          preferencesRepository = appContainer.preferencesRepository,
          permissionReader = appContainer.permissionReader,
          sessionRepository = appContainer.sessionRepository,
          notificationRuleRepository = appContainer.notificationRuleRepository,
          timerRepository = appContainer.focusTimerRepository,
          onWidgetDataChanged = { StudyWidgetUpdater.requestUpdateAsync(appContext) },
        )
    )

  val weeklyTrendsViewModel: WeeklyTrendsViewModel =
    viewModel(
      factory =
        WeeklyTrendsViewModel.Factory(
          sessionRepository = appContainer.sessionRepository,
          notificationRuleRepository = appContainer.notificationRuleRepository,
          preferencesRepository = appContainer.preferencesRepository,
        )
    )

  val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
  val timerUiState by focusTimerViewModel.uiState.collectAsStateWithLifecycle()
  val rulesUiState by notificationRulesViewModel.uiState.collectAsStateWithLifecycle()
  val analyticsUiState by analyticsViewModel.uiState.collectAsStateWithLifecycle()
  val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
  val weeklyTrendsUiState by weeklyTrendsViewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(launchRequest) {
    when (launchRequest) {
      MainLaunchRequest.NONE -> Unit
      MainLaunchRequest.OPEN_TIMER -> {
        navigateToTopLevel(navController, FocusFlowDestination.FocusTimer)
        onLaunchRequestConsumed()
      }
      MainLaunchRequest.START_STUDY_MODE -> {
        navigateToTopLevel(navController, FocusFlowDestination.FocusTimer)
        if (!appContainer.focusTimerRepository.timerState.value.isActive) {
          val prefs = appContainer.preferencesRepository.preferencesFlow.first()
          val duration =
            if (prefs.activeFocusMode == FocusMode.STUDY_MODE) prefs.selectedDurationMinutes
            else FocusMode.STUDY_MODE.defaultDurationMinutes
          appContainer.preferencesRepository.setActiveFocusMode(FocusMode.STUDY_MODE)
          appContainer.preferencesRepository.setSelectedDurationMinutes(duration)
          FocusTimerService.startSession(
            context = appContext,
            mode = FocusMode.STUDY_MODE,
            durationMinutes = duration,
            strictLock = prefs.studyModeConfig.strictSessionLock,
          )
        }
        onLaunchRequestConsumed()
      }
    }
  }

  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    homeViewModel.refreshSystemPermissions()
    notificationRulesViewModel.refreshPermissions()
    settingsViewModel.refreshPermissions()
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 640.dp

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = MaterialTheme.colorScheme.background,
      contentWindowInsets = WindowInsets.safeDrawing,
      bottomBar = {
        if (!useNavRail) {
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp,
          ) {
            FocusFlowDestination.topLevelDestinations.forEach { destination ->
              val selected =
                currentDestination?.hierarchy?.any { it.route == destination.route } == true
              val label = stringResource(destination.labelRes)
              NavigationBarItem(
                selected = selected,
                onClick = { navigateToTopLevel(navController, destination) },
                icon = {
                  Icon(
                    imageVector =
                      if (selected) destination.selectedIcon else destination.unselectedIcon,
                    contentDescription = label,
                  )
                },
                label = {
                  Text(
                    text = label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                },
                modifier = Modifier.testTag(destination.testTag),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (useNavRail) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            windowInsets =
              WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start),
          ) {
            Spacer(modifier = Modifier.height(12.dp))
            FocusFlowDestination.topLevelDestinations.forEach { destination ->
              val selected =
                currentDestination?.hierarchy?.any { it.route == destination.route } == true
              val label = stringResource(destination.labelRes)
              NavigationRailItem(
                selected = selected,
                onClick = { navigateToTopLevel(navController, destination) },
                icon = {
                  Icon(
                    imageVector =
                      if (selected) destination.selectedIcon else destination.unselectedIcon,
                    contentDescription = label,
                  )
                },
                label = {
                  Text(
                    text = label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                },
                modifier = Modifier.testTag(destination.testTag),
              )
            }
          }
        }

        NavHost(
          navController = navController,
          startDestination = FocusFlowDestination.Home.route,
          modifier = Modifier.weight(1f).fillMaxSize(),
        ) {
          composable(FocusFlowDestination.Home.route) {
            HomeScreen(
              uiState = homeUiState,
              isDarkTheme = isDarkTheme,
              onToggleTheme = { homeViewModel.cycleThemePreference(isDarkTheme) },
              onNavigateToTimer = {
                navigateToTopLevel(navController, FocusFlowDestination.FocusTimer)
              },
              onNavigateToAnalytics = {
                navigateToTopLevel(navController, FocusFlowDestination.Analytics)
              },
              onNavigateToWeeklyTrends = {
                navController.navigate(FocusFlowDestination.WeeklyTrends.route) {
                  launchSingleTop = true
                }
              },
              onNavigateToRules = {
                navigateToTopLevel(navController, FocusFlowDestination.NotificationRules)
              },
              onNavigateToSettings = {
                navigateToTopLevel(navController, FocusFlowDestination.Settings)
              },
              homeViewModel = homeViewModel,
            )
          }

          composable(FocusFlowDestination.FocusTimer.route) {
            FocusTimerScreen(
              uiState = timerUiState,
              onDurationSelected = focusTimerViewModel::selectDurationMinutes,
              onOpenCustomDuration = focusTimerViewModel::openCustomDurationDialog,
              onDismissCustomDuration = focusTimerViewModel::dismissCustomDurationDialog,
              onStartClick = focusTimerViewModel::startFocusSession,
              onPauseClick = focusTimerViewModel::pauseFocusSession,
              onResumeClick = focusTimerViewModel::resumeFocusSession,
              onResetClick = focusTimerViewModel::resetFocusSession,
              onDismissCompletion = focusTimerViewModel::dismissCompletionState,
              onNotificationPermissionResult = focusTimerViewModel::onNotificationPermissionResult,
              onOpenEmergencyUnlock = focusTimerViewModel::openEmergencyUnlockDialog,
              onDismissEmergencyUnlock = focusTimerViewModel::dismissEmergencyUnlockDialog,
              onEmergencyPhraseChanged = focusTimerViewModel::updateEmergencyUnlockPhrase,
              onEmergencyMathChanged = focusTimerViewModel::updateEmergencyMathInput,
              onEmergencyPinChanged = focusTimerViewModel::updateEmergencyPinInput,
              onConfirmEmergencyUnlock = focusTimerViewModel::confirmEmergencyUnlock,
            )
          }

          composable(FocusFlowDestination.NotificationRules.route) {
            NotificationRulesScreen(
              uiState = rulesUiState,
              onModeSelected = notificationRulesViewModel::selectMode,
              onSearchQueryChanged = notificationRulesViewModel::updateSearchQuery,
              onFilterSelected = notificationRulesViewModel::selectFilter,
              onToggleAppRule = notificationRulesViewModel::toggleAppAction,
              onResetModeDefaults = notificationRulesViewModel::resetModeRulesToDefaults,
              onModeAutoDndChanged = settingsViewModel::setModeAutoDnd,
              onDeepAlarmsOnlyChanged = settingsViewModel::setDeepFocusAlarmsOnlyDnd,
              onModeAcademicPriorityChanged = settingsViewModel::setModeAcademicPriority,
              onModeBlockAppsChanged = settingsViewModel::setModeBlockApps,
              onModeStrictLockChanged = settingsViewModel::setModeStrictLock,
              onOpenNotificationAccessSettings = {
                appContainer.permissionReader.openNotificationListenerSettings(appContext)
              },
              onOpenDndAccessSettings = {
                appContainer.permissionReader.openDndPolicySettings(appContext)
              },
              onOpenAppNotificationSettings = {
                appContainer.permissionReader.openAppNotificationSettings(appContext)
              },
              onOpenUsageAccessSettings = {
                appContainer.permissionReader.openUsageAccessSettings(appContext)
              },
              onOpenOverlaySettings = {
                appContainer.permissionReader.openOverlaySettings(appContext)
              },
            )
          }

          composable(FocusFlowDestination.Analytics.route) {
            AnalyticsScreen(
              uiState = analyticsUiState,
              onPeriodSelected = analyticsViewModel::selectPeriod,
              onDeleteSession = analyticsViewModel::deleteSession,
              onClearHistory = analyticsViewModel::clearAllHistory,
              onOpenWeeklyTrends = {
                navController.navigate(FocusFlowDestination.WeeklyTrends.route) {
                  launchSingleTop = true
                }
              },
            )
          }

          composable(FocusFlowDestination.WeeklyTrends.route) {
            WeeklyTrendsScreen(
              uiState = weeklyTrendsUiState,
              onRangeSelected = weeklyTrendsViewModel::selectRange,
              onSelectBucket = weeklyTrendsViewModel::selectBucket,
              onBackClick = { navController.popBackStack() },
            )
          }

          composable(FocusFlowDestination.Settings.route) {
            SettingsScreen(
              uiState = settingsUiState,
              onThemeSelected = settingsViewModel::setThemePreference,
              onDefaultModeSelected = settingsViewModel::setDefaultFocusMode,
              onDailyGoalSelected = settingsViewModel::setDailyGoalMinutes,
              onDefaultDurationSelected = settingsViewModel::setDefaultDurationMinutes,
              onEmergencyUnlockMethodSelected = settingsViewModel::setEmergencyUnlockMethod,
              onOpenPinSetup = settingsViewModel::openPinSetupDialog,
              onDismissPinSetup = settingsViewModel::dismissPinSetupDialog,
              onSaveAccountabilityPin = settingsViewModel::saveAccountabilityPin,
              onRemoveAccountabilityPin = settingsViewModel::removeAccountabilityPin,
              onShortcutFeedback = settingsViewModel::showShortcutFeedback,
              onOpenNotificationAccessSettings = {
                appContainer.permissionReader.openNotificationListenerSettings(appContext)
              },
              onOpenDndAccessSettings = {
                appContainer.permissionReader.openDndPolicySettings(appContext)
              },
              onOpenAppNotificationSettings = {
                appContainer.permissionReader.openAppNotificationSettings(appContext)
              },
              onOpenUsageAccessSettings = {
                appContainer.permissionReader.openUsageAccessSettings(appContext)
              },
              onOpenOverlaySettings = {
                appContainer.permissionReader.openOverlaySettings(appContext)
              },
              onResetAllRules = settingsViewModel::resetAllNotificationRules,
              onClearStudyHistory = settingsViewModel::clearAllStudyHistory,
              onDismissPrivacyMessage = settingsViewModel::dismissPrivacyMessage,
            )
          }
        }
      }
    }
  }
}

private fun navigateToTopLevel(
  navController: NavHostController,
  destination: FocusFlowDestination,
) {
  navController.navigate(destination.route) {
    popUpTo(navController.graph.findStartDestination().id) {
      saveState = true
    }
    launchSingleTop = true
    restoreState = true
  }
}

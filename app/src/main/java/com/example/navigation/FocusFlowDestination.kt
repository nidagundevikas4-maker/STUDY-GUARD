package com.example.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

sealed class FocusFlowDestination(
  val route: String,
  @StringRes val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
) {
  data object Home :
    FocusFlowDestination(
      route = "home",
      labelRes = R.string.nav_home,
      selectedIcon = Icons.Filled.Home,
      unselectedIcon = Icons.Outlined.Home,
      testTag = "nav_home",
    )

  data object FocusTimer :
    FocusFlowDestination(
      route = "focus_timer",
      labelRes = R.string.nav_focus,
      selectedIcon = Icons.Filled.Timer,
      unselectedIcon = Icons.Outlined.Timer,
      testTag = "nav_focus_timer",
    )

  data object NotificationRules :
    FocusFlowDestination(
      route = "notification_rules",
      labelRes = R.string.nav_rules,
      selectedIcon = Icons.Filled.NotificationsOff,
      unselectedIcon = Icons.Outlined.NotificationsOff,
      testTag = "nav_notification_rules",
    )

  data object Analytics :
    FocusFlowDestination(
      route = "analytics",
      labelRes = R.string.nav_analytics,
      selectedIcon = Icons.Filled.BarChart,
      unselectedIcon = Icons.Outlined.BarChart,
      testTag = "nav_analytics",
    )

  data object WeeklyTrends :
    FocusFlowDestination(
      route = "weekly_trends",
      labelRes = R.string.nav_weekly_trends,
      selectedIcon = Icons.AutoMirrored.Filled.ShowChart,
      unselectedIcon = Icons.AutoMirrored.Outlined.ShowChart,
      testTag = "nav_weekly_trends",
    )

  data object Settings :
    FocusFlowDestination(
      route = "settings",
      labelRes = R.string.nav_settings,
      selectedIcon = Icons.Filled.Settings,
      unselectedIcon = Icons.Outlined.Settings,
      testTag = "nav_settings",
    )

  companion object {
    val topLevelDestinations =
      listOf(Home, FocusTimer, NotificationRules, Analytics, Settings)
  }
}

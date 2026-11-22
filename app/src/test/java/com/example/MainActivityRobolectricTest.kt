package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class MainActivityRobolectricTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `launches home screen switches modes and navigates across all tabs`() {
    composeTestRule.waitForIdle()

    // Home screen displays core cards and CTA button
    composeTestRule.onNodeWithTag("daily_progress_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("start_focus_button").performScrollTo().assertIsDisplayed()

    // Switch to Deep Focus on Home and wait for DataStore update
    composeTestRule.onNodeWithTag("mode_pill_deep_focus").performScrollTo().performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithText("45 min preset").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("45 min preset").performScrollTo().assertIsDisplayed()

    // Navigate to Focus Timer tab
    composeTestRule.onNodeWithTag("nav_focus_timer").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("timer_dial").assertIsDisplayed()
    composeTestRule.onNodeWithTag("strict_lock_preview_banner").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("timer_start_button").performScrollTo().assertIsDisplayed()

    // Navigate to Notification Rules tab
    composeTestRule.onNodeWithTag("nav_notification_rules").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("notification_access_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("dnd_access_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("app_blocker_permission_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("notification_rules_list")
      .performScrollToNode(hasTestTag("toggle_mode_strict_lock"))
    composeTestRule.onNodeWithTag("toggle_mode_strict_lock").assertIsDisplayed()

    // Navigate to Analytics tab
    composeTestRule.onNodeWithTag("nav_analytics").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("analytics_period_week").assertIsDisplayed()

    // Open Weekly Study Trends screen from Analytics and verify charts
    composeTestRule.onNodeWithTag("open_weekly_trends_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("trends_range_last_4_weeks").assertIsDisplayed()
    composeTestRule.onNodeWithTag("weekly_trends_list")
      .performScrollToNode(hasTestTag("weekly_study_time_chart_card"))
    composeTestRule.onNodeWithTag("weekly_study_time_chart_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("weekly_trends_list")
      .performScrollToNode(hasTestTag("productivity_trend_chart_card"))
    composeTestRule.onNodeWithTag("productivity_trend_chart_card").assertIsDisplayed()

    // Navigate back to Analytics
    composeTestRule.onNodeWithTag("weekly_trends_back_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("analytics_period_week").assertIsDisplayed()

    // Navigate to Settings tab
    composeTestRule.onNodeWithTag("nav_settings").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("settings_theme_dark").assertIsDisplayed()
    composeTestRule.onNodeWithTag("settings_reset_rules_button").performScrollTo().assertIsDisplayed()
  }
}

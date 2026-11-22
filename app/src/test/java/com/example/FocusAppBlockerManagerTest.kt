package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.blocker.BlockEvaluationResult
import com.example.blocker.FocusAppBlockerManager
import com.example.data.rules.NotificationRuleRepository
import com.example.database.FocusFlowDatabase
import com.example.model.AppCategory
import com.example.model.FocusMode
import com.example.model.FocusTimerState
import com.example.model.RuleAction
import com.example.model.TimerPhase
import com.example.model.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FocusAppBlockerManagerTest {

  private lateinit var database: FocusFlowDatabase
  private lateinit var ruleRepository: NotificationRuleRepository
  private lateinit var blockerManager: FocusAppBlockerManager

  private val labelLookup =
    mapOf(
      "com.instagram.android" to "Instagram",
      "com.google.android.youtube" to "YouTube",
      "com.google.android.apps.classroom" to "Classroom",
      "com.google.android.calendar" to "Calendar",
      "com.android.settings" to "Settings",
      "com.google.android.apps.nexuslauncher" to "Pixel Launcher",
    )

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database =
      Room.inMemoryDatabaseBuilder(context, FocusFlowDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    ruleRepository =
      NotificationRuleRepository(
        ruleDao = database.appNotificationRuleDao(),
        distractionEventDao = database.distractionEventDao(),
      )
    blockerManager =
      FocusAppBlockerManager(
        ownPackageName = "com.example",
        notificationRuleRepository = ruleRepository,
      )
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `blocks silenced apps during active study mode and allows academic and safe apps`() = runBlocking {
    val runningStudy = FocusTimerState(phase = TimerPhase.RUNNING, mode = FocusMode.STUDY_MODE)
    val prefs = UserPreferences(activeFocusMode = FocusMode.STUDY_MODE)

    val blockedSocial =
      blockerManager.evaluateForegroundApp(
        foregroundPackage = "com.instagram.android",
        appLabelProvider = { labelLookup[it] ?: it },
        defaultLauncherPackage = "com.google.android.apps.nexuslauncher",
        timerState = runningStudy,
        preferences = prefs,
        nowMs = 10_000L,
      )
    assertTrue(blockedSocial is BlockEvaluationResult.BlockApp)
    assertEquals("Instagram", (blockedSocial as BlockEvaluationResult.BlockApp).appLabel)

    val logged = ruleRepository.observeRecentDistractions().first()
    assertEquals(1, logged.size)
    assertEquals(FocusAppBlockerManager.OUTCOME_APP_BLOCKED, logged.first().outcome)

    // Academic apps remain openable in Study Mode
    val allowedClassroom =
      blockerManager.evaluateForegroundApp(
        foregroundPackage = "com.google.android.apps.classroom",
        appLabelProvider = { labelLookup[it] ?: it },
        defaultLauncherPackage = "com.google.android.apps.nexuslauncher",
        timerState = runningStudy,
        preferences = prefs,
        nowMs = 10_200L,
      )
    assertEquals(BlockEvaluationResult.NoAction, allowedClassroom)

    // System settings, launcher, and FocusFlow itself are never blocked
    val allowedSettings =
      blockerManager.evaluateForegroundApp(
        foregroundPackage = "com.android.settings",
        appLabelProvider = { labelLookup[it] ?: it },
        defaultLauncherPackage = "com.google.android.apps.nexuslauncher",
        timerState = runningStudy,
        preferences = prefs,
        nowMs = 10_400L,
      )
    assertEquals(BlockEvaluationResult.NoAction, allowedSettings)
  }

  @Test
  fun `does not block when paused disabled for mode or overridden to allow`() = runBlocking {
    val pausedStudy = FocusTimerState(phase = TimerPhase.PAUSED, mode = FocusMode.STUDY_MODE)
    val runningStudy = FocusTimerState(phase = TimerPhase.RUNNING, mode = FocusMode.STUDY_MODE)
    val prefs = UserPreferences(activeFocusMode = FocusMode.STUDY_MODE)

    // Paused session never blocks
    assertEquals(
      BlockEvaluationResult.NoAction,
      blockerManager.evaluateForegroundApp(
        foregroundPackage = "com.google.android.youtube",
        appLabelProvider = { labelLookup[it] ?: it },
        defaultLauncherPackage = null,
        timerState = pausedStudy,
        preferences = prefs,
        nowMs = 20_000L,
      ),
    )

    // Disabling app blocking in Study Mode allows opening apps
    val disabledBlockingPrefs =
      prefs.copy(studyModeConfig = prefs.studyModeConfig.copy(blockSilencedApps = false))
    assertEquals(
      BlockEvaluationResult.NoAction,
      blockerManager.evaluateForegroundApp(
        foregroundPackage = "com.google.android.youtube",
        appLabelProvider = { labelLookup[it] ?: it },
        defaultLauncherPackage = null,
        timerState = runningStudy,
        preferences = disabledBlockingPrefs,
        nowMs = 21_000L,
      ),
    )

    // Explicit user ALLOW override skips blocking even when blocking is enabled
    ruleRepository.saveRule(
      packageName = "com.google.android.youtube",
      appLabel = "YouTube",
      focusMode = FocusMode.STUDY_MODE,
      category = AppCategory.ENTERTAINMENT,
      action = RuleAction.ALLOW,
    )
    assertEquals(
      BlockEvaluationResult.NoAction,
      blockerManager.evaluateForegroundApp(
        foregroundPackage = "com.google.android.youtube",
        appLabelProvider = { labelLookup[it] ?: it },
        defaultLauncherPackage = null,
        timerState = runningStudy,
        preferences = prefs,
        nowMs = 22_000L,
      ),
    )
  }

  @Test
  fun `respects cooldown and clears cooldown when returning to launcher`() = runBlocking {
    val runningStudy = FocusTimerState(phase = TimerPhase.RUNNING, mode = FocusMode.STUDY_MODE)
    val prefs = UserPreferences(activeFocusMode = FocusMode.STUDY_MODE)
    val launcher = "com.google.android.apps.nexuslauncher"

    // First open triggers block
    val firstAttempt =
      blockerManager.evaluateForegroundApp(
        foregroundPackage = "com.instagram.android",
        appLabelProvider = { labelLookup[it] ?: it },
        defaultLauncherPackage = launcher,
        timerState = runningStudy,
        preferences = prefs,
        nowMs = 30_000L,
      )
    assertTrue(firstAttempt is BlockEvaluationResult.BlockApp)

    // Immediate poll 500ms later is suppressed by cooldown
    val rapidRepeat =
      blockerManager.evaluateForegroundApp(
        foregroundPackage = "com.instagram.android",
        appLabelProvider = { labelLookup[it] ?: it },
        defaultLauncherPackage = launcher,
        timerState = runningStudy,
        preferences = prefs,
        nowMs = 30_500L,
      )
    assertEquals(BlockEvaluationResult.NoAction, rapidRepeat)

    // User returns to Home launcher, clearing cooldown immediately
    blockerManager.evaluateForegroundApp(
      foregroundPackage = launcher,
      appLabelProvider = { labelLookup[it] ?: it },
      defaultLauncherPackage = launcher,
      timerState = runningStudy,
      preferences = prefs,
      nowMs = 30_700L,
    )

    // Trying to open Instagram again right away is immediately blocked again
    val reopenAttempt =
      blockerManager.evaluateForegroundApp(
        foregroundPackage = "com.instagram.android",
        appLabelProvider = { labelLookup[it] ?: it },
        defaultLauncherPackage = launcher,
        timerState = runningStudy,
        preferences = prefs,
        nowMs = 30_900L,
      )
    assertTrue(reopenAttempt is BlockEvaluationResult.BlockApp)
  }
}

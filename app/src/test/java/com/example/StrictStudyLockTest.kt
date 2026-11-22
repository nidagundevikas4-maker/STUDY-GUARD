package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.blocker.BlockEvaluationResult
import com.example.blocker.FocusAppBlockerManager
import com.example.data.preferences.FocusPreferencesRepository
import com.example.data.rules.NotificationRuleRepository
import com.example.data.session.SessionRepository
import com.example.data.timer.FocusTimerRepository
import com.example.database.FocusFlowDatabase
import com.example.model.FocusMode
import com.example.model.FocusTimerState
import com.example.model.TimerPhase
import com.example.model.UserPreferences
import com.example.permissions.SystemPermissionReader
import com.example.ui.viewmodel.FocusTimerViewModel
import com.example.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StrictStudyLockTest {

  private val mainDispatcher = UnconfinedTestDispatcher()
  private lateinit var context: Context
  private lateinit var database: FocusFlowDatabase
  private lateinit var sessionRepository: SessionRepository
  private lateinit var ruleRepository: NotificationRuleRepository
  private lateinit var preferencesRepository: FocusPreferencesRepository
  private lateinit var timerRepository: FocusTimerRepository

  @Before
  fun setUp() {
    Dispatchers.setMain(mainDispatcher)
    context = ApplicationProvider.getApplicationContext()
    database =
      Room.inMemoryDatabaseBuilder(context, FocusFlowDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    sessionRepository =
      SessionRepository(
        sessionDao = database.focusSessionDao(),
        distractionEventDao = database.distractionEventDao(),
      )
    ruleRepository =
      NotificationRuleRepository(
        ruleDao = database.appNotificationRuleDao(),
        distractionEventDao = database.distractionEventDao(),
      )
    preferencesRepository = FocusPreferencesRepository(context)
    timerRepository = FocusTimerRepository(sessionRepository)
  }

  @After
  fun tearDown() {
    database.close()
    Dispatchers.resetMain()
  }

  @Test
  fun `repository blocks pause and normal reset during strict locked session until force unlock`() {
    timerRepository.onTimerStarted(
      mode = FocusMode.STUDY_MODE,
      durationMinutes = 45,
      isStrictLocked = true,
    )

    assertEquals(TimerPhase.RUNNING, timerRepository.timerState.value.phase)
    assertTrue(timerRepository.timerState.value.isStrictLocked)

    // Attempt pause -> ignored
    timerRepository.onTimerPaused(remainingSeconds = 2600)
    assertEquals(TimerPhase.RUNNING, timerRepository.timerState.value.phase)

    // Attempt normal reset -> ignored
    timerRepository.onTimerReset(
      mode = FocusMode.STUDY_MODE,
      durationMinutes = 45,
      elapsedSeconds = 100,
      forceUnlock = false,
    )
    assertEquals(TimerPhase.RUNNING, timerRepository.timerState.value.phase)

    // Emergency force unlock succeeds
    timerRepository.onTimerReset(
      mode = FocusMode.STUDY_MODE,
      durationMinutes = 45,
      elapsedSeconds = 100,
      forceUnlock = true,
    )
    assertEquals(TimerPhase.IDLE, timerRepository.timerState.value.phase)
    assertFalse(timerRepository.timerState.value.isStrictLocked)
  }

  @Test
  fun `non strict session still permits pause and normal reset`() {
    timerRepository.onTimerStarted(
      mode = FocusMode.LIGHT_FOCUS,
      durationMinutes = 20,
      isStrictLocked = false,
    )

    timerRepository.onTimerPaused(remainingSeconds = 1100)
    assertEquals(TimerPhase.PAUSED, timerRepository.timerState.value.phase)

    timerRepository.onTimerReset(
      mode = FocusMode.LIGHT_FOCUS,
      durationMinutes = 20,
      elapsedSeconds = 100,
    )
    assertEquals(TimerPhase.IDLE, timerRepository.timerState.value.phase)
  }

  @Test
  fun `blocker enforces lock even if timer state somehow pauses when strict locked`() = runBlocking {
    val blockerManager =
      FocusAppBlockerManager(
        ownPackageName = context.packageName,
        notificationRuleRepository = ruleRepository,
      )
    val lockedState =
      FocusTimerState(
        phase = TimerPhase.PAUSED,
        mode = FocusMode.STUDY_MODE,
        isStrictLocked = true,
      )

    val result =
      blockerManager.evaluateForegroundApp(
        foregroundPackage = "com.instagram.android",
        appLabelProvider = { "Instagram" },
        defaultLauncherPackage = "com.google.android.apps.nexuslauncher",
        timerState = lockedState,
        preferences = UserPreferences(activeFocusMode = FocusMode.STUDY_MODE),
        nowMs = 50_000L,
      )

    assertTrue(result is BlockEvaluationResult.BlockApp)
  }

  @Test
  fun `viewmodels block mode or duration tampering during active session and require UNLOCK phrase`() = runBlocking {
    preferencesRepository.setActiveFocusMode(FocusMode.STUDY_MODE)
    preferencesRepository.setSelectedDurationMinutes(25)

    val homeViewModel =
      HomeViewModel(
        preferencesRepository = preferencesRepository,
        permissionReader = SystemPermissionReader(context),
        timerRepository = timerRepository,
        sessionRepository = sessionRepository,
      )
    val timerViewModel =
      FocusTimerViewModel(
        appContext = context,
        preferencesRepository = preferencesRepository,
        timerRepository = timerRepository,
        unlockCooldownSeconds = 0,
      )

    // Wait for initial preferences (STUDY_MODE, 25m) on both ViewModels
    val initialHome =
      withTimeout(5000) {
        homeViewModel.uiState.first {
          it.activeFocusMode == FocusMode.STUDY_MODE && it.selectedDurationMinutes == 25
        }
      }
    assertEquals(25, initialHome.selectedDurationMinutes)
    withTimeout(5000) {
      timerViewModel.uiState.first {
        it.selectedMode == FocusMode.STUDY_MODE && it.configuredDurationMinutes == 25
      }
    }

    // Start strict-locked Study Mode session
    timerRepository.onTimerStarted(
      mode = FocusMode.STUDY_MODE,
      durationMinutes = 25,
      isStrictLocked = true,
    )

    // Attempt to switch mode to LIGHT_FOCUS or shorten duration to 5m while locked
    homeViewModel.selectFocusMode(FocusMode.LIGHT_FOCUS)
    homeViewModel.selectDurationMinutes(5)
    timerViewModel.selectDurationMinutes(5)

    val lockedTimerState =
      withTimeout(5000) {
        timerViewModel.uiState.first { it.timerState.isStrictLocked }
      }
    assertEquals(FocusMode.STUDY_MODE, lockedTimerState.selectedMode)
    assertEquals(25, lockedTimerState.configuredDurationMinutes)

    // Emergency unlock requires exact phrase
    timerViewModel.openEmergencyUnlockDialog()
    val openedState =
      withTimeout(5000) {
        timerViewModel.uiState.first { it.isEmergencyUnlockDialogOpen }
      }
    assertFalse(openedState.canConfirmEmergencyUnlock)

    timerViewModel.updateEmergencyUnlockPhrase("quit")
    assertFalse(timerViewModel.uiState.value.canConfirmEmergencyUnlock)

    timerViewModel.updateEmergencyUnlockPhrase("unlock")
    val readyState =
      withTimeout(5000) {
        timerViewModel.uiState.first { it.canConfirmEmergencyUnlock }
      }
    assertTrue(readyState.canConfirmEmergencyUnlock)
  }
}

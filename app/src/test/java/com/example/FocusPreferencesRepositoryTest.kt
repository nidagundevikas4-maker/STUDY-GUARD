package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.FocusPreferencesRepository
import com.example.model.FocusMode
import com.example.model.ThemePreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FocusPreferencesRepositoryTest {

  private lateinit var repository: FocusPreferencesRepository

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    repository = FocusPreferencesRepository(context)
  }

  @Test
  fun `persists mode selection duration goal and theme`() = runBlocking {
    repository.setActiveFocusMode(FocusMode.DEEP_FOCUS)
    repository.setSelectedDurationMinutes(60)
    repository.setDailyGoalMinutes(180)
    repository.setThemePreference(ThemePreference.DARK)

    val prefs = repository.preferencesFlow.first()
    assertEquals(FocusMode.DEEP_FOCUS, prefs.activeFocusMode)
    assertEquals(60, prefs.selectedDurationMinutes)
    assertEquals(180, prefs.dailyGoalMinutes)
    assertEquals(ThemePreference.DARK, prefs.themePreference)
  }

  @Test
  fun `switching active mode resets selected duration to mode preset`() = runBlocking {
    repository.setSelectedDurationMinutes(75)
    repository.setActiveFocusMode(FocusMode.LIGHT_FOCUS)

    val prefs = repository.preferencesFlow.first()
    assertEquals(FocusMode.LIGHT_FOCUS, prefs.activeFocusMode)
    assertEquals(FocusMode.LIGHT_FOCUS.defaultDurationMinutes, prefs.selectedDurationMinutes)
  }

  @Test
  fun `persists per mode auto dnd academic priority and app blocking flags`() = runBlocking {
    repository.updateModeAutoDnd(FocusMode.LIGHT_FOCUS, true)
    repository.updateDeepFocusAlarmsOnlyDnd(true)
    repository.updateModeAcademicPriority(FocusMode.DEEP_FOCUS, false)
    repository.updateModeBlockApps(FocusMode.LIGHT_FOCUS, true)
    repository.updateModeBlockApps(FocusMode.STUDY_MODE, false)

    val prefs = repository.preferencesFlow.first()
    assertTrue(prefs.lightFocusConfig.autoEnableDnd)
    assertTrue(prefs.deepFocusConfig.strictAlarmsOnlyDnd)
    assertFalse(prefs.deepFocusConfig.prioritizeAcademicNotifications)
    assertTrue(prefs.lightFocusConfig.blockSilencedApps)
    assertFalse(prefs.studyModeConfig.blockSilencedApps)
  }
}

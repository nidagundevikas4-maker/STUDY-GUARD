package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.FocusMode
import com.example.model.FocusModeConfig
import com.example.model.PersistedActiveSession
import com.example.model.ThemePreference
import com.example.model.UserPreferences
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.focusDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "focusflow_preferences")

class FocusPreferencesRepository(context: Context) {

  private val dataStore = context.applicationContext.focusDataStore

  private object Keys {
    val ACTIVE_FOCUS_MODE = stringPreferencesKey("active_focus_mode")
    val DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
    val SELECTED_DURATION_MINUTES = intPreferencesKey("selected_duration_minutes")
    val THEME_PREFERENCE = stringPreferencesKey("theme_preference")

    val DEEP_AUTO_DND = booleanPreferencesKey("deep_auto_dnd")
    val DEEP_STRICT_ALARMS_DND = booleanPreferencesKey("deep_strict_alarms_dnd")
    val DEEP_ACADEMIC_PRIORITY = booleanPreferencesKey("deep_academic_priority")
    val DEEP_BLOCK_APPS = booleanPreferencesKey("deep_block_apps")
    val DEEP_STRICT_LOCK = booleanPreferencesKey("deep_strict_lock")

    val STUDY_AUTO_DND = booleanPreferencesKey("study_auto_dnd")
    val STUDY_ACADEMIC_PRIORITY = booleanPreferencesKey("study_academic_priority")
    val STUDY_BLOCK_APPS = booleanPreferencesKey("study_block_apps")
    val STUDY_STRICT_LOCK = booleanPreferencesKey("study_strict_lock")

    val LIGHT_AUTO_DND = booleanPreferencesKey("light_auto_dnd")
    val LIGHT_ACADEMIC_PRIORITY = booleanPreferencesKey("light_academic_priority")
    val LIGHT_BLOCK_APPS = booleanPreferencesKey("light_block_apps")
    val LIGHT_STRICT_LOCK = booleanPreferencesKey("light_strict_lock")

    val ACTIVE_SESSION_MODE = stringPreferencesKey("active_session_mode")
    val ACTIVE_SESSION_PLANNED_MINUTES = intPreferencesKey("active_session_planned_minutes")
    val ACTIVE_SESSION_STRICT_LOCK = booleanPreferencesKey("active_session_strict_lock")
    val ACTIVE_SESSION_STARTED_AT_MS = longPreferencesKey("active_session_started_at_ms")
    val ACTIVE_SESSION_END_EPOCH_MS = longPreferencesKey("active_session_end_epoch_ms")
    val ACTIVE_SESSION_END_ELAPSED_MS = longPreferencesKey("active_session_end_elapsed_ms")
    val ACTIVE_SESSION_BOOT_MARKER_MS = longPreferencesKey("active_session_boot_marker_ms")
  }

  val preferencesFlow: Flow<UserPreferences> =
    dataStore.data
      .catch { exception ->
        if (exception is IOException) {
          emit(emptyPreferences())
        } else {
          throw exception
        }
      }
      .map { prefs ->
        val mode =
          prefs[Keys.ACTIVE_FOCUS_MODE]?.let { raw ->
            FocusMode.entries.firstOrNull { it.name == raw }
          } ?: FocusMode.STUDY_MODE

        val theme =
          prefs[Keys.THEME_PREFERENCE]?.let { raw ->
            ThemePreference.entries.firstOrNull { it.name == raw }
          } ?: ThemePreference.SYSTEM

        val dailyGoal = (prefs[Keys.DAILY_GOAL_MINUTES] ?: 120).coerceIn(30, 360)
        val duration =
          (prefs[Keys.SELECTED_DURATION_MINUTES] ?: mode.defaultDurationMinutes).coerceIn(5, 180)

        UserPreferences(
          activeFocusMode = mode,
          dailyGoalMinutes = dailyGoal,
          selectedDurationMinutes = duration,
          themePreference = theme,
          deepFocusConfig =
            FocusModeConfig(
              autoEnableDnd = prefs[Keys.DEEP_AUTO_DND] ?: true,
              strictAlarmsOnlyDnd = prefs[Keys.DEEP_STRICT_ALARMS_DND] ?: false,
              prioritizeAcademicNotifications = prefs[Keys.DEEP_ACADEMIC_PRIORITY] ?: true,
              blockSilencedApps = prefs[Keys.DEEP_BLOCK_APPS] ?: true,
              strictSessionLock = prefs[Keys.DEEP_STRICT_LOCK] ?: true,
            ),
          studyModeConfig =
            FocusModeConfig(
              autoEnableDnd = prefs[Keys.STUDY_AUTO_DND] ?: true,
              strictAlarmsOnlyDnd = false,
              prioritizeAcademicNotifications = prefs[Keys.STUDY_ACADEMIC_PRIORITY] ?: true,
              blockSilencedApps = prefs[Keys.STUDY_BLOCK_APPS] ?: true,
              strictSessionLock = prefs[Keys.STUDY_STRICT_LOCK] ?: true,
            ),
          lightFocusConfig =
            FocusModeConfig(
              autoEnableDnd = prefs[Keys.LIGHT_AUTO_DND] ?: false,
              strictAlarmsOnlyDnd = false,
              prioritizeAcademicNotifications = prefs[Keys.LIGHT_ACADEMIC_PRIORITY] ?: true,
              blockSilencedApps = prefs[Keys.LIGHT_BLOCK_APPS] ?: false,
              strictSessionLock = prefs[Keys.LIGHT_STRICT_LOCK] ?: false,
            ),
        )
      }

  val persistedActiveSessionFlow: Flow<PersistedActiveSession?> =
    dataStore.data
      .catch { exception ->
        if (exception is IOException) {
          emit(emptyPreferences())
        } else {
          throw exception
        }
      }
      .map { prefs ->
        val modeName = prefs[Keys.ACTIVE_SESSION_MODE] ?: return@map null
        val mode = FocusMode.entries.firstOrNull { it.name == modeName } ?: return@map null
        val plannedMinutes = prefs[Keys.ACTIVE_SESSION_PLANNED_MINUTES] ?: return@map null
        val startedAt = prefs[Keys.ACTIVE_SESSION_STARTED_AT_MS] ?: 0L
        val endEpochMs = prefs[Keys.ACTIVE_SESSION_END_EPOCH_MS] ?: return@map null
        val endElapsedMs = prefs[Keys.ACTIVE_SESSION_END_ELAPSED_MS] ?: 0L
        val bootMarkerMs = prefs[Keys.ACTIVE_SESSION_BOOT_MARKER_MS] ?: 0L
        val isStrictLocked = prefs[Keys.ACTIVE_SESSION_STRICT_LOCK] ?: false

        PersistedActiveSession(
          mode = mode,
          plannedMinutes = plannedMinutes,
          isStrictLocked = isStrictLocked,
          startedAtEpochMs = startedAt,
          endEpochMs = endEpochMs,
          endElapsedRealtimeMs = endElapsedMs,
          bootTimeEstimateMs = bootMarkerMs,
        )
      }

  suspend fun saveActiveSession(session: PersistedActiveSession) {
    dataStore.edit { prefs ->
      prefs[Keys.ACTIVE_SESSION_MODE] = session.mode.name
      prefs[Keys.ACTIVE_SESSION_PLANNED_MINUTES] = session.plannedMinutes
      prefs[Keys.ACTIVE_SESSION_STRICT_LOCK] = session.isStrictLocked
      prefs[Keys.ACTIVE_SESSION_STARTED_AT_MS] = session.startedAtEpochMs
      prefs[Keys.ACTIVE_SESSION_END_EPOCH_MS] = session.endEpochMs
      prefs[Keys.ACTIVE_SESSION_END_ELAPSED_MS] = session.endElapsedRealtimeMs
      prefs[Keys.ACTIVE_SESSION_BOOT_MARKER_MS] = session.bootTimeEstimateMs
    }
  }

  suspend fun clearActiveSession() {
    dataStore.edit { prefs ->
      prefs.remove(Keys.ACTIVE_SESSION_MODE)
      prefs.remove(Keys.ACTIVE_SESSION_PLANNED_MINUTES)
      prefs.remove(Keys.ACTIVE_SESSION_STRICT_LOCK)
      prefs.remove(Keys.ACTIVE_SESSION_STARTED_AT_MS)
      prefs.remove(Keys.ACTIVE_SESSION_END_EPOCH_MS)
      prefs.remove(Keys.ACTIVE_SESSION_END_ELAPSED_MS)
      prefs.remove(Keys.ACTIVE_SESSION_BOOT_MARKER_MS)
    }
  }

  suspend fun setActiveFocusMode(mode: FocusMode) {
    dataStore.edit { prefs ->
      prefs[Keys.ACTIVE_FOCUS_MODE] = mode.name
      prefs[Keys.SELECTED_DURATION_MINUTES] = mode.defaultDurationMinutes
    }
  }

  suspend fun setDailyGoalMinutes(minutes: Int) {
    dataStore.edit { prefs ->
      prefs[Keys.DAILY_GOAL_MINUTES] = minutes.coerceIn(30, 360)
    }
  }

  suspend fun setSelectedDurationMinutes(minutes: Int) {
    dataStore.edit { prefs ->
      prefs[Keys.SELECTED_DURATION_MINUTES] = minutes.coerceIn(5, 180)
    }
  }

  suspend fun setThemePreference(theme: ThemePreference) {
    dataStore.edit { prefs ->
      prefs[Keys.THEME_PREFERENCE] = theme.name
    }
  }

  suspend fun updateModeAutoDnd(mode: FocusMode, enabled: Boolean) {
    dataStore.edit { prefs ->
      when (mode) {
        FocusMode.DEEP_FOCUS -> prefs[Keys.DEEP_AUTO_DND] = enabled
        FocusMode.STUDY_MODE -> prefs[Keys.STUDY_AUTO_DND] = enabled
        FocusMode.LIGHT_FOCUS -> prefs[Keys.LIGHT_AUTO_DND] = enabled
      }
    }
  }

  suspend fun updateDeepFocusAlarmsOnlyDnd(enabled: Boolean) {
    dataStore.edit { prefs ->
      prefs[Keys.DEEP_STRICT_ALARMS_DND] = enabled
    }
  }

  suspend fun updateModeAcademicPriority(mode: FocusMode, enabled: Boolean) {
    dataStore.edit { prefs ->
      when (mode) {
        FocusMode.DEEP_FOCUS -> prefs[Keys.DEEP_ACADEMIC_PRIORITY] = enabled
        FocusMode.STUDY_MODE -> prefs[Keys.STUDY_ACADEMIC_PRIORITY] = enabled
        FocusMode.LIGHT_FOCUS -> prefs[Keys.LIGHT_ACADEMIC_PRIORITY] = enabled
      }
    }
  }

  suspend fun updateModeBlockApps(mode: FocusMode, enabled: Boolean) {
    dataStore.edit { prefs ->
      when (mode) {
        FocusMode.DEEP_FOCUS -> prefs[Keys.DEEP_BLOCK_APPS] = enabled
        FocusMode.STUDY_MODE -> prefs[Keys.STUDY_BLOCK_APPS] = enabled
        FocusMode.LIGHT_FOCUS -> prefs[Keys.LIGHT_BLOCK_APPS] = enabled
      }
    }
  }

  suspend fun updateModeStrictLock(mode: FocusMode, enabled: Boolean) {
    dataStore.edit { prefs ->
      when (mode) {
        FocusMode.DEEP_FOCUS -> prefs[Keys.DEEP_STRICT_LOCK] = enabled
        FocusMode.STUDY_MODE -> prefs[Keys.STUDY_STRICT_LOCK] = enabled
        FocusMode.LIGHT_FOCUS -> prefs[Keys.LIGHT_STRICT_LOCK] = enabled
      }
    }
  }
}

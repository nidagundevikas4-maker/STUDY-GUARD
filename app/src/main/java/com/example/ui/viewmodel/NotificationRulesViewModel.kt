package com.example.ui.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.FocusPreferencesRepository
import com.example.data.rules.InstalledAppInfo
import com.example.data.rules.InstalledAppsRepository
import com.example.data.rules.NotificationRuleRepository
import com.example.data.timer.FocusTimerRepository
import com.example.model.AcademicAppClassifier
import com.example.model.AppCategory
import com.example.model.DistractionEventRecord
import com.example.model.FocusMode
import com.example.model.FocusTimerState
import com.example.model.RuleAction
import com.example.model.UserPreferences
import com.example.permissions.SystemPermissionReader
import com.example.permissions.SystemPermissionSnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RuleFilter(val label: String) {
  ALL("All"),
  ACADEMIC("Academic"),
  ALLOWED("Allowed"),
  SILENCED("Silenced"),
}

data class AppRuleItem(
  val packageName: String,
  val appLabel: String,
  val category: AppCategory,
  val icon: ImageBitmap?,
  val action: RuleAction,
  val isUserCustomized: Boolean,
)

data class NotificationRulesUiState(
  val selectedMode: FocusMode = FocusMode.STUDY_MODE,
  val preferences: UserPreferences = UserPreferences(),
  val permissionSnapshot: SystemPermissionSnapshot = SystemPermissionSnapshot(),
  val searchQuery: String = "",
  val activeFilter: RuleFilter = RuleFilter.ALL,
  val isLoadingApps: Boolean = true,
  val appRules: List<AppRuleItem> = emptyList(),
  val allowedCount: Int = 0,
  val silencedCount: Int = 0,
  val hasCustomizedRulesInMode: Boolean = false,
  val recentDistractions: List<DistractionEventRecord> = emptyList(),
  val isSessionActive: Boolean = false,
  val isSessionLocked: Boolean = false,
  val activeRemainingSeconds: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationRulesViewModel(
  private val preferencesRepository: FocusPreferencesRepository,
  private val installedAppsRepository: InstalledAppsRepository,
  private val notificationRuleRepository: NotificationRuleRepository,
  private val permissionReader: SystemPermissionReader,
  private val timerRepository: FocusTimerRepository,
) : ViewModel() {

  private val installedAppsFlow = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
  private val isLoadingAppsFlow = MutableStateFlow(true)
  private val searchQueryFlow = MutableStateFlow("")
  private val filterFlow = MutableStateFlow(RuleFilter.ALL)
  private val permissionSnapshotFlow = MutableStateFlow(permissionReader.readSnapshot())

  private val rulesForActiveModeFlow =
    preferencesRepository.preferencesFlow.flatMapLatest { prefs ->
      notificationRuleRepository.observeRulesForMode(prefs.activeFocusMode)
    }

  val uiState: StateFlow<NotificationRulesUiState> =
    combine(
      preferencesRepository.preferencesFlow,
      installedAppsFlow,
      isLoadingAppsFlow,
      rulesForActiveModeFlow,
      searchQueryFlow,
      filterFlow,
      permissionSnapshotFlow,
      notificationRuleRepository.observeRecentDistractions(),
      timerRepository.timerState,
    ) { values ->
      val prefs = values[0] as UserPreferences
      @Suppress("UNCHECKED_CAST")
      val installedApps = values[1] as List<InstalledAppInfo>
      val isLoading = values[2] as Boolean
      @Suppress("UNCHECKED_CAST")
      val savedRulesMap =
        values[3] as Map<String, com.example.database.entities.AppNotificationRuleEntity>
      val query = values[4] as String
      val filter = values[5] as RuleFilter
      val permissions = values[6] as SystemPermissionSnapshot
      @Suppress("UNCHECKED_CAST")
      val distractions = values[7] as List<DistractionEventRecord>
      val timerState = values[8] as FocusTimerState

      val mode = prefs.activeFocusMode
      val modeConfig = prefs.configFor(mode)

      val allMapped =
        installedApps.map { app ->
          val saved = savedRulesMap[app.packageName]
          val effectiveAction =
            if (saved != null) {
              RuleAction.entries.firstOrNull { it.name == saved.action } ?: RuleAction.ALLOW
            } else if (app.category == AppCategory.ACADEMIC && modeConfig.prioritizeAcademicNotifications) {
              RuleAction.ALLOW
            } else if (app.category == AppCategory.ACADEMIC && !modeConfig.prioritizeAcademicNotifications && mode == FocusMode.DEEP_FOCUS) {
              RuleAction.SILENCE
            } else {
              AcademicAppClassifier.defaultActionFor(app.category, mode)
            }

          AppRuleItem(
            packageName = app.packageName,
            appLabel = app.appLabel,
            category = app.category,
            icon = app.icon,
            action = effectiveAction,
            isUserCustomized = saved?.isUserCustomized == true,
          )
        }

      val normalizedQuery = query.trim().lowercase()
      val filtered =
        allMapped.filter { item ->
          val matchesSearch =
            normalizedQuery.isEmpty() ||
              item.appLabel.lowercase().contains(normalizedQuery) ||
              item.packageName.lowercase().contains(normalizedQuery)

          val matchesFilter =
            when (filter) {
              RuleFilter.ALL -> true
              RuleFilter.ACADEMIC -> item.category == AppCategory.ACADEMIC
              RuleFilter.ALLOWED -> item.action == RuleAction.ALLOW
              RuleFilter.SILENCED -> item.action == RuleAction.SILENCE
            }

          matchesSearch && matchesFilter
        }

      NotificationRulesUiState(
        selectedMode = mode,
        preferences = prefs,
        permissionSnapshot = permissions,
        searchQuery = query,
        activeFilter = filter,
        isLoadingApps = isLoading,
        appRules = filtered,
        allowedCount = allMapped.count { it.action == RuleAction.ALLOW },
        silencedCount = allMapped.count { it.action == RuleAction.SILENCE },
        hasCustomizedRulesInMode = allMapped.any { it.isUserCustomized },
        recentDistractions = distractions,
        isSessionActive = timerState.isActive,
        isSessionLocked = timerState.isActive && timerState.isStrictLocked,
        activeRemainingSeconds = timerState.remainingSeconds,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = NotificationRulesUiState(),
    )

  init {
    refreshInstalledApps()
  }

  fun refreshInstalledApps() {
    viewModelScope.launch {
      isLoadingAppsFlow.value = true
      installedAppsFlow.value = installedAppsRepository.loadLaunchableApps()
      isLoadingAppsFlow.value = false
    }
  }

  fun refreshPermissions() {
    permissionSnapshotFlow.value = permissionReader.readSnapshot()
  }

  fun selectMode(mode: FocusMode) {
    if (timerRepository.timerState.value.isActive) return
    viewModelScope.launch { preferencesRepository.setActiveFocusMode(mode) }
  }

  fun updateSearchQuery(query: String) {
    searchQueryFlow.value = query
  }

  fun selectFilter(filter: RuleFilter) {
    filterFlow.value = filter
  }

  fun toggleAppAction(item: AppRuleItem) {
    val timer = timerRepository.timerState.value
    if (timer.isActive && timer.isStrictLocked) return
    val nextAction =
      if (item.action == RuleAction.ALLOW) RuleAction.SILENCE else RuleAction.ALLOW
    val mode = uiState.value.selectedMode
    viewModelScope.launch {
      notificationRuleRepository.saveRule(
        packageName = item.packageName,
        appLabel = item.appLabel,
        focusMode = mode,
        category = item.category,
        action = nextAction,
      )
    }
  }

  fun resetModeRulesToDefaults() {
    val timer = timerRepository.timerState.value
    if (timer.isActive && timer.isStrictLocked) return
    val mode = uiState.value.selectedMode
    viewModelScope.launch { notificationRuleRepository.resetRulesForMode(mode) }
  }

  class Factory(
    private val preferencesRepository: FocusPreferencesRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val notificationRuleRepository: NotificationRuleRepository,
    private val permissionReader: SystemPermissionReader,
    private val timerRepository: FocusTimerRepository,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      require(modelClass.isAssignableFrom(NotificationRulesViewModel::class.java)) {
        "Unknown ViewModel class: ${modelClass.name}"
      }
      return NotificationRulesViewModel(
        preferencesRepository = preferencesRepository,
        installedAppsRepository = installedAppsRepository,
        notificationRuleRepository = notificationRuleRepository,
        permissionReader = permissionReader,
        timerRepository = timerRepository,
      ) as T
    }
  }
}

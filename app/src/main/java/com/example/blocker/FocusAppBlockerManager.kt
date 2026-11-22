package com.example.blocker

import com.example.data.rules.NotificationRuleRepository
import com.example.model.FocusMode
import com.example.model.FocusTimerState
import com.example.model.RuleAction
import com.example.model.TimerPhase
import com.example.model.UserPreferences

sealed interface BlockEvaluationResult {
  data object NoAction : BlockEvaluationResult

  data class BlockApp(
    val packageName: String,
    val appLabel: String,
    val focusMode: FocusMode,
  ) : BlockEvaluationResult
}

class FocusAppBlockerManager(
  private val ownPackageName: String,
  private val notificationRuleRepository: NotificationRuleRepository,
) {

  private var lastBlockedPackage: String? = null
  private var lastBlockedAtMs: Long = 0L

  suspend fun evaluateForegroundApp(
    foregroundPackage: String?,
    appLabelProvider: (String) -> String,
    defaultLauncherPackage: String?,
    timerState: FocusTimerState,
    preferences: UserPreferences,
    nowMs: Long = System.currentTimeMillis(),
  ): BlockEvaluationResult {
    if (foregroundPackage.isNullOrBlank()) return BlockEvaluationResult.NoAction
    val shouldEnforceBlock =
      timerState.phase == TimerPhase.RUNNING || (timerState.isStrictLocked && timerState.isActive)
    if (!shouldEnforceBlock) return BlockEvaluationResult.NoAction

    val mode = timerState.mode
    val modeConfig = preferences.configFor(mode)
    if (!modeConfig.blockSilencedApps) return BlockEvaluationResult.NoAction

    if (isSystemSafePackage(foregroundPackage, defaultLauncherPackage)) {
      if (foregroundPackage == ownPackageName || foregroundPackage == defaultLauncherPackage) {
        // User returned to Study Guard or Home launcher; clear last blocked state so re-opening is caught immediately
        lastBlockedPackage = null
      }
      return BlockEvaluationResult.NoAction
    }

    val appLabel = appLabelProvider(foregroundPackage)
    val rule =
      notificationRuleRepository.resolveEffectiveRule(
        packageName = foregroundPackage,
        appLabel = appLabel,
        focusMode = mode,
        academicPriorityEnabled = modeConfig.prioritizeAcademicNotifications,
      )

    if (rule.action != RuleAction.SILENCE) {
      return BlockEvaluationResult.NoAction
    }

    val withinCooldown =
      lastBlockedPackage == foregroundPackage && (nowMs - lastBlockedAtMs) < BLOCK_COOLDOWN_MS
    if (withinCooldown) {
      return BlockEvaluationResult.NoAction
    }

    lastBlockedPackage = foregroundPackage
    lastBlockedAtMs = nowMs

    notificationRuleRepository.logDistractionEvent(
      packageName = foregroundPackage,
      appLabel = appLabel,
      focusMode = mode,
      outcome = OUTCOME_APP_BLOCKED,
    )

    return BlockEvaluationResult.BlockApp(
      packageName = foregroundPackage,
      appLabel = appLabel,
      focusMode = mode,
    )
  }

  fun resetCooldown() {
    lastBlockedPackage = null
    lastBlockedAtMs = 0L
  }

  private fun isSystemSafePackage(
    packageName: String,
    defaultLauncherPackage: String?,
  ): Boolean {
    if (packageName == ownPackageName) return true
    if (!defaultLauncherPackage.isNullOrBlank() && packageName == defaultLauncherPackage) return true
    return ALWAYS_ALLOWED_SYSTEM_PACKAGES.contains(packageName)
  }

  companion object {
    const val OUTCOME_APP_BLOCKED = "APP_BLOCKED"
    private const val BLOCK_COOLDOWN_MS = 1_500L

    private val ALWAYS_ALLOWED_SYSTEM_PACKAGES =
      setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.incallui",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.emergency",
      )
  }
}

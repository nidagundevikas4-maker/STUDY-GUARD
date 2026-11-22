package com.example.notification

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.FocusFlowApplication
import com.example.model.RuleAction
import com.example.model.TimerPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FocusNotificationListenerService : NotificationListenerService() {

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val appContainer by lazy { (application as FocusFlowApplication).appContainer }

  override fun onListenerConnected() {
    super.onListenerConnected()
    _isListenerBound.value = true
  }

  override fun onListenerDisconnected() {
    _isListenerBound.value = false
    super.onListenerDisconnected()
  }

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    if (sbn == null) return

    // Never interfere with FocusFlow's own timer notifications, system alerts, alarms, or active calls.
    if (sbn.packageName == packageName) return
    if (sbn.isOngoing) return

    val category = sbn.notification.category
    if (
      category == Notification.CATEGORY_CALL ||
        category == Notification.CATEGORY_ALARM ||
        category == Notification.CATEGORY_SYSTEM ||
        category == Notification.CATEGORY_TRANSPORT
    ) {
      return
    }

    val timerState = appContainer.focusTimerRepository.timerState.value
    if (timerState.phase != TimerPhase.RUNNING) return

    val pkg = sbn.packageName
    val sbnKey = sbn.key
    val activeMode = timerState.mode

    serviceScope.launch {
      val appLabel = resolveAppLabel(pkg)
      val prefs = appContainer.preferencesRepository.preferencesFlow.first()
      val modeConfig = prefs.configFor(activeMode)

      val resolved =
        appContainer.notificationRuleRepository.resolveEffectiveRule(
          packageName = pkg,
          appLabel = appLabel,
          focusMode = activeMode,
          academicPriorityEnabled = modeConfig.prioritizeAcademicNotifications,
        )

      if (resolved.action == RuleAction.SILENCE) {
        try {
          cancelNotification(sbnKey)
          appContainer.focusTimerRepository.incrementDismissedNotificationCount()
          appContainer.notificationRuleRepository.logDistractionEvent(
            packageName = pkg,
            appLabel = appLabel,
            focusMode = activeMode,
            outcome = "DISMISSED",
          )
        } catch (_: SecurityException) {
          // Listener access was revoked concurrently; fail safely.
        }
      }
    }
  }

  private fun resolveAppLabel(packageName: String): String =
    try {
      val appInfo = packageManager.getApplicationInfo(packageName, 0)
      packageManager.getApplicationLabel(appInfo).toString()
    } catch (_: PackageManager.NameNotFoundException) {
      packageName
    }

  override fun onDestroy() {
    _isListenerBound.value = false
    serviceScope.cancel()
    super.onDestroy()
  }

  companion object {
    private val _isListenerBound = MutableStateFlow(false)
    val isListenerBound: StateFlow<Boolean> = _isListenerBound.asStateFlow()
  }
}

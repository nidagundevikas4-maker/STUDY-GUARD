package com.example.notification

import android.app.NotificationManager
import android.content.Context
import com.example.model.FocusMode
import com.example.model.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DndPolicyManager(context: Context) {

  private val notificationManager =
    context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

  private var savedPreviousFilter: Int? = null

  private val _isDndManagedByFocusFlow = MutableStateFlow(false)
  val isDndManagedByFocusFlow: StateFlow<Boolean> = _isDndManagedByFocusFlow.asStateFlow()

  fun isPolicyAccessGranted(): Boolean =
    notificationManager?.isNotificationPolicyAccessGranted == true

  fun applyDndForSession(mode: FocusMode, preferences: UserPreferences) {
    val manager = notificationManager ?: return
    if (!manager.isNotificationPolicyAccessGranted) return

    val modeConfig = preferences.configFor(mode)
    if (!modeConfig.autoEnableDnd) {
      restorePreviousDndIfManaged()
      return
    }

    if (savedPreviousFilter == null) {
      savedPreviousFilter = manager.currentInterruptionFilter
    }

    val targetFilter =
      if (mode == FocusMode.DEEP_FOCUS && modeConfig.strictAlarmsOnlyDnd) {
        NotificationManager.INTERRUPTION_FILTER_ALARMS
      } else {
        NotificationManager.INTERRUPTION_FILTER_PRIORITY
      }

    try {
      manager.setInterruptionFilter(targetFilter)
      _isDndManagedByFocusFlow.value = true
    } catch (_: SecurityException) {
      savedPreviousFilter = null
      _isDndManagedByFocusFlow.value = false
    }
  }

  fun restorePreviousDndIfManaged() {
    val manager = notificationManager ?: return
    val priorFilter = savedPreviousFilter ?: return
    savedPreviousFilter = null
    _isDndManagedByFocusFlow.value = false

    if (!manager.isNotificationPolicyAccessGranted) return
    try {
      manager.setInterruptionFilter(priorFilter)
    } catch (_: SecurityException) {
      // Permission was revoked while focusing; leave system state untouched.
    }
  }
}

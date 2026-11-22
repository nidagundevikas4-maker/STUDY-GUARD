package com.example.permissions

import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.example.notification.FocusNotificationListenerService

data class SystemPermissionSnapshot(
  val notificationAccessGranted: Boolean = false,
  val notificationListenerBound: Boolean = false,
  val dndAccessGranted: Boolean = false,
  val usageAccessGranted: Boolean = false,
  val overlayPermissionGranted: Boolean = false,
) {
  val appBlockerReady: Boolean
    get() = usageAccessGranted && overlayPermissionGranted
}

class SystemPermissionReader(private val context: Context) {

  fun readSnapshot(): SystemPermissionSnapshot {
    val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
    val hasNotificationListener = enabledPackages.contains(context.packageName)
    val listenerBound = FocusNotificationListenerService.isListenerBound.value

    val notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    val hasDndAccess = notificationManager?.isNotificationPolicyAccessGranted == true

    val hasUsageAccess = hasUsageStatsPermission()
    val hasOverlayPermission = Settings.canDrawOverlays(context)

    return SystemPermissionSnapshot(
      notificationAccessGranted = hasNotificationListener,
      notificationListenerBound = listenerBound,
      dndAccessGranted = hasDndAccess,
      usageAccessGranted = hasUsageAccess,
      overlayPermissionGranted = hasOverlayPermission,
    )
  }

  private fun hasUsageStatsPermission(): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
    val mode =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
          AppOpsManager.OPSTR_GET_USAGE_STATS,
          Process.myUid(),
          context.packageName,
        )
      } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
          AppOpsManager.OPSTR_GET_USAGE_STATS,
          Process.myUid(),
          context.packageName,
        )
      }
    return mode == AppOpsManager.MODE_ALLOWED
  }

  fun openNotificationListenerSettings(callerContext: Context) {
    val intent =
      Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    runCatching { callerContext.startActivity(intent) }
  }

  fun openDndPolicySettings(callerContext: Context) {
    val intent =
      Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    runCatching { callerContext.startActivity(intent) }
  }

  fun openUsageAccessSettings(callerContext: Context) {
    val intent =
      Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    runCatching { callerContext.startActivity(intent) }
  }

  fun openOverlaySettings(callerContext: Context) {
    val intent =
      Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${callerContext.packageName}"),
      ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    runCatching {
      callerContext.startActivity(intent)
    }.onFailure {
      val fallback =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      runCatching { callerContext.startActivity(fallback) }
    }
  }

  fun openAppNotificationSettings(callerContext: Context) {
    val intent =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
          putExtra(Settings.EXTRA_APP_PACKAGE, callerContext.packageName)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      }
    runCatching { callerContext.startActivity(intent) }
  }
}

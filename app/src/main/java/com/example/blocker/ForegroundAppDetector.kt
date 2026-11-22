package com.example.blocker

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

class ForegroundAppDetector(context: Context) {

  private val appContext = context.applicationContext
  private val usageStatsManager =
    appContext.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

  @Volatile
  private var lastObservedPackage: String? = null

  fun detectForegroundPackage(lookbackMs: Long = 60_000L): String? {
    val manager = usageStatsManager ?: return lastObservedPackage
    val endTime = System.currentTimeMillis()
    val startTime = (endTime - lookbackMs).coerceAtLeast(0L)

    val events = runCatching { manager.queryEvents(startTime, endTime) }.getOrNull()
      ?: return lastObservedPackage

    val event = UsageEvents.Event()
    var latestPkg: String? = null
    var latestTimestamp = -1L

    while (events.hasNextEvent()) {
      events.getNextEvent(event)
      val isForegroundEvent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
        } else {
          @Suppress("DEPRECATION")
          event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
        }

      if (isForegroundEvent && !event.packageName.isNullOrBlank() && event.timeStamp >= latestTimestamp) {
        latestTimestamp = event.timeStamp
        latestPkg = event.packageName
      }
    }

    if (latestPkg != null) {
      lastObservedPackage = latestPkg
    }
    return latestPkg ?: lastObservedPackage
  }

  fun resolveDefaultLauncherPackage(): String? {
    val homeIntent =
      Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
      }
    val resolveInfo =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        appContext.packageManager.resolveActivity(
          homeIntent,
          PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
        )
      } else {
        @Suppress("DEPRECATION")
        appContext.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
      }
    return resolveInfo?.activityInfo?.packageName
  }

  fun resolveAppLabel(packageName: String): String {
    val pm = appContext.packageManager
    return runCatching {
      val appInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
        } else {
          @Suppress("DEPRECATION")
          pm.getApplicationInfo(packageName, 0)
        }
      pm.getApplicationLabel(appInfo).toString().trim().ifEmpty { packageName }
    }.getOrDefault(packageName)
  }

  fun clearCache() {
    lastObservedPackage = null
  }
}

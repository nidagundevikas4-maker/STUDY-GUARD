package com.example.data.rules

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.example.model.AcademicAppClassifier
import com.example.model.AppCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppInfo(
  val packageName: String,
  val appLabel: String,
  val category: AppCategory,
  val icon: ImageBitmap?,
)

class InstalledAppsRepository(context: Context) {

  private val appContext = context.applicationContext

  suspend fun loadLaunchableApps(): List<InstalledAppInfo> =
    withContext(Dispatchers.IO) {
      val pm = appContext.packageManager
      val mainIntent =
        Intent(Intent.ACTION_MAIN, null).apply {
          addCategory(Intent.CATEGORY_LAUNCHER)
        }

      val resolveInfos =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
          @Suppress("DEPRECATION")
          pm.queryIntentActivities(mainIntent, 0)
        }

      val uniqueByPackage = LinkedHashMap<String, InstalledAppInfo>()
      for (info in resolveInfos) {
        val pkg = info.activityInfo?.packageName ?: continue
        if (pkg == appContext.packageName || uniqueByPackage.containsKey(pkg)) continue

        val label = info.loadLabel(pm)?.toString()?.trim().takeUnless { it.isNullOrEmpty() } ?: pkg
        val category = AcademicAppClassifier.classify(pkg, label)
        val iconBitmap =
          runCatching {
              info.loadIcon(pm)?.toBitmap(width = 96, height = 96)?.asImageBitmap()
            }
            .getOrNull()

        uniqueByPackage[pkg] =
          InstalledAppInfo(
            packageName = pkg,
            appLabel = label,
            category = category,
            icon = iconBitmap,
          )
      }

      uniqueByPackage.values.sortedWith(
        compareBy<InstalledAppInfo> { it.category != AppCategory.ACADEMIC }
          .thenBy { it.appLabel.lowercase() }
      )
    }
}

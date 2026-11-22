package com.example

import android.app.Application
import android.os.SystemClock
import com.example.di.AppContainer
import com.example.services.FocusTimerService
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FocusFlowApplication : Application() {
  val appContainer: AppContainer by lazy { AppContainer(this) }
  private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  override fun onCreate() {
    super.onCreate()
    appScope.launch {
      val persisted = appContainer.preferencesRepository.persistedActiveSessionFlow.first()
      if (persisted != null && persisted.isStrictLocked) {
        val nowEpoch = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()
        val currentBootMarker = nowEpoch - nowElapsed
        val sameBoot = abs(currentBootMarker - persisted.bootTimeEstimateMs) < 10_000L
        val remainingMs =
          if (sameBoot && persisted.endElapsedRealtimeMs > 0L) {
            persisted.endElapsedRealtimeMs - nowElapsed
          } else {
            persisted.endEpochMs - nowEpoch
          }
        if (remainingMs > 1_000L && !appContainer.focusTimerRepository.timerState.value.isActive) {
          FocusTimerService.restoreLockedSessionIfNeeded(this@FocusFlowApplication)
        } else if (remainingMs <= 1_000L) {
          appContainer.preferencesRepository.clearActiveSession()
        }
      }
    }
  }
}

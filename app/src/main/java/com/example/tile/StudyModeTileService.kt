package com.example.tile

import android.app.PendingIntent
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.FocusFlowApplication
import com.example.MainActivity
import com.example.R
import com.example.model.FocusMode
import com.example.services.FocusTimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StudyModeTileService : TileService() {

  private val tileScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  override fun onStartListening() {
    super.onStartListening()
    refreshTileState()
  }

  override fun onClick() {
    super.onClick()
    if (isLocked) {
      unlockAndRun { handleTileTap() }
    } else {
      handleTileTap()
    }
  }

  override fun onDestroy() {
    tileScope.cancel()
    super.onDestroy()
  }

  private fun refreshTileState() {
    val tile = qsTile ?: return
    val appContainer = (application as FocusFlowApplication).appContainer
    val timerState = appContainer.focusTimerRepository.timerState.value

    if (timerState.isActive) {
      tile.state = Tile.STATE_ACTIVE
      tile.label =
        if (timerState.isStrictLocked) "Study Locked"
        else timerState.mode.title
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        tile.subtitle = "${FocusTimerService.formatCountdown(timerState.remainingSeconds)} left"
      }
      tile.updateTile()
    } else {
      tile.state = Tile.STATE_INACTIVE
      tile.label = getString(R.string.qs_tile_study_mode)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        tile.subtitle = "${FocusMode.STUDY_MODE.defaultDurationMinutes}m · Tap to start"
      }
      tile.updateTile()

      tileScope.launch {
        val prefs = appContainer.preferencesRepository.preferencesFlow.first()
        val refreshed = qsTile ?: return@launch
        if (!appContainer.focusTimerRepository.timerState.value.isActive) {
          val duration =
            if (prefs.activeFocusMode == FocusMode.STUDY_MODE) prefs.selectedDurationMinutes
            else FocusMode.STUDY_MODE.defaultDurationMinutes
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            refreshed.subtitle = "${duration}m · Tap to start"
            refreshed.updateTile()
          }
        }
      }
    }
  }

  private fun handleTileTap() {
    val appContainer = (application as FocusFlowApplication).appContainer
    val timerState = appContainer.focusTimerRepository.timerState.value

    if (timerState.isActive) {
      // Never bypass an active or strict-locked session; open the Focus Timer UI.
      openActivityCollapsed(MainActivity.createOpenTimerIntent(this))
      return
    }

    tileScope.launch {
      val prefs = appContainer.preferencesRepository.preferencesFlow.first()
      val duration =
        if (prefs.activeFocusMode == FocusMode.STUDY_MODE) prefs.selectedDurationMinutes
        else FocusMode.STUDY_MODE.defaultDurationMinutes
      val strictLock = prefs.studyModeConfig.strictSessionLock

      appContainer.preferencesRepository.setActiveFocusMode(FocusMode.STUDY_MODE)
      appContainer.preferencesRepository.setSelectedDurationMinutes(duration)

      val fgsResult =
        runCatching {
          FocusTimerService.startSession(
            context = applicationContext,
            mode = FocusMode.STUDY_MODE,
            durationMinutes = duration,
            strictLock = strictLock,
          )
        }

      if (fgsResult.isSuccess) {
        qsTile?.let { tile ->
          tile.state = Tile.STATE_ACTIVE
          tile.label = if (strictLock) "Study Locked" else FocusMode.STUDY_MODE.title
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = "${FocusTimerService.formatCountdown(duration * 60)} left"
          }
          tile.updateTile()
        }
      } else {
        openActivityCollapsed(MainActivity.createQuickStudyIntent(this@StudyModeTileService))
      }
    }
  }

  private fun openActivityCollapsed(intent: android.content.Intent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      val pendingIntent =
        PendingIntent.getActivity(
          this,
          QS_TILE_PENDING_INTENT_CODE,
          intent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
      startActivityAndCollapse(pendingIntent)
    } else {
      @Suppress("DEPRECATION")
      startActivityAndCollapse(intent)
    }
  }

  companion object {
    private const val QS_TILE_PENDING_INTENT_CODE = 4401
  }
}

object StudyTileHelper {
  fun requestTileUpdate(context: Context) {
    val appContext = context.applicationContext
    runCatching {
      TileService.requestListeningState(
        appContext,
        ComponentName(appContext, StudyModeTileService::class.java),
      )
    }
  }

  fun canPromptAddTile(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

  fun requestAddTile(
    context: Context,
    onStatusMessage: (String) -> Unit,
  ) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      onStatusMessage("Swipe down twice from the status bar, tap Edit, and drag Study Mode into Quick Settings.")
      return
    }
    val statusBarManager = context.getSystemService(StatusBarManager::class.java)
    if (statusBarManager == null) {
      onStatusMessage("Quick Settings placement is unavailable on this device.")
      return
    }
    statusBarManager.requestAddTileService(
      ComponentName(context, StudyModeTileService::class.java),
      context.getString(R.string.qs_tile_study_mode),
      Icon.createWithResource(context, R.drawable.ic_qs_study_guard),
      context.mainExecutor,
    ) { resultCode ->
      val message =
        when (resultCode) {
          StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
            "Study Mode Quick Settings tile added."
          StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
            "Study Mode tile is already in your Quick Settings."
          StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED ->
            "Tile was not added. You can add it anytime from Quick Settings edit mode."
          else -> "Quick Settings tile request finished (code $resultCode)."
        }
      onStatusMessage(message)
    }
  }
}

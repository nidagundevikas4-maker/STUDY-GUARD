package com.example.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.BlockedAppShieldActivity
import com.example.FocusFlowApplication
import com.example.MainActivity
import com.example.R
import com.example.blocker.BlockEvaluationResult
import com.example.model.FocusMode
import com.example.model.PersistedActiveSession
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FocusTimerService : Service() {

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val persistScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var tickJob: Job? = null
  private var appBlockerJob: Job? = null

  private var targetEndElapsedMs: Long = 0L
  private var pausedRemainingSeconds: Int = 0
  private var totalSessionSeconds: Int = 25 * 60
  private var activeMode: FocusMode = FocusMode.STUDY_MODE
  private var activeDurationMinutes: Int = 25
  private var isStrictLocked: Boolean = false

  private val appContainer by lazy { (application as FocusFlowApplication).appContainer }
  private val timerRepository by lazy { appContainer.focusTimerRepository }

  override fun onCreate() {
    super.onCreate()
    ensureNotificationChannels()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START -> {
        val modeName = intent.getStringExtra(EXTRA_MODE_NAME) ?: FocusMode.STUDY_MODE.name
        activeMode = FocusMode.entries.firstOrNull { it.name == modeName } ?: FocusMode.STUDY_MODE
        activeDurationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 25).coerceIn(1, 180)
        isStrictLocked =
          intent.getBooleanExtra(
            EXTRA_STRICT_LOCK,
            activeMode != FocusMode.LIGHT_FOCUS,
          )
        totalSessionSeconds = activeDurationMinutes * 60
        pausedRemainingSeconds = totalSessionSeconds

        val nowElapsed = SystemClock.elapsedRealtime()
        val nowEpoch = System.currentTimeMillis()
        targetEndElapsedMs = nowElapsed + totalSessionSeconds * 1000L

        timerRepository.onTimerStarted(
          mode = activeMode,
          durationMinutes = activeDurationMinutes,
          isStrictLocked = isStrictLocked,
        )
        persistActiveSessionAsync(
          PersistedActiveSession(
            mode = activeMode,
            plannedMinutes = activeDurationMinutes,
            isStrictLocked = isStrictLocked,
            startedAtEpochMs = nowEpoch,
            endEpochMs = nowEpoch + totalSessionSeconds * 1000L,
            endElapsedRealtimeMs = targetEndElapsedMs,
            bootTimeEstimateMs = nowEpoch - nowElapsed,
          )
        )
        applyModeDndAsync(activeMode)
        startForegroundWithNotification(
          remainingSeconds = totalSessionSeconds,
          isPaused = false,
        )
        startTickLoop()
        startAppBlockerLoop()
      }

      ACTION_PAUSE -> {
        if (isStrictLocked || timerRepository.timerState.value.isStrictLocked) {
          return START_STICKY
        }
        tickJob?.cancel()
        stopAppBlockerLoop()
        val remainingMs = (targetEndElapsedMs - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        pausedRemainingSeconds = ((remainingMs + 999L) / 1000L).toInt()
        timerRepository.onTimerPaused(pausedRemainingSeconds)
        appContainer.dndPolicyManager.restorePreviousDndIfManaged()
        updateOngoingNotification(
          remainingSeconds = pausedRemainingSeconds,
          isPaused = true,
        )
      }

      ACTION_RESUME -> {
        val secondsToRun =
          if (pausedRemainingSeconds > 0) pausedRemainingSeconds
          else timerRepository.timerState.value.remainingSeconds
        targetEndElapsedMs = SystemClock.elapsedRealtime() + secondsToRun * 1000L
        timerRepository.onTimerResumed()
        applyModeDndAsync(activeMode)
        startForegroundWithNotification(
          remainingSeconds = secondsToRun,
          isPaused = false,
        )
        startTickLoop()
        startAppBlockerLoop()
      }

      ACTION_RESET -> {
        val forceUnlock = intent.getBooleanExtra(EXTRA_FORCE_UNLOCK, false)
        if ((isStrictLocked || timerRepository.timerState.value.isStrictLocked) && !forceUnlock) {
          return START_STICKY
        }
        tickJob?.cancel()
        stopAppBlockerLoop()
        val elapsedSeconds =
          (totalSessionSeconds - timerRepository.timerState.value.remainingSeconds)
            .coerceAtLeast(0)
        isStrictLocked = false
        clearActiveSessionAsync()
        appContainer.dndPolicyManager.restorePreviousDndIfManaged()
        timerRepository.onTimerReset(
          mode = activeMode,
          durationMinutes = activeDurationMinutes,
          elapsedSeconds = elapsedSeconds,
          forceUnlock = forceUnlock,
        )
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
      }

      ACTION_RESTORE, null -> {
        restoreSessionIfPersisted()
      }
    }

    return if (isStrictLocked) START_STICKY else START_NOT_STICKY
  }

  private fun restoreSessionIfPersisted() {
    // Enter foreground immediately with placeholder so Android never throws ForegroundServiceDidNotStartInTimeException
    startForegroundWithNotification(
      remainingSeconds = timerRepository.timerState.value.remainingSeconds.coerceAtLeast(1),
      isPaused = false,
    )
    serviceScope.launch {
      val persisted = appContainer.preferencesRepository.persistedActiveSessionFlow.first()
      if (persisted == null || !persisted.isStrictLocked) {
        ServiceCompat.stopForeground(this@FocusTimerService, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
        return@launch
      }

      val nowEpoch = System.currentTimeMillis()
      val nowElapsed = SystemClock.elapsedRealtime()
      val currentBootMarker = nowEpoch - nowElapsed
      val sameBoot = abs(currentBootMarker - persisted.bootTimeEstimateMs) < 10_000L
      val remainingMs =
        if (sameBoot && persisted.endElapsedRealtimeMs > 0L) {
          (persisted.endElapsedRealtimeMs - nowElapsed).coerceAtLeast(0L)
        } else {
          (persisted.endEpochMs - nowEpoch).coerceAtLeast(0L)
        }

      if (remainingMs <= 1_000L) {
        clearActiveSessionAsync()
        ServiceCompat.stopForeground(this@FocusTimerService, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
        return@launch
      }

      activeMode = persisted.mode
      activeDurationMinutes = persisted.plannedMinutes.coerceIn(1, 180)
      totalSessionSeconds = activeDurationMinutes * 60
      isStrictLocked = true
      targetEndElapsedMs = nowElapsed + remainingMs
      val remainingSeconds = ((remainingMs + 999L) / 1000L).toInt()
      pausedRemainingSeconds = remainingSeconds

      timerRepository.restoreRunningSession(
        mode = activeMode,
        durationMinutes = activeDurationMinutes,
        remainingSeconds = remainingSeconds,
        isStrictLocked = true,
        startedAtEpochMs = persisted.startedAtEpochMs,
      )
      applyModeDndAsync(activeMode)
      updateOngoingNotification(remainingSeconds = remainingSeconds, isPaused = false)
      startTickLoop()
      startAppBlockerLoop()
    }
  }

  private fun persistActiveSessionAsync(session: PersistedActiveSession) {
    persistScope.launch {
      appContainer.preferencesRepository.saveActiveSession(session)
    }
  }

  private fun clearActiveSessionAsync() {
    persistScope.launch {
      appContainer.preferencesRepository.clearActiveSession()
    }
  }

  private fun applyModeDndAsync(mode: FocusMode) {
    serviceScope.launch {
      val prefs = appContainer.preferencesRepository.preferencesFlow.first()
      appContainer.dndPolicyManager.applyDndForSession(mode, prefs)
    }
  }

  private fun startTickLoop() {
    tickJob?.cancel()
    var lastDisplayedSecond = -1
    tickJob =
      serviceScope.launch {
        while (isActive) {
          val remainingMs = (targetEndElapsedMs - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
          val remainingSeconds =
            if (remainingMs == 0L) 0 else ((remainingMs + 999L) / 1000L).toInt()

          if (remainingSeconds != lastDisplayedSecond) {
            lastDisplayedSecond = remainingSeconds
            timerRepository.onTimerTick(remainingSeconds)
            updateOngoingNotification(
              remainingSeconds = remainingSeconds,
              isPaused = false,
            )
          }

          if (remainingMs <= 0L) {
            handleSessionCompleted()
            break
          }

          delay(250L)
        }
      }
  }

  private fun startAppBlockerLoop() {
    appBlockerJob?.cancel()
    appContainer.appBlockerManager.resetCooldown()
    appContainer.foregroundAppDetector.clearCache()
    val defaultLauncherPackage = appContainer.foregroundAppDetector.resolveDefaultLauncherPackage()

    appBlockerJob =
      serviceScope.launch {
        while (isActive) {
          val permissionSnapshot = appContainer.permissionReader.readSnapshot()
          if (!permissionSnapshot.usageAccessGranted) {
            delay(1_500L)
            continue
          }

          val prefs = appContainer.preferencesRepository.preferencesFlow.first()
          val currentTimerState = timerRepository.timerState.value
          val result =
            withContext(Dispatchers.IO) {
              val foregroundPkg = appContainer.foregroundAppDetector.detectForegroundPackage()
              appContainer.appBlockerManager.evaluateForegroundApp(
                foregroundPackage = foregroundPkg,
                appLabelProvider = appContainer.foregroundAppDetector::resolveAppLabel,
                defaultLauncherPackage = defaultLauncherPackage,
                timerState = currentTimerState,
                preferences = prefs,
              )
            }

          if (result is BlockEvaluationResult.BlockApp) {
            timerRepository.incrementDismissedNotificationCount()
            if (permissionSnapshot.overlayPermissionGranted) {
              runCatching {
                startActivity(
                  BlockedAppShieldActivity.createIntent(
                    context = this@FocusTimerService,
                    blockedAppLabel = result.appLabel,
                    focusMode = result.focusMode,
                  )
                )
              }.onFailure {
                postBlockedAppAlertNotification(result.appLabel, result.focusMode)
              }
            } else {
              postBlockedAppAlertNotification(result.appLabel, result.focusMode)
            }
          }

          delay(500L)
        }
      }
  }

  private fun stopAppBlockerLoop() {
    appBlockerJob?.cancel()
    appBlockerJob = null
    appContainer.appBlockerManager.resetCooldown()
  }

  private fun handleSessionCompleted() {
    isStrictLocked = false
    clearActiveSessionAsync()
    stopAppBlockerLoop()
    appContainer.dndPolicyManager.restorePreviousDndIfManaged()
    timerRepository.onTimerCompleted()
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    postCompletionNotification()
    stopSelf()
  }

  private fun startForegroundWithNotification(
    remainingSeconds: Int,
    isPaused: Boolean,
  ) {
    val notification = buildOngoingNotification(remainingSeconds, isPaused)
    val fgsType =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
      } else {
        0
      }
    ServiceCompat.startForeground(this, ONGOING_NOTIFICATION_ID, notification, fgsType)
  }

  private fun updateOngoingNotification(
    remainingSeconds: Int,
    isPaused: Boolean,
  ) {
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
          PackageManager.PERMISSION_GRANTED
    ) {
      return
    }
    val notification = buildOngoingNotification(remainingSeconds, isPaused)
    NotificationManagerCompat.from(this).notify(ONGOING_NOTIFICATION_ID, notification)
  }

  private fun buildOngoingNotification(
    remainingSeconds: Int,
    isPaused: Boolean,
  ): Notification {
    val formattedTime = formatCountdown(remainingSeconds)
    val locked = isStrictLocked || timerRepository.timerState.value.isStrictLocked
    val title =
      when {
        locked -> "${activeMode.title} · Strict Lock"
        isPaused -> "${activeMode.title} · Paused"
        else -> "${activeMode.title} · Focusing"
      }
    val statusText =
      when {
        locked -> "$formattedTime remaining · Disabled apps stay locked until complete"
        isPaused -> "$formattedTime remaining · Tap Resume to continue"
        else -> "$formattedTime remaining"
      }

    val openAppIntent =
      PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val builder =
      NotificationCompat.Builder(this, CHANNEL_ONGOING_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(statusText)
        .setContentIntent(openAppIntent)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

    if (!locked) {
      val toggleAction =
        if (isPaused) {
          NotificationCompat.Action.Builder(
            0,
            "Resume",
            servicePendingIntent(ACTION_RESUME, 101),
          ).build()
        } else {
          NotificationCompat.Action.Builder(
            0,
            "Pause",
            servicePendingIntent(ACTION_PAUSE, 102),
          ).build()
        }

      val resetAction =
        NotificationCompat.Action.Builder(
          0,
          "Reset",
          servicePendingIntent(ACTION_RESET, 103),
        ).build()

      builder.addAction(toggleAction).addAction(resetAction)
    }

    return builder.build()
  }

  private fun postCompletionNotification() {
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
          PackageManager.PERMISSION_GRANTED
    ) {
      return
    }

    val openAppIntent =
      PendingIntent.getActivity(
        this,
        2,
        Intent(this, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val notification =
      NotificationCompat.Builder(this, CHANNEL_COMPLETE_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("Focus session complete")
        .setContentText(
          "Completed ${activeDurationMinutes}m in ${activeMode.title}. Locked apps are now accessible."
        )
        .setContentIntent(openAppIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    NotificationManagerCompat.from(this).notify(COMPLETION_NOTIFICATION_ID, notification)
  }

  private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
    val intent = Intent(this, FocusTimerService::class.java).apply { this.action = action }
    return PendingIntent.getService(
      this,
      requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun postBlockedAppAlertNotification(
    appLabel: String,
    focusMode: FocusMode,
  ) {
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
          PackageManager.PERMISSION_GRANTED
    ) {
      return
    }

    val openAppIntent =
      PendingIntent.getActivity(
        this,
        3,
        Intent(this, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val notification =
      NotificationCompat.Builder(this, CHANNEL_BLOCKED_APP_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("$appLabel is blocked in ${focusMode.title}")
        .setContentText("Tap to return to Study Guard. Grant Overlay permission for automatic full-screen blocking.")
        .setContentIntent(openAppIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    NotificationManagerCompat.from(this).notify(BLOCKED_APP_NOTIFICATION_ID, notification)
  }

  private fun ensureNotificationChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

      val ongoingChannel =
        NotificationChannel(
          CHANNEL_ONGOING_ID,
          "Active Focus Timer",
          NotificationManager.IMPORTANCE_LOW,
        ).apply {
          description = "Shows the live countdown while a focus session is active."
          setShowBadge(false)
        }

      val completeChannel =
        NotificationChannel(
          CHANNEL_COMPLETE_ID,
          "Session Completion Alerts",
          NotificationManager.IMPORTANCE_HIGH,
        ).apply {
          description = "Notifies you when your scheduled focus session finishes."
        }

      val blockerChannel =
        NotificationChannel(
          CHANNEL_BLOCKED_APP_ID,
          "Blocked App Reminders",
          NotificationManager.IMPORTANCE_HIGH,
        ).apply {
          description = "Notifies you if a blocked app is opened while Overlay permission is disabled."
        }

      manager.createNotificationChannel(ongoingChannel)
      manager.createNotificationChannel(completeChannel)
      manager.createNotificationChannel(blockerChannel)
    }
  }

  override fun onDestroy() {
    tickJob?.cancel()
    stopAppBlockerLoop()
    appContainer.dndPolicyManager.restorePreviousDndIfManaged()
    serviceScope.cancel()
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  companion object {
    const val ACTION_START = "com.example.action.FOCUS_TIMER_START"
    const val ACTION_PAUSE = "com.example.action.FOCUS_TIMER_PAUSE"
    const val ACTION_RESUME = "com.example.action.FOCUS_TIMER_RESUME"
    const val ACTION_RESET = "com.example.action.FOCUS_TIMER_RESET"
    const val ACTION_RESTORE = "com.example.action.FOCUS_TIMER_RESTORE"

    const val EXTRA_MODE_NAME = "extra_mode_name"
    const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
    const val EXTRA_STRICT_LOCK = "extra_strict_lock"
    const val EXTRA_FORCE_UNLOCK = "extra_force_unlock"

    private const val CHANNEL_ONGOING_ID = "focus_timer_ongoing"
    private const val CHANNEL_COMPLETE_ID = "focus_timer_complete"
    private const val CHANNEL_BLOCKED_APP_ID = "focus_app_blocker"
    private const val ONGOING_NOTIFICATION_ID = 1001
    private const val COMPLETION_NOTIFICATION_ID = 1002
    private const val BLOCKED_APP_NOTIFICATION_ID = 1003

    fun startSession(
      context: Context,
      mode: FocusMode,
      durationMinutes: Int,
      strictLock: Boolean = true,
    ) {
      val intent =
        Intent(context, FocusTimerService::class.java).apply {
          action = ACTION_START
          putExtra(EXTRA_MODE_NAME, mode.name)
          putExtra(EXTRA_DURATION_MINUTES, durationMinutes)
          putExtra(EXTRA_STRICT_LOCK, strictLock)
        }
      ContextCompat.startForegroundService(context, intent)
    }

    fun pauseSession(context: Context) {
      val intent = Intent(context, FocusTimerService::class.java).apply { action = ACTION_PAUSE }
      context.startService(intent)
    }

    fun resumeSession(context: Context) {
      val intent = Intent(context, FocusTimerService::class.java).apply { action = ACTION_RESUME }
      ContextCompat.startForegroundService(context, intent)
    }

    fun resetSession(context: Context) {
      val intent = Intent(context, FocusTimerService::class.java).apply { action = ACTION_RESET }
      context.startService(intent)
    }

    fun emergencyUnlockSession(context: Context) {
      val intent =
        Intent(context, FocusTimerService::class.java).apply {
          action = ACTION_RESET
          putExtra(EXTRA_FORCE_UNLOCK, true)
        }
      context.startService(intent)
    }

    fun restoreLockedSessionIfNeeded(context: Context) {
      val intent = Intent(context, FocusTimerService::class.java).apply { action = ACTION_RESTORE }
      runCatching { ContextCompat.startForegroundService(context, intent) }
    }

    fun formatCountdown(totalSeconds: Int): String {
      val safeSeconds = totalSeconds.coerceAtLeast(0)
      val minutes = safeSeconds / 60
      val seconds = safeSeconds % 60
      return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
  }
}

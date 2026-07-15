package com.example.volumelimiter.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.volumelimiter.MainActivity
import com.example.volumelimiter.R
import com.example.volumelimiter.data.datastore.VolumeDataStore
import com.example.volumelimiter.data.model.VolumeLimiterPreferences
import com.example.volumelimiter.data.repository.InstalledAppRepository
import com.example.volumelimiter.data.repository.VolumeRuleRepository
import com.example.volumelimiter.domain.usecase.RuleEditor
import com.example.volumelimiter.domain.usecase.VolumeDecision
import com.example.volumelimiter.domain.usecase.VolumeLimitSession
import com.example.volumelimiter.domain.usecase.VolumeLevelCalculator
import com.example.volumelimiter.util.ForegroundAppDetector
import com.example.volumelimiter.util.PermissionUtils
import com.example.volumelimiter.util.VolumeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VolumeLimiterService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val session = VolumeLimitSession()

    private lateinit var repository: VolumeRuleRepository
    private lateinit var installedAppRepository: InstalledAppRepository
    private lateinit var detector: ForegroundAppDetector
    private lateinit var volumeController: VolumeController

    private var latestPreferences: VolumeLimiterPreferences? = null
    private var monitorJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        repository = VolumeRuleRepository(VolumeDataStore(applicationContext))
        installedAppRepository = InstalledAppRepository(applicationContext)
        detector = ForegroundAppDetector(applicationContext)
        volumeController = VolumeController(applicationContext)

        createNotificationChannel()
        VolumeMonitorStatusStore.setServiceRunning(true)

        scope.launch {
            repository.preferences.collect { preferences ->
                latestPreferences = preferences
                if (!preferences.monitoringEnabled && monitorJob?.isActive == true) {
                    requestStop(updatePreference = false)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                scope.launch { requestStop(updatePreference = true) }
                return START_NOT_STICKY
            }
            ACTION_START, null -> startSafely()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        restoreBeforeDestroy()
        monitorJob?.cancel()
        scope.cancel()
        VolumeMonitorStatusStore.update {
            it.copy(
                isServiceRunning = false,
                appliedRule = null,
                lastMessage = "Monitoramento parado.",
            )
        }
        super.onDestroy()
    }

    private fun startSafely() {
        if (!PermissionUtils.hasUsageStatsPermission(applicationContext)) {
            scope.launch { repository.setMonitoringEnabled(false) }
            VolumeMonitorStatusStore.update {
                it.copy(
                    isServiceRunning = false,
                    lastMessage = "Libere o acesso ao uso de aplicativos para iniciar.",
                )
            }
            stopSelf()
            return
        }

        runCatching {
            startForeground(NOTIFICATION_ID, buildNotification())
        }.onFailure { error ->
            scope.launch { repository.setMonitoringEnabled(false) }
            VolumeMonitorStatusStore.update {
                it.copy(
                    isServiceRunning = false,
                    lastMessage = "Não foi possível iniciar a notificação: ${error.localizedMessage.orEmpty()}",
                )
            }
            stopSelf()
            return
        }

        VolumeMonitorStatusStore.setServiceRunning(true)
        startMonitoringLoop()
    }

    private fun startMonitoringLoop() {
        if (monitorJob?.isActive == true) return
        monitorJob = scope.launch {
            while (isActive) {
                val preferences = latestPreferences ?: repository.preferences.first()
                if (!preferences.monitoringEnabled) {
                    requestStop(updatePreference = false)
                    break
                }
                if (!PermissionUtils.hasUsageStatsPermission(applicationContext)) {
                    VolumeMonitorStatusStore.update {
                        it.copy(lastMessage = "Acesso ao uso removido. Monitoramento parado.")
                    }
                    requestStop(updatePreference = true)
                    break
                }
                tick(preferences)
                delay(MONITORING_INTERVAL_MS)
            }
        }
    }

    private suspend fun tick(preferences: VolumeLimiterPreferences) {
        val maxVolumeLevel = volumeController.getMaxMediaVolumeLevel()
        val currentVolumeLevel = volumeController.getCurrentMediaVolumeLevel()

        if (maxVolumeLevel == null || currentVolumeLevel == null || maxVolumeLevel <= 0) {
            VolumeMonitorStatusStore.update {
                it.copy(lastMessage = "Não foi possível acessar o volume de mídia do aparelho.")
            }
            return
        }

        val foregroundPackageName = detector.detectForegroundPackageName()
        val monitoredPackage = foregroundPackageName?.takeUnless { it == packageName }
        val rule = RuleEditor.findEnabledRule(preferences.rules, monitoredPackage)
        val decision = session.evaluate(
            foregroundPackageName = foregroundPackageName,
            matchingRule = rule,
            currentVolumeLevel = currentVolumeLevel,
            maxVolumeLevel = maxVolumeLevel,
        )

        val finalVolumeLevel = when (decision) {
            VolumeDecision.None -> currentVolumeLevel
            is VolumeDecision.SetVolume -> {
                volumeController.setMediaVolumeLevel(decision.level)
                decision.level
            }
        }

        val currentAppName = withContext(Dispatchers.IO) {
            installedAppRepository.resolveAppName(foregroundPackageName)
        }

        VolumeMonitorStatusStore.update {
            it.copy(
                isServiceRunning = true,
                currentPackageName = foregroundPackageName,
                currentAppName = currentAppName,
                appliedRule = rule,
                currentVolumeLevel = finalVolumeLevel,
                maxVolumeLevel = maxVolumeLevel,
                currentVolumePercent = VolumeLevelCalculator.levelToPercent(
                    finalVolumeLevel,
                    maxVolumeLevel,
                ),
                lastMessage = when {
                    foregroundPackageName == null -> "Aplicativo atual não identificado."
                    rule != null -> "Aplicando limite de ${rule.maxVolumePercent}% para ${rule.appName}."
                    else -> "Nenhum limite aplicado no momento."
                },
            )
        }
    }

    private suspend fun requestStop(updatePreference: Boolean) {
        if (updatePreference) {
            repository.setMonitoringEnabled(false)
        }
        restoreBeforeDestroy()
        stopForegroundCompat()
        stopSelf()
    }

    private fun restoreBeforeDestroy() {
        val current = volumeController.getCurrentMediaVolumeLevel() ?: return
        val decision = session.stop(current)
        if (decision is VolumeDecision.SetVolume) {
            volumeController.setMediaVolumeLevel(decision.level)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            REQUEST_CONTENT,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val stopIntent = PendingIntent.getService(
            this,
            REQUEST_STOP,
            Intent(this, VolumeLimiterService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_description))
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                R.drawable.ic_launcher_foreground,
                getString(R.string.notification_stop),
                stopIntent,
            )
            .build()
    }

    companion object {
        const val ACTION_START = "com.example.volumelimiter.action.START"
        const val ACTION_STOP = "com.example.volumelimiter.action.STOP"
        const val CHANNEL_ID = "volume_limiter_monitoring"
        const val NOTIFICATION_ID = 2001
        const val MONITORING_INTERVAL_MS = 750L

        private const val REQUEST_CONTENT = 1
        private const val REQUEST_STOP = 2

        fun start(context: Context): Boolean {
            val intent = Intent(context, VolumeLimiterService::class.java).setAction(ACTION_START)
            return runCatching {
                ContextCompat.startForegroundService(context, intent)
                true
            }.getOrDefault(false)
        }

        fun stop(context: Context) {
            val intent = Intent(context, VolumeLimiterService::class.java).setAction(ACTION_STOP)
            runCatching { context.startService(intent) }
                .onFailure { context.stopService(intent) }
        }
    }
}

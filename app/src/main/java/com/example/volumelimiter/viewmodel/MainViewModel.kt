package com.example.volumelimiter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.volumelimiter.data.datastore.VolumeDataStore
import com.example.volumelimiter.data.model.AppVolumeRule
import com.example.volumelimiter.data.model.MonitoringStatus
import com.example.volumelimiter.data.repository.VolumeRuleRepository
import com.example.volumelimiter.service.VolumeLimiterService
import com.example.volumelimiter.service.VolumeMonitorStatusStore
import com.example.volumelimiter.util.PermissionUtils
import com.example.volumelimiter.util.VolumeController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val repository = VolumeRuleRepository(VolumeDataStore(appContext))
    private val volumeController = VolumeController(appContext)
    private val permissionState = MutableStateFlow(readPermissions())
    private val actionMessage = MutableStateFlow<String?>(null)

    val uiState = combine(
        repository.preferences,
        VolumeMonitorStatusStore.status,
        permissionState,
        actionMessage,
    ) { preferences, status, permissions, message ->
        MainUiState(
            rules = preferences.rules,
            monitoringEnabled = preferences.monitoringEnabled,
            status = status,
            usagePermissionGranted = permissions.usageStatsGranted,
            notificationPermissionGranted = permissions.notificationGranted,
            actionMessage = message ?: status.lastMessage,
            currentVolumePercent = status.currentVolumePercent
                ?: volumeController.getCurrentMediaVolumePercent(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    fun refreshPermissions() {
        permissionState.value = readPermissions()
    }

    fun setMonitoringEnabled(enabled: Boolean) {
        if (enabled) {
            startMonitoring()
        } else {
            stopMonitoring()
        }
    }

    fun startMonitoring() {
        refreshPermissions()
        if (!permissionState.value.usageStatsGranted) {
            actionMessage.value = "Libere o acesso ao uso de aplicativos antes de ativar."
            return
        }

        viewModelScope.launch {
            repository.setMonitoringEnabled(true)
            val started = VolumeLimiterService.start(appContext)
            if (started) {
                actionMessage.value = "Monitoramento ativado."
            } else {
                repository.setMonitoringEnabled(false)
                actionMessage.value = "Não foi possível iniciar o serviço de monitoramento."
            }
        }
    }

    fun stopMonitoring() {
        viewModelScope.launch {
            repository.setMonitoringEnabled(false)
            VolumeLimiterService.stop(appContext)
            actionMessage.value = "Monitoramento desativado."
        }
    }

    fun setRuleEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setRuleEnabled(packageName, enabled)
        }
    }

    fun setRuleLimit(packageName: String, percent: Int) {
        viewModelScope.launch {
            repository.setRuleLimit(packageName, percent)
        }
    }

    fun removeRule(packageName: String) {
        viewModelScope.launch {
            repository.removeRule(packageName)
        }
    }

    private fun readPermissions(): PermissionState =
        PermissionState(
            usageStatsGranted = PermissionUtils.hasUsageStatsPermission(appContext),
            notificationGranted = PermissionUtils.hasNotificationPermission(appContext),
        )
}

data class MainUiState(
    val rules: List<AppVolumeRule> = emptyList(),
    val monitoringEnabled: Boolean = false,
    val status: MonitoringStatus = MonitoringStatus(),
    val usagePermissionGranted: Boolean = false,
    val notificationPermissionGranted: Boolean = true,
    val actionMessage: String? = null,
    val currentVolumePercent: Int? = null,
)

private data class PermissionState(
    val usageStatsGranted: Boolean,
    val notificationGranted: Boolean,
)

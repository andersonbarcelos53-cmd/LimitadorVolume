package com.example.volumelimiter.service

import com.example.volumelimiter.data.model.MonitoringStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object VolumeMonitorStatusStore {
    private val mutableStatus = MutableStateFlow(MonitoringStatus())
    val status: StateFlow<MonitoringStatus> = mutableStatus.asStateFlow()

    fun update(transform: (MonitoringStatus) -> MonitoringStatus) {
        mutableStatus.update(transform)
    }

    fun setServiceRunning(isRunning: Boolean) {
        mutableStatus.update { it.copy(isServiceRunning = isRunning) }
    }
}

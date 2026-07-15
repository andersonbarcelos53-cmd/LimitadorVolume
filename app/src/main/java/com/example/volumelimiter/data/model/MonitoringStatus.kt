package com.example.volumelimiter.data.model

data class MonitoringStatus(
    val isServiceRunning: Boolean = false,
    val currentPackageName: String? = null,
    val currentAppName: String? = null,
    val appliedRule: AppVolumeRule? = null,
    val currentVolumePercent: Int? = null,
    val currentVolumeLevel: Int? = null,
    val maxVolumeLevel: Int? = null,
    val lastMessage: String? = null,
)

package com.example.volumelimiter.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AppVolumeRule(
    val packageName: String,
    val appName: String,
    val maxVolumePercent: Int,
    val enabled: Boolean,
)

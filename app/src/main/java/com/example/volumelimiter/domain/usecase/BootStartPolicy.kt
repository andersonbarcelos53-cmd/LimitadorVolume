package com.example.volumelimiter.domain.usecase

import com.example.volumelimiter.data.model.VolumeLimiterPreferences

object BootStartPolicy {
    fun shouldStartAfterBoot(
        preferences: VolumeLimiterPreferences,
        usageAccessGranted: Boolean,
    ): Boolean =
        preferences.monitoringEnabled &&
            preferences.parentalControls.autoStartOnBoot &&
            usageAccessGranted
}

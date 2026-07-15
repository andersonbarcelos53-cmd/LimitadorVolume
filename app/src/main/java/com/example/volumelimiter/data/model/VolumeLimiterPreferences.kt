package com.example.volumelimiter.data.model

data class VolumeLimiterPreferences(
    val rules: List<AppVolumeRule> = emptyList(),
    val monitoringEnabled: Boolean = false,
    val parentalControls: ParentalControlPreferences = ParentalControlPreferences(),
) {
    val protectedAppCount: Int
        get() = rules.count { it.enabled }
}

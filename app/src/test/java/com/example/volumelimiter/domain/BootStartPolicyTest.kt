package com.example.volumelimiter.domain

import com.example.volumelimiter.data.model.ParentalControlPreferences
import com.example.volumelimiter.data.model.VolumeLimiterPreferences
import com.example.volumelimiter.domain.usecase.BootStartPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootStartPolicyTest {
    @Test
    fun shouldStartAfterBoot_whenMonitoringAutoStartAndPermissionAreEnabled() {
        val preferences = VolumeLimiterPreferences(
            monitoringEnabled = true,
            parentalControls = ParentalControlPreferences(autoStartOnBoot = true),
        )

        assertTrue(BootStartPolicy.shouldStartAfterBoot(preferences, usageAccessGranted = true))
    }

    @Test
    fun shouldNotStartAfterBoot_whenMonitoringIsDisabled() {
        val preferences = VolumeLimiterPreferences(
            monitoringEnabled = false,
            parentalControls = ParentalControlPreferences(autoStartOnBoot = true),
        )

        assertFalse(BootStartPolicy.shouldStartAfterBoot(preferences, usageAccessGranted = true))
    }

    @Test
    fun shouldNotStartAfterBoot_whenAutoStartIsDisabled() {
        val preferences = VolumeLimiterPreferences(
            monitoringEnabled = true,
            parentalControls = ParentalControlPreferences(autoStartOnBoot = false),
        )

        assertFalse(BootStartPolicy.shouldStartAfterBoot(preferences, usageAccessGranted = true))
    }

    @Test
    fun shouldNotStartAfterBoot_withoutUsageAccess() {
        val preferences = VolumeLimiterPreferences(
            monitoringEnabled = true,
            parentalControls = ParentalControlPreferences(autoStartOnBoot = true),
        )

        assertFalse(BootStartPolicy.shouldStartAfterBoot(preferences, usageAccessGranted = false))
    }
}

package com.example.volumelimiter.domain

import com.example.volumelimiter.data.model.AppVolumeRule
import com.example.volumelimiter.domain.usecase.VolumeDecision
import com.example.volumelimiter.domain.usecase.VolumeDecisionReason
import com.example.volumelimiter.domain.usecase.VolumeLimitSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeLimitSessionTest {
    @Test
    fun monitoredAppAboveLimit_reducesVolumeAndStoresOriginal() {
        val session = VolumeLimitSession()

        val decision = session.evaluate(
            foregroundPackageName = "com.video",
            matchingRule = rule("com.video", 40),
            currentVolumeLevel = 12,
            maxVolumeLevel = 15,
        )

        assertSetVolume(decision, 6)
        assertEquals(12, session.snapshot.originalVolumeLevel)
        assertTrue(session.snapshot.limiterReducedVolume)
    }

    @Test
    fun monitoredAppBelowLimit_doesNotIncreaseVolume() {
        val session = VolumeLimitSession()

        val decision = session.evaluate(
            foregroundPackageName = "com.video",
            matchingRule = rule("com.video", 40),
            currentVolumeLevel = 4,
            maxVolumeLevel = 15,
        )

        assertEquals(VolumeDecision.None, decision)
        assertEquals(4, session.snapshot.originalVolumeLevel)
    }

    @Test
    fun leavingMonitoredApp_restoresOriginalOnlyWhenLimiterChangedVolume() {
        val session = VolumeLimitSession()
        session.evaluate(
            foregroundPackageName = "com.video",
            matchingRule = rule("com.video", 40),
            currentVolumeLevel = 12,
            maxVolumeLevel = 15,
        )

        val decision = session.evaluate(
            foregroundPackageName = "com.launcher",
            matchingRule = null,
            currentVolumeLevel = 6,
            maxVolumeLevel = 15,
        )

        assertSetVolume(decision, 12)
        assertEquals(null, session.snapshot.originalVolumeLevel)
    }

    @Test
    fun leavingAfterUserLoweredVolume_doesNotRestoreIncorrectValue() {
        val session = VolumeLimitSession()
        session.evaluate(
            foregroundPackageName = "com.video",
            matchingRule = rule("com.video", 40),
            currentVolumeLevel = 12,
            maxVolumeLevel = 15,
        )

        val decision = session.evaluate(
            foregroundPackageName = "com.launcher",
            matchingRule = null,
            currentVolumeLevel = 3,
            maxVolumeLevel = 15,
        )

        assertEquals(VolumeDecision.None, decision)
    }

    @Test
    fun directSwitchBetweenMonitoredApps_keepsOriginalVolumeAndAppliesStricterLimit() {
        val session = VolumeLimitSession()
        session.evaluate(
            foregroundPackageName = "com.video",
            matchingRule = rule("com.video", 40),
            currentVolumeLevel = 12,
            maxVolumeLevel = 15,
        )

        val secondDecision = session.evaluate(
            foregroundPackageName = "com.shortvideo",
            matchingRule = rule("com.shortvideo", 25),
            currentVolumeLevel = 6,
            maxVolumeLevel = 15,
        )

        assertSetVolume(secondDecision, 4)
        assertEquals(12, session.snapshot.originalVolumeLevel)

        val restoreDecision = session.evaluate(
            foregroundPackageName = "com.launcher",
            matchingRule = null,
            currentVolumeLevel = 4,
            maxVolumeLevel = 15,
        )

        assertSetVolume(restoreDecision, 12)
    }

    @Test
    fun stop_restoresWhenServiceEndsDuringLimitedSession() {
        val session = VolumeLimitSession()
        session.evaluate(
            foregroundPackageName = "com.video",
            matchingRule = rule("com.video", 40),
            currentVolumeLevel = 12,
            maxVolumeLevel = 15,
        )

        val decision = session.stop(currentVolumeLevel = 6)

        assertSetVolume(decision, 12)
    }

    private fun assertSetVolume(decision: VolumeDecision, expectedLevel: Int) {
        assertTrue(decision is VolumeDecision.SetVolume)
        val setVolume = decision as VolumeDecision.SetVolume
        assertEquals(expectedLevel, setVolume.level)
        assertTrue(
            setVolume.reason is VolumeDecisionReason.ApplyLimit ||
                setVolume.reason is VolumeDecisionReason.RestoreOriginal,
        )
    }

    private fun rule(packageName: String, percent: Int): AppVolumeRule =
        AppVolumeRule(
            packageName = packageName,
            appName = packageName,
            maxVolumePercent = percent,
            enabled = true,
        )
}

package com.example.volumelimiter.domain.usecase

import com.example.volumelimiter.data.model.AppVolumeRule

class VolumeLimitSession {
    private var originalVolumeLevel: Int? = null
    private var lastLimiterLevel: Int? = null
    private var limiterReducedVolume: Boolean = false
    private var activePackageName: String? = null

    val snapshot: VolumeSessionSnapshot
        get() = VolumeSessionSnapshot(
            originalVolumeLevel = originalVolumeLevel,
            lastLimiterLevel = lastLimiterLevel,
            limiterReducedVolume = limiterReducedVolume,
            activePackageName = activePackageName,
        )

    fun evaluate(
        foregroundPackageName: String?,
        matchingRule: AppVolumeRule?,
        currentVolumeLevel: Int,
        maxVolumeLevel: Int,
    ): VolumeDecision {
        if (foregroundPackageName == null) {
            return VolumeDecision.None
        }

        if (matchingRule == null || !matchingRule.enabled) {
            return leaveMonitoredSession(currentVolumeLevel)
        }

        if (originalVolumeLevel == null) {
            originalVolumeLevel = currentVolumeLevel
        }
        activePackageName = matchingRule.packageName

        val limitLevel = VolumeLevelCalculator.percentToLevel(
            percent = matchingRule.maxVolumePercent,
            maxVolumeLevel = maxVolumeLevel,
        )

        return if (currentVolumeLevel > limitLevel) {
            limiterReducedVolume = true
            lastLimiterLevel = limitLevel
            VolumeDecision.SetVolume(
                level = limitLevel,
                reason = VolumeDecisionReason.ApplyLimit(matchingRule),
            )
        } else {
            VolumeDecision.None
        }
    }

    fun stop(currentVolumeLevel: Int): VolumeDecision = leaveMonitoredSession(currentVolumeLevel)

    private fun leaveMonitoredSession(currentVolumeLevel: Int): VolumeDecision {
        val original = originalVolumeLevel
        val lastSet = lastLimiterLevel
        val shouldRestore = limiterReducedVolume &&
            original != null &&
            lastSet != null &&
            currentVolumeLevel == lastSet &&
            original != currentVolumeLevel

        reset()

        return if (shouldRestore) {
            VolumeDecision.SetVolume(
                level = original,
                reason = VolumeDecisionReason.RestoreOriginal,
            )
        } else {
            VolumeDecision.None
        }
    }

    private fun reset() {
        originalVolumeLevel = null
        lastLimiterLevel = null
        limiterReducedVolume = false
        activePackageName = null
    }
}

data class VolumeSessionSnapshot(
    val originalVolumeLevel: Int?,
    val lastLimiterLevel: Int?,
    val limiterReducedVolume: Boolean,
    val activePackageName: String?,
)

sealed interface VolumeDecision {
    data object None : VolumeDecision

    data class SetVolume(
        val level: Int,
        val reason: VolumeDecisionReason,
    ) : VolumeDecision
}

sealed interface VolumeDecisionReason {
    data class ApplyLimit(val rule: AppVolumeRule) : VolumeDecisionReason
    data object RestoreOriginal : VolumeDecisionReason
}

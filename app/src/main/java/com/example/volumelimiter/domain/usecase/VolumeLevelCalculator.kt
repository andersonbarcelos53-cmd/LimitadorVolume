package com.example.volumelimiter.domain.usecase

import kotlin.math.roundToInt

object VolumeLevelCalculator {
    fun percentToLevel(percent: Int, maxVolumeLevel: Int): Int {
        if (maxVolumeLevel <= 0) return 0
        val boundedPercent = percent.coerceIn(0, 100)
        return (maxVolumeLevel * (boundedPercent / 100f))
            .roundToInt()
            .coerceIn(0, maxVolumeLevel)
    }

    fun levelToPercent(level: Int, maxVolumeLevel: Int): Int {
        if (maxVolumeLevel <= 0) return 0
        return ((level.coerceIn(0, maxVolumeLevel) / maxVolumeLevel.toFloat()) * 100f)
            .roundToInt()
            .coerceIn(0, 100)
    }
}

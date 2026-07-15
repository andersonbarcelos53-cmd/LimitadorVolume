package com.example.volumelimiter.domain

import com.example.volumelimiter.domain.usecase.VolumeLevelCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class VolumeLevelCalculatorTest {
    @Test
    fun percentToLevel_convertsFortyPercentOfFifteenToSix() {
        assertEquals(6, VolumeLevelCalculator.percentToLevel(percent = 40, maxVolumeLevel = 15))
    }

    @Test
    fun percentToLevel_clampsPercentOutsideRange() {
        assertEquals(0, VolumeLevelCalculator.percentToLevel(percent = -20, maxVolumeLevel = 15))
        assertEquals(15, VolumeLevelCalculator.percentToLevel(percent = 140, maxVolumeLevel = 15))
    }

    @Test
    fun levelToPercent_returnsZeroWhenMaxVolumeIsUnexpected() {
        assertEquals(0, VolumeLevelCalculator.levelToPercent(level = 7, maxVolumeLevel = 0))
    }
}

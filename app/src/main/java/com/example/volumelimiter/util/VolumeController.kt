package com.example.volumelimiter.util

import android.content.Context
import android.media.AudioManager
import com.example.volumelimiter.domain.usecase.VolumeLevelCalculator

class VolumeController(
    context: Context,
) {
    private val audioManager: AudioManager? = context.getSystemService(AudioManager::class.java)

    fun getMaxMediaVolumeLevel(): Int? =
        runCatching { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
            .getOrNull()
            ?.takeIf { it > 0 }

    fun getCurrentMediaVolumeLevel(): Int? =
        runCatching { audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) }
            .getOrNull()

    fun getCurrentMediaVolumePercent(): Int? {
        val max = getMaxMediaVolumeLevel() ?: return null
        val current = getCurrentMediaVolumeLevel() ?: return null
        return VolumeLevelCalculator.levelToPercent(current, max)
    }

    fun setMediaVolumeLevel(level: Int): Boolean {
        val max = getMaxMediaVolumeLevel() ?: return false
        val boundedLevel = level.coerceIn(0, max)
        return runCatching {
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, boundedLevel, 0)
            true
        }.getOrDefault(false)
    }
}

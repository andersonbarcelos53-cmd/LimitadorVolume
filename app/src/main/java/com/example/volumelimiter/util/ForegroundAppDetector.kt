package com.example.volumelimiter.util

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build

class ForegroundAppDetector(
    private val context: Context,
    private val lookBackMillis: Long = DEFAULT_LOOK_BACK_MILLIS,
) {
    private val usageStatsManager: UsageStatsManager? =
        context.getSystemService(UsageStatsManager::class.java)

    fun detectForegroundPackageName(): String? {
        if (!PermissionUtils.hasUsageStatsPermission(context)) return null
        val manager = usageStatsManager ?: return null
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - lookBackMillis
        val events = runCatching { manager.queryEvents(beginTime, endTime) }.getOrNull()
            ?: return null

        val event = UsageEvents.Event()
        var latestPackageName: String? = null
        var latestTimestamp = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.timeStamp < latestTimestamp) continue

            val foregroundEvent = event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    event.eventType == UsageEvents.Event.ACTIVITY_RESUMED)

            if (foregroundEvent && !event.packageName.isNullOrBlank()) {
                latestPackageName = event.packageName
                latestTimestamp = event.timeStamp
            }
        }

        return latestPackageName
    }

    companion object {
        const val DEFAULT_LOOK_BACK_MILLIS = 10_000L
    }
}

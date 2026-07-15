package com.example.volumelimiter.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.volumelimiter.data.datastore.VolumeDataStore
import com.example.volumelimiter.data.repository.VolumeRuleRepository
import com.example.volumelimiter.service.VolumeLimiterService
import com.example.volumelimiter.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val repository = VolumeRuleRepository(VolumeDataStore(context.applicationContext))
                val preferences = repository.preferences.first()
                if (preferences.monitoringEnabled &&
                    PermissionUtils.hasUsageStatsPermission(context.applicationContext)
                ) {
                    VolumeLimiterService.start(context.applicationContext)
                }
            }
            pendingResult.finish()
        }
    }
}

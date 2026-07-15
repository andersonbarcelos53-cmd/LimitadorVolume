package com.example.volumelimiter.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.volumelimiter.data.datastore.VolumeDataStore
import com.example.volumelimiter.data.repository.VolumeRuleRepository
import com.example.volumelimiter.domain.usecase.BootStartPolicy
import com.example.volumelimiter.service.VolumeLimiterService
import com.example.volumelimiter.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val repository = VolumeRuleRepository(VolumeDataStore(context.applicationContext))
                val preferences = repository.preferences.first()
                val usageAccessGranted =
                    PermissionUtils.hasUsageStatsPermission(context.applicationContext)
                if (BootStartPolicy.shouldStartAfterBoot(preferences, usageAccessGranted)) {
                    val started = VolumeLimiterService.start(context.applicationContext)
                    Log.i(TAG, "Serviço solicitado após $action. started=$started")
                } else {
                    Log.i(
                        TAG,
                        "Serviço não iniciado após $action. monitoring=${preferences.monitoringEnabled}, " +
                            "autoStart=${preferences.parentalControls.autoStartOnBoot}, " +
                            "usageAccess=$usageAccessGranted",
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Falha ao processar $action", error)
            }
            pendingResult.finish()
        }
    }

    private companion object {
        const val TAG = "BootReceiver"
    }
}

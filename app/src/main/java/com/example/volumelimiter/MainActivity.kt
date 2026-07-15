package com.example.volumelimiter

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.volumelimiter.ui.navigation.VolumeLimiterApp
import com.example.volumelimiter.ui.theme.VolumeLimiterTheme
import com.example.volumelimiter.util.PermissionUtils
import com.example.volumelimiter.viewmodel.MainViewModel
import com.example.volumelimiter.viewmodel.SecurityViewModel

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private val securityViewModel: SecurityViewModel by viewModels()
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) {
            mainViewModel.refreshPermissions()
        }

        setContent {
            VolumeLimiterTheme {
                VolumeLimiterApp(
                    mainViewModel = mainViewModel,
                    securityViewModel = securityViewModel,
                    onOpenUsageSettings = {
                        startActivitySafely(PermissionUtils.usageAccessSettingsIntent())
                    },
                    onOpenAppSettings = {
                        startActivitySafely(PermissionUtils.appDetailsSettingsIntent(this))
                    },
                    onOpenNotificationSettings = {
                        startActivitySafely(PermissionUtils.appNotificationSettingsIntent(this))
                    },
                    onOpenBatterySettings = {
                        startActivitySafely(PermissionUtils.batteryOptimizationSettingsIntent())
                    },
                    onRequestNotificationPermission = ::requestNotificationPermission,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.refreshPermissions()
    }

    override fun onStart() {
        super.onStart()
        securityViewModel.onAppForegrounded()
    }

    override fun onStop() {
        securityViewModel.onAppBackgrounded()
        super.onStop()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            mainViewModel.refreshPermissions()
        }
    }

    private fun startActivitySafely(intent: Intent) {
        runCatching {
            startActivity(intent)
        }.recoverCatching {
            if (it is ActivityNotFoundException) {
                startActivity(PermissionUtils.appDetailsSettingsIntent(this))
            } else {
                throw it
            }
        }
    }
}

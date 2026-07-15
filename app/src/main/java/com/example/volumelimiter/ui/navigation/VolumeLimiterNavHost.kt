package com.example.volumelimiter.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.volumelimiter.ui.screens.AppConfigScreen
import com.example.volumelimiter.ui.screens.AppSelectionScreen
import com.example.volumelimiter.ui.screens.HomeScreen
import com.example.volumelimiter.ui.screens.InfoScreen
import com.example.volumelimiter.ui.screens.PinLockScreen
import com.example.volumelimiter.ui.screens.PinSetupScreen
import com.example.volumelimiter.ui.screens.SecurityScreen
import com.example.volumelimiter.ui.screens.SettingsScreen
import com.example.volumelimiter.viewmodel.MainViewModel
import com.example.volumelimiter.viewmodel.SecurityViewModel

@Composable
fun VolumeLimiterApp(
    mainViewModel: MainViewModel,
    securityViewModel: SecurityViewModel,
    onOpenUsageSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val securityState by securityViewModel.uiState.collectAsStateWithLifecycle()

    if (!securityState.isPinConfigured) {
        PinSetupScreen(
            state = securityState,
            onCreatePin = securityViewModel::createPin,
            modifier = modifier,
        )
        return
    }

    if (!securityState.isUnlocked) {
        PinLockScreen(
            state = securityState,
            onUnlock = securityViewModel::authenticate,
            modifier = modifier,
        )
        return
    }

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Home,
        modifier = modifier,
    ) {
        composable(Routes.Home) {
            HomeScreen(
                viewModel = mainViewModel,
                onAddApp = { navController.navigate(Routes.SelectApp) },
                onOpenConfig = { packageName ->
                    navController.navigate(Routes.config(packageName))
                },
                onOpenInfo = { navController.navigate(Routes.Info) },
                onOpenSettings = { navController.navigate(Routes.Settings) },
                onLockNow = securityViewModel::lockNow,
                onOpenUsageSettings = onOpenUsageSettings,
                onOpenBatterySettings = onOpenBatterySettings,
                onRequestNotificationPermission = onRequestNotificationPermission,
            )
        }

        composable(Routes.SelectApp) {
            AppSelectionScreen(
                onBack = { navController.popBackStack() },
                onOpenConfig = { packageName ->
                    navController.navigate(Routes.config(packageName))
                },
            )
        }

        composable(
            route = Routes.Config,
            arguments = listOf(
                navArgument(Routes.ConfigPackageArg) {
                    type = NavType.StringType
                },
            ),
        ) { backStackEntry ->
            val encodedPackageName = backStackEntry.arguments
                ?.getString(Routes.ConfigPackageArg)
                .orEmpty()
            AppConfigScreen(
                packageName = Uri.decode(encodedPackageName),
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.Info) {
            InfoScreen(
                onBack = { navController.popBackStack() },
                onOpenUsageSettings = onOpenUsageSettings,
                onOpenAppSettings = onOpenAppSettings,
                onOpenBatterySettings = onOpenBatterySettings,
            )
        }

        composable(Routes.Settings) {
            SettingsScreen(
                viewModel = mainViewModel,
                onBack = { navController.popBackStack() },
                onOpenUsageSettings = onOpenUsageSettings,
                onOpenNotificationSettings = onOpenNotificationSettings,
                onOpenBatterySettings = onOpenBatterySettings,
                onOpenSecurity = { navController.navigate(Routes.Security) },
                onLockNow = securityViewModel::lockNow,
            )
        }

        composable(Routes.Security) {
            SecurityScreen(
                viewModel = securityViewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private object Routes {
    const val Home = "home"
    const val SelectApp = "select_app"
    const val Info = "info"
    const val Settings = "settings"
    const val Security = "security"
    const val ConfigPackageArg = "packageName"
    const val Config = "config/{$ConfigPackageArg}"

    fun config(packageName: String): String = "config/${Uri.encode(packageName)}"
}

package com.example.volumelimiter.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.volumelimiter.ui.screens.AppConfigScreen
import com.example.volumelimiter.ui.screens.AppSelectionScreen
import com.example.volumelimiter.ui.screens.HomeScreen
import com.example.volumelimiter.ui.screens.InfoScreen
import com.example.volumelimiter.viewmodel.MainViewModel

@Composable
fun VolumeLimiterApp(
    mainViewModel: MainViewModel,
    onOpenUsageSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                onOpenUsageSettings = onOpenUsageSettings,
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
    }
}

private object Routes {
    const val Home = "home"
    const val SelectApp = "select_app"
    const val Info = "info"
    const val ConfigPackageArg = "packageName"
    const val Config = "config/{$ConfigPackageArg}"

    fun config(packageName: String): String = "config/${Uri.encode(packageName)}"
}

package com.example.volumelimiter.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.example.volumelimiter.data.model.InstalledAppInfo

class InstalledAppRepository(
    private val context: Context,
) {
    private val packageManager: PackageManager = context.packageManager

    fun loadLaunchableApps(): List<InstalledAppInfo> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(0L),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

        return resolveInfos
            .asSequence()
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                val packageName = activityInfo.packageName ?: return@mapNotNull null
                if (packageName == context.packageName) return@mapNotNull null

                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                    ?: return@mapNotNull null
                if (launchIntent.categories?.contains(Intent.CATEGORY_LAUNCHER) == false) {
                    return@mapNotNull null
                }

                InstalledAppInfo(
                    packageName = packageName,
                    appName = resolveInfo.loadLabel(packageManager)?.toString()
                        ?: packageName,
                    icon = runCatching { resolveInfo.loadIcon(packageManager) }.getOrNull(),
                )
            }
            .distinctBy { it.packageName }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName })
            .toList()
    }

    fun resolveAppName(packageName: String?): String? {
        if (packageName.isNullOrBlank()) return null
        return runCatching {
            val applicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0L),
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
            packageManager.getApplicationLabel(applicationInfo).toString()
        }.getOrNull()
    }

    fun resolveIcon(packageName: String): android.graphics.drawable.Drawable? =
        runCatching { packageManager.getApplicationIcon(packageName) }.getOrNull()
}

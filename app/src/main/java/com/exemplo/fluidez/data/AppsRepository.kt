package com.exemplo.fluidez.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process

private const val FLAG_INSTALLED = 0x00800000 // ApplicationInfo.FLAG_INSTALLED

class AppsRepository(private val context: Context) {

    /** O usuário já liberou o "Acesso de uso"? */
    fun hasUsageAccess(): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun loadApps(): List<AppInfo> {
        val pm = context.packageManager
        val usage = loadUsageMap()
        // MATCH_UNINSTALLED_PACKAGES inclui apps removidos para o usuário (pra poder restaurar).
        val flags = PackageManager.GET_META_DATA or PackageManager.MATCH_UNINSTALLED_PACKAGES

        return pm.getInstalledApplications(flags)
            .map { app ->
                AppInfo(
                    packageName = app.packageName,
                    label = pm.getApplicationLabel(app).toString(),
                    isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    isEnabled = app.enabled,
                    isInstalled = (app.flags and FLAG_INSTALLED) != 0,
                    usageTimeMs = usage[app.packageName]?.first ?: 0L,
                    lastUsed = usage[app.packageName]?.second ?: 0L
                )
            }
            .sortedByDescending { it.usageTimeMs }
    }

    // Lê o tempo de uso dos últimos 7 dias.
    private fun loadUsageMap(): Map<String, Pair<Long, Long>> {
        if (!hasUsageAccess()) return emptyMap()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val weekAgo = now - 7L * 24 * 60 * 60 * 1000
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, weekAgo, now)

        val map = HashMap<String, Pair<Long, Long>>()
        stats?.forEach { s ->
            val prev = map[s.packageName]
            val total = (prev?.first ?: 0L) + s.totalTimeInForeground
            val last = maxOf(prev?.second ?: 0L, s.lastTimeUsed)
            map[s.packageName] = total to last
        }
        return map
    }
}

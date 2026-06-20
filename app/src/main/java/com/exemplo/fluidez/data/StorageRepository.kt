package com.exemplo.fluidez.data

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.StatFs

class StorageRepository(private val context: Context) {

    /** Panorama geral (total / usado / livre). */
    fun overview(): StorageOverview {
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.totalBytes
        val free = stat.availableBytes
        return StorageOverview(total = total, used = total - free, free = free)
    }

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

    /** Quanto cada app ocupa. Precisa do 'Acesso de uso'. */
    fun appStorage(): List<AppStorage> {
        if (!hasUsageAccess()) return emptyList()
        val pm = context.packageManager
        val ssm = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
        val user = Process.myUserHandle()

        return pm.getInstalledApplications(0).mapNotNull { app ->
            try {
                val stats = ssm.queryStatsForPackage(app.storageUuid, app.packageName, user)
                AppStorage(
                    packageName = app.packageName,
                    label = pm.getApplicationLabel(app).toString(),
                    appBytes = stats.appBytes,
                    dataBytes = stats.dataBytes,
                    cacheBytes = stats.cacheBytes
                )
            } catch (e: Exception) {
                null // alguns apps de sistema bloqueiam a leitura — ignora
            }
        }.sortedByDescending { it.totalBytes }
    }
}

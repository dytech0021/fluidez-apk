package com.exemplo.fluidez.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs

/** Responsável por LER as informações do sistema. */
class SystemInfoRepository(private val context: Context) {

    fun readSnapshot(): SystemSnapshot {
        val (totalRam, usedRam) = readRam()
        val (totalStorage, usedStorage) = readStorage()
        val (percent, charging, temp) = readBattery()

        return SystemSnapshot(
            totalRam = totalRam,
            usedRam = usedRam,
            totalStorage = totalStorage,
            usedStorage = usedStorage,
            batteryPercent = percent,
            isCharging = charging,
            batteryTemperature = temp
        )
    }

    private fun readRam(): Pair<Long, Long> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        val used = info.totalMem - info.availMem
        return info.totalMem to used
    }

    private fun readStorage(): Pair<Long, Long> {
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.totalBytes
        val used = total - stat.availableBytes
        return total to used
    }

    private fun readBattery(): Triple<Int, Boolean, Float> {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val percent = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        val status = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val chargeStatus = status?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = chargeStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
                chargeStatus == BatteryManager.BATTERY_STATUS_FULL

        val tempRaw = status?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        return Triple(percent, isCharging, tempRaw / 10f)
    }
}

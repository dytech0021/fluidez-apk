package com.exemplo.fluidez.data

/** Um "retrato" do estado do aparelho num instante. */
data class SystemSnapshot(
    val totalRam: Long = 0,
    val usedRam: Long = 0,
    val totalStorage: Long = 0,
    val usedStorage: Long = 0,
    val batteryPercent: Int = 0,
    val isCharging: Boolean = false,
    val batteryTemperature: Float = 0f
) {
    val ramUsedFraction: Float
        get() = if (totalRam == 0L) 0f else usedRam.toFloat() / totalRam

    val storageUsedFraction: Float
        get() = if (totalStorage == 0L) 0f else usedStorage.toFloat() / totalStorage
}

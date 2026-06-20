package com.exemplo.fluidez.data

data class StorageOverview(
    val total: Long = 0,
    val used: Long = 0,
    val free: Long = 0
) {
    val usedFraction: Float
        get() = if (total == 0L) 0f else used.toFloat() / total
}

data class AppStorage(
    val packageName: String,
    val label: String,
    val appBytes: Long,    // o APK / código do app
    val dataBytes: Long,   // dados do app (já inclui o cache)
    val cacheBytes: Long   // só a parte de cache (subconjunto de dataBytes)
) {
    val totalBytes: Long get() = appBytes + dataBytes
}

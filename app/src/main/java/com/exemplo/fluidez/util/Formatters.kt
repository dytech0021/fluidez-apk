package com.exemplo.fluidez.util

import java.util.Locale

/** Transforma bytes (ex: 4509715660) em texto legível (ex: "4,2 GB"). */
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, units[unitIndex])
}

/** Estimativa curta de tempo: "2min 5s" ou "8s". */
fun formatEta(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return if (m > 0) "${m}min ${s}s" else "${s}s"
}

/** Transforma milissegundos em "2h 15min". */
fun formatDuration(ms: Long): String {
    if (ms <= 0) return "—"
    val totalMin = ms / 60000
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "${h}h ${m}min" else "${m}min"
}

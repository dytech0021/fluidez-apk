package com.exemplo.fluidez.data

data class AppInfo(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean,
    val isEnabled: Boolean,
    val isInstalled: Boolean,
    val usageTimeMs: Long,
    val lastUsed: Long
)

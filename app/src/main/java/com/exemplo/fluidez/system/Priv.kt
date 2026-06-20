package com.exemplo.fluidez.system

import android.content.Context

/**
 * Escolhe o backend privilegiado ativo: prefere o ADB embutido (se conectado),
 * senão usa o Shizuku.
 */
object Priv {
    fun shell(context: Context): PrivShell {
        AdbManager.init(context)
        return if (AdbManager.isReady()) AdbManager else ShizukuManager(context.applicationContext)
    }
}

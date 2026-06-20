package com.exemplo.fluidez.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object StorageActions {

    /** Limpa TODOS os caches do sistema. */
    fun trimAllCaches(shell: PrivShell, onResult: (Boolean, String) -> Unit) {
        shell.exec("pm trim-caches 9999999999999", onResult)
    }

    /** Para todos os apps de usuário (terceiros) — libera RAM/CPU em segundo plano. */
    fun stopUserApps(shell: PrivShell, onResult: (Boolean, String) -> Unit) {
        shell.exec(
            "for p in \$(pm list packages -3 | cut -d: -f2); do am force-stop \$p; done",
            onResult
        )
    }

    /** Abre os detalhes do app (onde dá pra limpar cache/dados na mão). */
    fun openAppDetails(context: Context, pkg: String) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

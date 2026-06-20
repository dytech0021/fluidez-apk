package com.exemplo.fluidez.system

import android.content.Context
import android.content.Intent
import android.net.Uri

object AppActions {

    /** Apps do usuário: abre o diálogo padrão de desinstalar. */
    fun uninstallUserApp(context: Context, pkg: String) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Bloatware do sistema: desativa via shell privilegiado (reversível). */
    fun disableSystemApp(
        shell: PrivShell, pkg: String, onResult: (Boolean, String) -> Unit
    ) = shell.exec("pm disable-user --user 0 $pkg", onResult)

    fun enableApp(
        shell: PrivShell, pkg: String, onResult: (Boolean, String) -> Unit
    ) = shell.exec("pm enable $pkg", onResult)

    /** Força a parada de um app (libera recursos na hora). */
    fun forceStop(
        shell: PrivShell, pkg: String, onResult: (Boolean, String) -> Unit
    ) = shell.exec("am force-stop $pkg", onResult)

    /** Remove o app para o usuário atual — funciona em apps de sistema/protegidos. Reversível. */
    fun uninstallForUser(
        shell: PrivShell, pkg: String, onResult: (Boolean, String) -> Unit
    ) = shell.exec("pm uninstall --user 0 $pkg", onResult)

    /** Restaura um app que foi removido para o usuário. */
    fun reinstallForUser(
        shell: PrivShell, pkg: String, onResult: (Boolean, String) -> Unit
    ) = shell.exec("cmd package install-existing $pkg", onResult)
}

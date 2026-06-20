package com.exemplo.fluidez.system

import android.content.Context

object MaintenanceActions {
    /**
     * Recompila os apps UM A UM (pra poder mostrar progresso).
     * onProgress(feitos, total, pacoteAtual) é chamado antes de cada app.
     * onDone(sucesso) ao terminar.
     */
    fun optimizeAllApps(
        context: Context,
        shell: PrivShell,
        onProgress: (Int, Int, String) -> Unit,
        onDone: (Boolean) -> Unit
    ) {
        val pkgs = try {
            context.packageManager.getInstalledApplications(0).map { it.packageName }.sorted()
        } catch (e: Exception) {
            emptyList()
        }

        if (pkgs.isEmpty()) {
            onDone(false)
            return
        }

        fun step(i: Int) {
            if (i >= pkgs.size) {
                onProgress(pkgs.size, pkgs.size, "")
                onDone(true)
                return
            }
            onProgress(i, pkgs.size, pkgs[i])
            // Ignora falha individual (alguns apps de sistema recusam) e segue.
            shell.exec("pm compile -m speed -f ${pkgs[i]}") { _, _ ->
                step(i + 1)
            }
        }
        step(0)
    }
}

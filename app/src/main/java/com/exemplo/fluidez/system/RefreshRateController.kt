package com.exemplo.fluidez.system

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display

/** Lê e força a taxa de atualização da tela. */
object RefreshRateController {

    /** Maior taxa de atualização suportada pela tela (Hz). */
    fun maxSupported(context: Context): Int {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = dm.getDisplay(Display.DEFAULT_DISPLAY) ?: return 60
        return (display.supportedModes.maxOfOrNull { it.refreshRate } ?: 60f).toInt()
    }

    /** Trava a tela na taxa máxima. */
    fun forceMax(context: Context, shell: PrivShell, onResult: (Boolean, String) -> Unit) {
        val r = maxSupported(context)
        shell.exec(
            "settings put system min_refresh_rate $r ; settings put system peak_refresh_rate $r",
            onResult
        )
    }

    /** Volta ao comportamento automático do sistema. */
    fun restore(shell: PrivShell, onResult: (Boolean, String) -> Unit) {
        shell.exec(
            "settings delete system min_refresh_rate ; settings delete system peak_refresh_rate",
            onResult
        )
    }
}

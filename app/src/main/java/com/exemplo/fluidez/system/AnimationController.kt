package com.exemplo.fluidez.system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/** Responsável por LER e ESCREVER as 3 escalas de animação do sistema. */
object AnimationController {

    private val keys = listOf(
        Settings.Global.WINDOW_ANIMATION_SCALE,
        Settings.Global.TRANSITION_ANIMATION_SCALE,
        Settings.Global.ANIMATOR_DURATION_SCALE
    )

    /** O app já recebeu o "poder" de mexer nessas configs? */
    fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    /** Coloca as 3 escalas no valor desejado (0f = zero/instantâneo). */
    fun setScale(context: Context, scale: Float): Boolean {
        if (!hasPermission(context)) return false
        return try {
            keys.all { key -> Settings.Global.putFloat(context.contentResolver, key, scale) }
        } catch (e: SecurityException) {
            false
        }
    }

    fun setZero(context: Context) = setScale(context, 0f)

    /** Aplica as 3 escalas via shell (ADB/Shizuku), sem precisar da permissão nativa. */
    fun setScaleViaShell(shell: PrivShell, scale: Float, onResult: (Boolean, String) -> Unit) {
        val cmd = keys.joinToString(" ; ") { "settings put global $it $scale" }
        shell.exec(cmd, onResult)
    }

    /** Confere se as 3 já estão em zero. */
    fun areAllZero(context: Context): Boolean =
        keys.all { Settings.Global.getFloat(context.contentResolver, it, 1f) == 0f }
}

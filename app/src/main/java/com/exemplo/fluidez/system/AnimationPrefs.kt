package com.exemplo.fluidez.system

import android.content.Context

/** Lembra as escolhas do usuário (travar em zero / vigia / taxa de atualização). */
object AnimationPrefs {
    private const val FILE = "fluidez"
    private const val KEY_LOCK = "lock_zero"
    private const val KEY_GUARD = "guard"
    private const val KEY_REFRESH = "refresh_forced"

    fun isLockEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LOCK, false)

    fun setLockEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_LOCK, enabled).apply()
    }

    fun isGuardEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_GUARD, false)

    fun setGuardEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_GUARD, enabled).apply()
    }

    fun isRefreshForced(context: Context): Boolean =
        prefs(context).getBoolean(KEY_REFRESH, false)

    fun setRefreshForced(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_REFRESH, enabled).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}

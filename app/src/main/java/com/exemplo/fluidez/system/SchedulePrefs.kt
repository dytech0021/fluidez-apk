package com.exemplo.fluidez.system

import android.content.Context

object SchedulePrefs {
    private const val FILE = "fluidez_schedule"

    data class Slot(val enabled: Boolean, val hour: Int, val minute: Int, val profileId: String)

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun get(context: Context, slot: Int): Slot {
        val p = prefs(context)
        return Slot(
            enabled = p.getBoolean("en_$slot", false),
            hour = p.getInt("h_$slot", if (slot == 0) 22 else 7),
            minute = p.getInt("m_$slot", 0),
            profileId = p.getString("pf_$slot", if (slot == 0) "economy" else "normal") ?: "normal"
        )
    }

    fun set(context: Context, slot: Int, s: Slot) {
        prefs(context).edit()
            .putBoolean("en_$slot", s.enabled)
            .putInt("h_$slot", s.hour)
            .putInt("m_$slot", s.minute)
            .putString("pf_$slot", s.profileId)
            .apply()
    }
}

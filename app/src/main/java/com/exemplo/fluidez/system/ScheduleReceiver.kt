package com.exemplo.fluidez.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Disparado pelo alarme: aplica o perfil agendado. */
class ScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val profileId = intent.getStringExtra("profile") ?: return
        val profile = ProfileManager.byId(profileId)
        ProfileManager.apply(context, Priv.shell(context), profile) { }
    }
}

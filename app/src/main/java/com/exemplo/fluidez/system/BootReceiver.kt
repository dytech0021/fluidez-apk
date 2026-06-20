package com.exemplo.fluidez.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Reaplica a trava de animação, se ativada.
        if (AnimationPrefs.isLockEnabled(context)) {
            AnimationController.setZero(context)
            if (AnimationPrefs.isGuardEnabled(context)) {
                GuardController.start(context)
            }
        }

        // Reaplica a taxa de atualização forçada, se ativada.
        if (AnimationPrefs.isRefreshForced(context)) {
            RefreshRateController.forceMax(context, Priv.shell(context)) { _, _ -> }
        }

        // Reagenda os perfis automáticos.
        ScheduleManager.reschedule(context)
    }
}

package com.exemplo.fluidez.system

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

object GuardController {
    fun start(context: Context) {
        ContextCompat.startForegroundService(
            context, Intent(context, AnimationGuardService::class.java)
        )
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, AnimationGuardService::class.java))
    }
}

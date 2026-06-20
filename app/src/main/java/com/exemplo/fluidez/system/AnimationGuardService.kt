package com.exemplo.fluidez.system

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings

/** Fica "de olho" nas 3 escalas e zera de novo se algo mexer. */
class AnimationGuardService : Service() {

    companion object {
        private const val CHANNEL_ID = "animation_guard"
        private const val NOTIF_ID = 1
    }

    private val keys = listOf(
        Settings.Global.WINDOW_ANIMATION_SCALE,
        Settings.Global.TRANSITION_ANIMATION_SCALE,
        Settings.Global.ANIMATOR_DURATION_SCALE
    )

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            if (!AnimationController.areAllZero(this@AnimationGuardService)) {
                AnimationController.setZero(this@AnimationGuardService)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        keys.forEach { key ->
            contentResolver.registerContentObserver(
                Settings.Global.getUriFor(key), false, observer
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AnimationController.setZero(this)
        return START_STICKY
    }

    override fun onDestroy() {
        contentResolver.unregisterContentObserver(observer)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Trava de animação", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Fluidez ativo")
            .setContentText("Mantendo as animações em zero")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }
}

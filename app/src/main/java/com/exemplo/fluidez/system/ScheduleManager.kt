package com.exemplo.fluidez.system

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object ScheduleManager {
    private val SLOTS = listOf(0, 1)

    /** (Re)agenda todos os slots conforme as preferências. */
    fun reschedule(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        SLOTS.forEach { slot ->
            val s = SchedulePrefs.get(context, slot)
            val pi = pendingIntent(context, slot, s.profileId)
            am.cancel(pi)
            if (s.enabled) {
                am.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    nextTrigger(s.hour, s.minute),
                    AlarmManager.INTERVAL_DAY,
                    pi
                )
            }
        }
    }

    private fun pendingIntent(context: Context, slot: Int, profileId: String): PendingIntent {
        val intent = Intent(context, ScheduleReceiver::class.java).apply {
            action = "com.exemplo.fluidez.SLOT_$slot"
            putExtra("profile", profileId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, slot, intent, flags)
    }

    private fun nextTrigger(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= now.timeInMillis) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }
}

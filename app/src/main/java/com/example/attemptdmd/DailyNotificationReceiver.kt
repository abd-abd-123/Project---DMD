// DailyNotificationReceiver.kt
package com.example.attemptdmd

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

import android.util.Log

class DailyNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DailyNotification::WakeLock")
        wakeLock.acquire(10 * 60 * 1000L) // Hold for 10 minutes (adjust as needed)

        val notificationHelper = NotificationHelper(context)
        notificationHelper.showNotification(
            "hi bro",
            "u good?"
        )

        wakeLock.release()
    }
}

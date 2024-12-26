package com.example.attemptdmd

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import java.util.Calendar
import android.app.AlarmManager
import android.app.PendingIntent

class DailyNotificationReceiver : BroadcastReceiver() {


    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("DailyNotificationReceiver", "onReceive() called")

        // Acquire a wake lock, just in case the device is sleeping
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "DailyNotification::WakeLock"
        )
        wakeLock.acquire(10 * 60 * 1000L) // e.g., 10 minutes

        // Show the notification
        val notificationHelper = NotificationHelper(context)
        notificationHelper.showNotification("Hi bro", "You good?")

        // Immediately schedule the next day's alarm.
        // E.g., do it for the same HOUR_OF_DAY, MINUTE, etc.
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val newIntent = Intent(context, DailyNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            newIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)  // schedule tomorrow
            set(Calendar.HOUR_OF_DAY, 23) // or any time you want
            set(Calendar.MINUTE, 27)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Use exact alarm for tomorrow
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
        Log.d("DailyNotificationReceiver", "Re-scheduled next alarm for tomorrow at 23:27")

        wakeLock.release()
    }
}

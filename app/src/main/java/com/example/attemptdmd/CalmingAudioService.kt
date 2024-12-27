package com.example.attemptdmd

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class CalmingAudioService : Service() {

    private val CHANNEL_ID = "CalmingAudioChannel"
    private val NOTIF_ID = 123

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("CalmingAudioService", "onCreate() called")
        createNotificationChannel()

        mediaPlayer = MediaPlayer.create(this, R.raw.calming_music).apply {
            isLooping = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("CalmingAudioService", "onStartCommand() called")

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Calming Audio")
            .setContentText("Playing soothing background music...")
            .setSmallIcon(R.drawable.ic_send)
            .setOngoing(true)
            .build()

        // Foreground Service
        startForeground(NOTIF_ID, notification)
        mediaPlayer?.start()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CalmingAudioService", "onDestroy() called")

        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Calming Audio Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for background calming audio"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}

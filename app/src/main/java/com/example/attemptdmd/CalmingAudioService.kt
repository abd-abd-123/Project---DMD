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

    // Notification constants
    private val CHANNEL_ID = "CalmingAudioChannel"
    private val NOTIF_ID = 123

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("CalmingAudioService", "onCreate() called")
        createNotificationChannel()

        // Initialize MediaPlayer with your raw resource
        // R.raw.calming_music is your file name in /res/raw/
        mediaPlayer = MediaPlayer.create(this, R.raw.calming_music).apply {
            isLooping = true // Loop the audio if you want continuous playback
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("CalmingAudioService", "onStartCommand() called")

        // Build the notification to show while playing
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Calming Audio")
            .setContentText("Playing soothing background music...")
            .setSmallIcon(R.drawable.ic_send) // or another drawable you have
            .setOngoing(true) // Mark as an ongoing notification
            .build()

        // Start foreground service
        startForeground(NOTIF_ID, notification)

        // Start playing audio if not already playing
        mediaPlayer?.start()

        // If the service is killed, we want to restart (or can choose otherwise)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CalmingAudioService", "onDestroy() called")

        // Cleanup MediaPlayer
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // Foreground service must override onBind; return null if not a bound service
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

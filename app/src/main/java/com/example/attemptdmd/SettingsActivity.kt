package com.example.attemptdmd

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import java.util.Calendar

class SettingsActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var themeSwitch: SwitchCompat
    private lateinit var notifSwitch: SwitchCompat
    private lateinit var audioSwitch: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        // We can load the theme preference before setContentView if we want the theme to apply here
        sharedPreferences = getSharedPreferences("chat_prefs", MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)
        applyTheme(isDarkMode, immediate = false)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<MaterialToolbar>(R.id.chatToolbar)
        // In onCreate of SettingsActivity
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)  // show the back arrow
        toolbar.setNavigationOnClickListener {
            // If user taps the back arrow, go back:
            finish()
        }

        // Initialize Switches
        themeSwitch = findViewById(R.id.switchDarkMode)
        notifSwitch = findViewById(R.id.switchDailyNotif)
        audioSwitch = findViewById(R.id.switchAudio)

        // Load states
        themeSwitch.isChecked = isDarkMode
        val isNotificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true)
        notifSwitch.isChecked = isNotificationsEnabled
        val isAudioOn = sharedPreferences.getBoolean("audio_on", false)
        audioSwitch.isChecked = isAudioOn

        // Dark Mode
        themeSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                sharedPreferences.edit().putBoolean("dark_mode", isChecked).apply()
                // Immediately re-apply theme in *this* activity
                applyTheme(isChecked, immediate = true)
            }
        }


        // Daily Notifications
        notifSwitch.setOnCheckedChangeListener { _, checked ->
            sharedPreferences.edit().putBoolean("notifications_enabled", checked).apply()
            if (checked) {
                scheduleDailyNotification()
            } else {
                cancelNotification()
            }
        }

        // Calming Audio
        audioSwitch.setOnCheckedChangeListener { _, checked ->
            sharedPreferences.edit().putBoolean("audio_on", checked).apply()
            if (checked) {
                startCalmingAudioService()
            } else {
                stopCalmingAudioService()
            }
        }

        // If you want to request POST_NOTIFICATIONS permission here
        requestNotificationPermissionIfNeeded()
    }

    /**
     * Actually apply the app theme if user toggles dark mode.
     * We can call recreate() if immediate = true to redraw this activity with new theme.
     */
    private fun applyTheme(darkMode: Boolean, immediate: Boolean) {
        if (darkMode) {
            setTheme(R.style.Theme_AttemptDMD_Dark)
        } else {
            setTheme(R.style.Theme_AttemptDMD_Light)
        }
        if (immediate) {
            recreate()
        }
    }

    /**
     * Request for POST_NOTIFICATIONS permission on Android 13+ if needed.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    /**
     * EXACT alarm scheduling logic
     */
    private fun ensureExactAlarmPermission(grantedCallback: () -> Unit) {
        val alarmManager = getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(this, "Need exact alarm permission", Toast.LENGTH_LONG).show()
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            } else {
                grantedCallback()
            }
        } else {
            grantedCallback()
        }
    }

    fun scheduleDailyNotification() {
        ensureExactAlarmPermission {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, DailyNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                // Example time:
                set(Calendar.HOUR_OF_DAY, 18)
                set(Calendar.MINUTE, 35)
                set(Calendar.SECOND, 0)
                if (timeInMillis < System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Toast.makeText(this, "Daily notification scheduled at ${calendar.time}", Toast.LENGTH_SHORT).show()
        }
    }

    fun cancelNotification() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, DailyNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun startCalmingAudioService() {
        val intent = Intent(this, CalmingAudioService::class.java)
        startService(intent)
    }

    private fun stopCalmingAudioService() {
        val intent = Intent(this, CalmingAudioService::class.java)
        stopService(intent)
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // If user taps "Settings" again, we might do nothing,
                // because we are already in SettingsActivity.
                // Or maybe show a Toast, etc.
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

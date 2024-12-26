package com.example.attemptdmd

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.concurrent.thread
import android.content.SharedPreferences
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import androidx.appcompat.widget.SwitchCompat

private val REQUEST_NOTIFICATION_PERMISSION = 101
private lateinit var db: ChatDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var rvChatMessages: RecyclerView
    private lateinit var btnSendRequest: ImageButton
    private lateinit var etUserMessage: EditText
    private val chatAdapter = ChatAdapter(mutableListOf())

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var themeSwitch: SwitchCompat
    private lateinit var notifSwitch: SwitchCompat

    private var loadingItem: ChatItem.Loading? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1) Load prefs before super.onCreate & setContentView
        sharedPreferences = getSharedPreferences("chat_prefs", MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)

        // 2) Apply the stored theme here to avoid flicker on launch
        applyTheme(isDarkMode, immediate = false)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnPlayAudio = findViewById<Button>(R.id.btnPlayAudio)
        val btnStopAudio = findViewById<Button>(R.id.btnStopAudio)

        btnPlayAudio.setOnClickListener {
            startCalmingAudioService()
        }

        btnStopAudio.setOnClickListener {
            stopCalmingAudioService()
        }

        // Initialize Room database
        db = ChatDatabase.getDatabase(this)

        // Initialize switches
        themeSwitch = findViewById(R.id.switchTheme)
        notifSwitch = findViewById(R.id.switchNotifications)

        // Set switch states based on saved preferences
        themeSwitch.isChecked = isDarkMode
        val isNotificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true)
        notifSwitch.isChecked = isNotificationsEnabled

        // Listen for theme switch toggles
        themeSwitch.setOnCheckedChangeListener { buttonView, checked ->
            if (buttonView.isPressed) {
                sharedPreferences.edit().putBoolean("dark_mode", checked).apply()
                applyTheme(checked, immediate = false)
                recreate()
            }
        }

        // Listen for notifications switch toggles
        notifSwitch.setOnCheckedChangeListener { _, checked ->
            sharedPreferences.edit().putBoolean("notifications_enabled", checked).apply()
            if (checked) {
                scheduleDailyNotification()
            } else {
                cancelNotification()
            }
        }

        // Schedule initial notification if enabled
        if (isNotificationsEnabled) {
            scheduleDailyNotification()
        }

        // Request notification permissions (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }

        // Initialize UI elements
        rvChatMessages = findViewById(R.id.rvChatMessages)
        btnSendRequest = findViewById(R.id.btnSendRequest)
        etUserMessage = findViewById(R.id.etUserMessage)

        rvChatMessages.layoutManager = LinearLayoutManager(this)
        rvChatMessages.adapter = chatAdapter

        // Load chat history from Room database
        loadChatHistoryFromDb()

        // Handle message sending
        btnSendRequest.setOnClickListener {
            val userMessage = etUserMessage.text.toString().trim()
            if (userMessage.isNotEmpty()) {
                val chatMessage = ChatItem.Message(userMessage, true)
                chatAdapter.addItem(chatMessage)
                etUserMessage.setText("")

                loadingItem = ChatItem.Loading
                chatAdapter.addItem(loadingItem!!)
                rvChatMessages.scrollToPosition(chatAdapter.itemCount - 1)

                saveMessageToHistory(chatMessage)
                sendChatMessage(userMessage)
            } else {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Apply light/dark theme. If 'immediate' is false, we just call setTheme.
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

    // Cancel daily notification if disabled
    fun cancelNotification() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, DailyNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    // Save a chat message to Room database
    private fun saveMessageToHistory(message: ChatItem.Message) {
        val chatMessage = ChatMessage(
            text = message.text,
            isUser = message.isUser,
            timestamp = System.currentTimeMillis()
        )
        lifecycleScope.launch {
            db.chatDao().insertMessage(chatMessage)
        }
    }

    // Load chat history from Room database
    private fun loadChatHistoryFromDb() {
        lifecycleScope.launch {
            val messages = db.chatDao().getAllMessages()
            runOnUiThread {
                messages.forEach { msg ->
                    val chatItem = ChatItem.Message(msg.text, msg.isUser)
                    chatAdapter.addItem(chatItem)
                }
            }
        }
    }

    // Send chat message to the Flask API
    private fun sendChatMessage(message: String) {
        thread {
            try {
                val jsonRequest = JSONObject().apply {
                    put("user_message", message)
                }

                val url = URL("https://flask-chatbot-3uw8.onrender.com/chat")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doInput = true
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

                conn.outputStream.use { it.write(jsonRequest.toString().toByteArray(Charsets.UTF_8)) }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val jsonResponse = JSONObject(response)
                    val assistantResponse = jsonResponse.optString("assistant", "No response")

                    runOnUiThread {
                        loadingItem?.let { chatAdapter.removeItem(it) }
                        val assistantMessage = ChatItem.Message(assistantResponse, false)
                        chatAdapter.addItem(assistantMessage)
                        saveMessageToHistory(assistantMessage)

                        rvChatMessages.scrollToPosition(chatAdapter.itemCount - 1)

                        // Show immediate in-app notification
                        val notificationHelper = NotificationHelper(this)
                        notificationHelper.showNotification("New Message", assistantResponse)
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    loadingItem?.let { chatAdapter.removeItem(it) }
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Schedule a one-time exact alarm for (e.g.) 23:27.
     * Then "DailyNotificationReceiver" will handle scheduling tomorrow's alarm.
     */
    private fun ensureExactAlarmPermission(grantedCallback: () -> Unit) {
        val alarmManager = getSystemService(AlarmManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requires checking canScheduleExactAlarms
            if (!alarmManager.canScheduleExactAlarms()) {
                // Permission not granted; show a dialog or toast, then request it:
                Toast.makeText(this, "Need exact alarm permission", Toast.LENGTH_LONG).show()
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            } else {
                // We already have permission
                grantedCallback()
            }
        } else {
            // Before Android 12, no special permission needed
            grantedCallback()
        }
    }
    fun scheduleDailyNotification() {
        ensureExactAlarmPermission {
            // This callback runs only if canScheduleExactAlarms() is true.
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, DailyNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 18)
                set(Calendar.MINUTE, 35)
                set(Calendar.SECOND, 0)
                if (timeInMillis < System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            // This exact alarm call is now safe
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d("DailyNotificationReceiver", "Scheduled next alarm at ${calendar.time}")

//            Toast.makeText(this, "Exact daily alarm scheduled at 23:36", Toast.LENGTH_SHORT).show()
        }
    }
    private fun startCalmingAudioService() {
        val intent = Intent(this, CalmingAudioService::class.java)
        startService(intent)
    }

    // Stop
    private fun stopCalmingAudioService() {
        val intent = Intent(this, CalmingAudioService::class.java)
        stopService(intent)
    }

}

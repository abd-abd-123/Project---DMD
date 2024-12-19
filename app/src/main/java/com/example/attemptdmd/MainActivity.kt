package com.example.attemptdmd

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
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

private val REQUEST_NOTIFICATION_PERMISSION = 101

class MainActivity : AppCompatActivity() {

    private lateinit var rvChatMessages: RecyclerView
    private lateinit var btnSendRequest: ImageButton
    private lateinit var etUserMessage: EditText
    private val chatAdapter = ChatAdapter(mutableListOf())

    private lateinit var sharedPreferences: SharedPreferences

    private var loadingItem: ChatItem.Loading? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scheduleDailyNotification()

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

        rvChatMessages = findViewById(R.id.rvChatMessages)
        btnSendRequest = findViewById(R.id.btnSendRequest)
        etUserMessage = findViewById(R.id.etUserMessage)

        rvChatMessages.layoutManager = LinearLayoutManager(this)
        rvChatMessages.adapter = chatAdapter

        sharedPreferences = getSharedPreferences("chat_prefs", MODE_PRIVATE)

        loadChatHistory()

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

    private fun sendChatMessage(message: String) {
        thread {
            try {
                val jsonRequest = JSONObject().apply { put("user_message", message) }

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
                    val disclaimer = jsonResponse.optString("disclaimer", "")

                    runOnUiThread {
                        loadingItem?.let { chatAdapter.removeItem(it) }
                        val assistantMessage = ChatItem.Message(assistantResponse, false, disclaimer)
                        chatAdapter.addItem(assistantMessage)
                        saveMessageToHistory(assistantMessage)
                        rvChatMessages.scrollToPosition(chatAdapter.itemCount - 1)

                        // Show notification
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

    fun scheduleDailyNotification() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, DailyNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(this, "Permission required to schedule exact alarms", Toast.LENGTH_LONG).show()
                // Direct the user to settings to grant the permission
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                return
            }
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 28)
            set(Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        Log.d("NotificationDebug", "Alarm scheduled for: ${calendar.time}")

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to schedule exact alarm: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


    private fun saveMessageToHistory(message: ChatItem.Message) {
        val chatHistory = getChatHistory()
        chatHistory.add(message.toJson())
        sharedPreferences.edit().putString("chat_history", JSONArray(chatHistory).toString()).apply()
    }

    private fun loadChatHistory() {
        val jsonArray = JSONArray(sharedPreferences.getString("chat_history", "[]"))
        for (i in 0 until jsonArray.length()) {
            chatAdapter.addItem(jsonArray.getJSONObject(i).toChatMessage())
        }
    }

    private fun getChatHistory(): MutableList<JSONObject> =
        JSONArray(sharedPreferences.getString("chat_history", "[]")).let { jsonArray ->
            MutableList(jsonArray.length()) { i -> jsonArray.getJSONObject(i) }
        }
}
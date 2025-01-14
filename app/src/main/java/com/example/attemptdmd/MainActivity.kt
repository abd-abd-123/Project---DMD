package com.example.attemptdmd

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.concurrent.thread
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.appbar.MaterialToolbar

class MainActivity : AppCompatActivity() {
    private lateinit var rvChatMessages: RecyclerView
    private lateinit var btnSendRequest: ImageButton
    private lateinit var etUserMessage: EditText
    private val chatAdapter = ChatAdapter(mutableListOf())

    private var lastIsDarkMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPrefs = getSharedPreferences("chat_prefs", MODE_PRIVATE)
        val isDarkMode = sharedPrefs.getBoolean("dark_mode", false)
        lastIsDarkMode = isDarkMode

        // 2) Apply dark or light theme
        if (isDarkMode) {
            setTheme(R.style.Theme_AttemptDMD_Dark)
        } else {
            setTheme(R.style.Theme_AttemptDMD)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.chatToolbar)
        setSupportActionBar(toolbar)
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

                val loadingItem = ChatItem.Loading
                chatAdapter.addItem(loadingItem)
                rvChatMessages.scrollToPosition(chatAdapter.itemCount - 1)

                saveMessageToHistory(chatMessage)
                sendChatMessage(userMessage)
            } else {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val sharedPrefs = getSharedPreferences("chat_prefs", MODE_PRIVATE)

        val isDarkMode = sharedPrefs.getBoolean("dark_mode", false)
        if (isDarkMode != lastIsDarkMode) {
            lastIsDarkMode = isDarkMode
            recreate()
        }
    }

//    private fun reapplyTheme() {
//        val sharedPrefs = getSharedPreferences("chat_prefs", MODE_PRIVATE)
//        val isDarkMode = sharedPrefs.getBoolean("dark_mode", false)
//
//        if (isDarkMode) {
//            setTheme(R.style.Theme_AttemptDMD_Dark)
//        } else {
//            setTheme(R.style.Theme_AttemptDMD)
//        }
//        recreate()
//    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // Launch SettingsActivity
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveMessageToHistory(message: ChatItem.Message) {
        val chatMessage = ChatMessage(
            text = message.text,
            isUser = message.isUser,
            timestamp = System.currentTimeMillis()
        )
        lifecycleScope.launch {
            ChatDatabase.getDatabase(this@MainActivity).chatDao().insertMessage(chatMessage)
        }
    }

    private fun loadChatHistoryFromDb() {
        lifecycleScope.launch {
            val messages = ChatDatabase.getDatabase(this@MainActivity)
                .chatDao()
                .getAllMessages()
            runOnUiThread {
                messages.forEach { msg ->
                    val chatItem = ChatItem.Message(msg.text, msg.isUser)
                    chatAdapter.addItem(chatItem)
                }
            }
        }
    }

    private fun sendChatMessage(message: String) {
        thread {
            try {
                val jsonRequest = JSONObject().apply {
                    put("message", message)
                }

                val url = URL("https://abd-dmd-server-a72a6febb9c5.herokuapp.com/chat")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doInput = true
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

                conn.outputStream.use { it.write(jsonRequest.toString().toByteArray(Charsets.UTF_8)) }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val jsonResponse = JSONObject(response)
                    val assistantResponse = jsonResponse.optString("reply", "No response")

                    runOnUiThread {
                        chatAdapter.removeItem(ChatItem.Loading)
                        val assistantMessage = ChatItem.Message(assistantResponse, false)
                        chatAdapter.addItem(assistantMessage)
                        saveMessageToHistory(assistantMessage)
                        rvChatMessages.scrollToPosition(chatAdapter.itemCount - 1)

                        // Show immediate in-app notification
                        val notificationHelper = NotificationHelper(this@MainActivity)
                        notificationHelper.showNotification("New Message", assistantResponse)
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    chatAdapter.removeItem(ChatItem.Loading)
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
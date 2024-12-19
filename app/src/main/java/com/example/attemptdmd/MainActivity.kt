package com.example.kotlin_dmd_pro

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var rvChatMessages: RecyclerView
    private lateinit var btnSendRequest: ImageButton
    private lateinit var etUserMessage: EditText
    private val chatAdapter = ChatAdapter(mutableListOf())

    // Keep a reference to the loading item to remove it later
    private var loadingItem: ChatItem.Loading? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvChatMessages = findViewById(R.id.rvChatMessages)
        btnSendRequest = findViewById(R.id.btnSendRequest)
        etUserMessage = findViewById(R.id.etUserMessage)

        rvChatMessages.layoutManager = LinearLayoutManager(this)
        rvChatMessages.adapter = chatAdapter

        btnSendRequest.setOnClickListener {
            val userMessage = etUserMessage.text.toString().trim()
            if (userMessage.isNotEmpty()) {
                // Add user message to UI
                chatAdapter.addItem(ChatItem.Message(userMessage, true))
                etUserMessage.setText("")
                // Add loading indicator
                loadingItem = ChatItem.Loading
                chatAdapter.addItem(loadingItem!!)
                rvChatMessages.scrollToPosition(chatAdapter.itemCount - 1)
                // Send chat message
                sendChatMessage(userMessage)
            } else {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

                conn.outputStream.use { output ->
                    output.write(jsonRequest.toString().toByteArray(Charsets.UTF_8))
                    output.flush()
                }

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseBuilder = StringBuilder()
                    conn.inputStream.bufferedReader().use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            responseBuilder.append(line)
                        }
                    }

                    val response = responseBuilder.toString()
                    val jsonResponse = JSONObject(response)
                    val assistantResponse = jsonResponse.optString("assistant", "No response")
                    val disclaimer = jsonResponse.optString("disclaimer", "")

                    runOnUiThread {
                        // Remove loading indicator
                        if (loadingItem != null) {
                            val index = chatAdapter.items.indexOf<ChatItem>(loadingItem!!)
                            if (index != -1) {
                                chatAdapter.removeItem(loadingItem!!)
                            }
                        }
                        // Add assistant message with typewriter effect
                        chatAdapter.addItem(ChatItem.Message(assistantResponse, false, disclaimer))
                        rvChatMessages.scrollToPosition(chatAdapter.itemCount - 1)
                    }
                } else {
                    runOnUiThread {
                        // Remove loading indicator
                        if (loadingItem != null) {
                            val index = chatAdapter.items.indexOf<ChatItem>(loadingItem!!)
                            if (index != -1) {
                                chatAdapter.removeItem(loadingItem!!)
                            }
                        }
                        Toast.makeText(
                            this@MainActivity,
                            "Response not successful: $responseCode",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    // Remove loading indicator
                    if (loadingItem != null) {
                        val index = chatAdapter.items.indexOf<ChatItem>(loadingItem!!)
                        if (index != -1) {
                            chatAdapter.removeItem(loadingItem!!)
                        }
                    }
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

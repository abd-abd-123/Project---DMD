package com.example.attemptdmd

import org.json.JSONObject

sealed class ChatItem {
    data class Message(val text: String, val isUser: Boolean, val disclaimer: String = "") : ChatItem()
    object Loading : ChatItem()
}

fun ChatItem.Message.toJson(): JSONObject = JSONObject().apply {
    put("text", text)
    put("isUser", isUser)
    put("disclaimer", disclaimer)
}

fun JSONObject.toChatMessage(): ChatItem.Message = ChatItem.Message(
    getString("text"),
    getBoolean("isUser"),
    optString("disclaimer", "")
)

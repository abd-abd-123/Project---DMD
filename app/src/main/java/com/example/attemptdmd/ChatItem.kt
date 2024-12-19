package com.example.kotlin_dmd_pro

sealed class ChatItem {
    data class Message(val text: String, val isUserMessage: Boolean, val disclaimer: String = "") : ChatItem()
    object Loading : ChatItem()
}

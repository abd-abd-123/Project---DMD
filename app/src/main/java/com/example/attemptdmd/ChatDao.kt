package com.example.attemptdmd

import androidx.room.*

@Dao
interface ChatDao {
    @Insert
    suspend fun insertMessage(message: ChatMessage): Long  // Return Long (row ID)

    @Query("SELECT * FROM chat_history ORDER BY timestamp DESC")
    suspend fun getAllMessages(): List<ChatMessage>  // Return List<ChatMessage>

    @Query("DELETE FROM chat_history")
    suspend fun deleteAll(): Int  // Return Int for rows deleted
}

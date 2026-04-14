package com.arcadiapps.localIA.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageRole { USER, ASSISTANT, SYSTEM }
enum class MessageType { TEXT, IMAGE, AUDIO }

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val role: MessageRole,
    val content: String,
    val type: MessageType = MessageType.TEXT,
    val mediaPath: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val modelId: String = "",
    val tokensUsed: Int = 0
)

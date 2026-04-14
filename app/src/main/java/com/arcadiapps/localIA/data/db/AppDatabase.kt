package com.arcadiapps.localIA.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.arcadiapps.localIA.data.dao.AIModelDao
import com.arcadiapps.localIA.data.dao.ChatMessageDao
import com.arcadiapps.localIA.data.dao.ChatSessionDao
import com.arcadiapps.localIA.data.model.AIModel
import com.arcadiapps.localIA.data.model.ChatMessage
import com.arcadiapps.localIA.data.model.ChatSession

@Database(
    entities = [AIModel::class, ChatMessage::class, ChatSession::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun aiModelDao(): AIModelDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun chatSessionDao(): ChatSessionDao
}

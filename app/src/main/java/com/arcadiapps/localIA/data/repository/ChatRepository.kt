package com.arcadiapps.localIA.data.repository

import com.arcadiapps.localIA.data.dao.ChatMessageDao
import com.arcadiapps.localIA.data.dao.ChatSessionDao
import com.arcadiapps.localIA.data.model.ChatMessage
import com.arcadiapps.localIA.data.model.ChatSession
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val messageDao: ChatMessageDao,
    private val sessionDao: ChatSessionDao
) {
    val allSessions: Flow<List<ChatSession>> = sessionDao.getAllSessions()

    fun getMessages(sessionId: String): Flow<List<ChatMessage>> =
        messageDao.getMessagesForSession(sessionId)

    suspend fun getMessagesSync(sessionId: String): List<ChatMessage> =
        messageDao.getMessagesForSessionSync(sessionId)

    suspend fun createSession(session: ChatSession) = sessionDao.insertSession(session)

    suspend fun updateSession(session: ChatSession) = sessionDao.updateSession(session)

    suspend fun deleteSession(session: ChatSession) {
        messageDao.deleteMessagesForSession(session.id)
        sessionDao.deleteSession(session)
    }

    suspend fun addMessage(message: ChatMessage): Long = messageDao.insertMessage(message)

    suspend fun clearSession(sessionId: String) = messageDao.deleteMessagesForSession(sessionId)
}

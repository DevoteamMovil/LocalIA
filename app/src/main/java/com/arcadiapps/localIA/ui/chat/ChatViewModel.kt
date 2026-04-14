package com.arcadiapps.localIA.ui.chat

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcadiapps.localIA.data.model.ChatMessage
import com.arcadiapps.localIA.data.model.ChatSession
import com.arcadiapps.localIA.data.model.MessageRole
import com.arcadiapps.localIA.data.model.MessageType
import com.arcadiapps.localIA.data.repository.ChatRepository
import com.arcadiapps.localIA.data.repository.ModelRepository
import com.arcadiapps.localIA.inference.EngineManager
import com.arcadiapps.localIA.inference.EngineState
import com.arcadiapps.localIA.ui.settings.AppSettings
import com.arcadiapps.localIA.ui.settings.SettingsDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val session: ChatSession? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val streamingText: String = "",
    val pendingImage: android.graphics.Bitmap? = null,
    val pendingImagePath: String? = null,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val modelRepository: ModelRepository,
    private val engineManager: EngineManager,
    private val settingsDataSource: SettingsDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val sessions = chatRepository.allSessions.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val engineState = engineManager.state.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), engineManager.state.value
    )

    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            val session = chatRepository.allSessions.first().find { it.id == sessionId }
            _uiState.update { it.copy(session = session) }
            chatRepository.getMessages(sessionId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun newSession(modelId: String) {
        viewModelScope.launch {
            val session = ChatSession(
                id = UUID.randomUUID().toString(),
                modelId = modelId
            )
            chatRepository.createSession(session)
            _uiState.update { it.copy(session = session, messages = emptyList()) }
            loadSession(session.id)
        }
    }

    fun sendMessage(text: String, systemPrompt: String = "") {
        val session = _uiState.value.session ?: return
        val engine = engineManager.getEngine()
        if (engine == null || !engine.isLoaded) {
            _uiState.update { it.copy(error = "Ningún modelo cargado. Ve a Modelos y selecciona uno.") }
            return
        }

        val settings = settingsDataSource.settings.first()
        val effectiveSystemPrompt = systemPrompt.ifBlank { settings.systemPrompt }
        val imagePath = _uiState.value.pendingImagePath

        viewModelScope.launch {
            // Guardar mensaje del usuario
            val userMsg = ChatMessage(
                sessionId = session.id,
                role = MessageRole.USER,
                content = text,
                type = if (imagePath != null) MessageType.IMAGE else MessageType.TEXT,
                mediaPath = imagePath,
                modelId = session.modelId
            )
            chatRepository.addMessage(userMsg)
            _uiState.update { it.copy(isGenerating = true, streamingText = "", error = null,
                pendingImage = null, pendingImagePath = null) }

            // Actualizar título de sesión si es el primer mensaje
            if (_uiState.value.messages.size <= 1) {
                val title = text.take(40).let { if (text.length > 40) "$it…" else it }
                chatRepository.updateSession(session.copy(title = title, updatedAt = System.currentTimeMillis()))
            }

            // Prompt con contexto de imagen si aplica
            val promptWithContext = if (imagePath != null)
                "[Imagen adjunta]\n$text"
            else text

            val sb = StringBuilder()
            try {
                if (settings.streamingEnabled) {
                    engine.generateStream(promptWithContext, effectiveSystemPrompt).collect { token ->
                        sb.append(token)
                        _uiState.update { it.copy(streamingText = sb.toString()) }
                    }
                } else {
                    val result = engine.generate(promptWithContext, effectiveSystemPrompt)
                    sb.append(result)
                }
                val assistantMsg = ChatMessage(
                    sessionId = session.id,
                    role = MessageRole.ASSISTANT,
                    content = sb.toString(),
                    modelId = session.modelId
                )
                chatRepository.addMessage(assistantMsg)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isGenerating = false, streamingText = "") }
            }
        }
    }

    fun setPendingImage(bitmap: android.graphics.Bitmap, path: String) {
        _uiState.update { it.copy(pendingImage = bitmap, pendingImagePath = path) }
    }

    fun clearPendingImage() {
        _uiState.update { it.copy(pendingImage = null, pendingImagePath = null) }
    }

    fun deleteSession(session: ChatSession) {
        viewModelScope.launch {
            chatRepository.deleteSession(session)
            if (_uiState.value.session?.id == session.id) {
                _uiState.update { it.copy(session = null, messages = emptyList()) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}

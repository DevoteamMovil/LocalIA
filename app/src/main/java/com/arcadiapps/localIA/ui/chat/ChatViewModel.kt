package com.arcadiapps.localIA.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcadiapps.localIA.data.model.ChatMessage
import com.arcadiapps.localIA.data.model.ChatSession
import com.arcadiapps.localIA.data.model.MessageRole
import com.arcadiapps.localIA.data.model.MessageType
import com.arcadiapps.localIA.data.repository.ChatRepository
import com.arcadiapps.localIA.data.repository.ModelRepository
import com.arcadiapps.localIA.inference.EngineManager
import com.arcadiapps.localIA.inference.MediaPipeTextEngine
import com.arcadiapps.localIA.ui.settings.SettingsDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    val copiedToClipboard: Boolean = false,
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

    private var generationJob: Job? = null

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

        generationJob = viewModelScope.launch {
            val settings = settingsDataSource.settings.first()
            val effectiveSystemPrompt = systemPrompt.ifBlank { settings.systemPrompt }
            val imagePath = _uiState.value.pendingImagePath

            val userMsg = ChatMessage(
                sessionId = session.id,
                role = MessageRole.USER,
                content = text,
                type = if (imagePath != null) MessageType.IMAGE else MessageType.TEXT,
                mediaPath = imagePath,
                modelId = session.modelId
            )
            chatRepository.addMessage(userMsg)
            _uiState.update {
                it.copy(isGenerating = true, streamingText = "", error = null,
                    pendingImage = null, pendingImagePath = null)
            }

            if (_uiState.value.messages.size <= 1) {
                val title = text.take(40).let { t -> if (text.length > 40) "$t…" else t }
                chatRepository.updateSession(
                    session.copy(title = title, updatedAt = System.currentTimeMillis())
                )
            }

            val promptWithContext = if (imagePath != null) "[Imagen adjunta]\n$text" else text

            // Construir historial completo para que el modelo tenga contexto
            val history = _uiState.value.messages
                .filter { it.role != MessageRole.SYSTEM }
                .dropLast(1) // el último es el mensaje que acabamos de añadir

            val sb = StringBuilder()

            try {
                if (settings.streamingEnabled) {
                    engine.generateStream(promptWithContext, effectiveSystemPrompt, history)
                        .collect { token ->
                            sb.append(token)
                            _uiState.update { it.copy(streamingText = sb.toString()) }
                        }
                } else {
                    sb.append(engine.generate(promptWithContext, effectiveSystemPrompt, history))
                }
                // Solo guardar si no fue cancelado
                if (sb.isNotEmpty()) {
                    chatRepository.addMessage(
                        ChatMessage(
                            sessionId = session.id,
                            role = MessageRole.ASSISTANT,
                            content = sb.toString(),
                            modelId = session.modelId
                        )
                    )
                }
            } catch (e: Exception) {
                // Si hay texto parcial al cancelar, guardarlo igualmente
                if (sb.isNotEmpty()) {
                    chatRepository.addMessage(
                        ChatMessage(
                            sessionId = session.id,
                            role = MessageRole.ASSISTANT,
                            content = sb.toString() + " [interrumpido]",
                            modelId = session.modelId
                        )
                    )
                } else if (e !is kotlinx.coroutines.CancellationException) {
                    _uiState.update { it.copy(error = e.message) }
                }
            } finally {
                _uiState.update { it.copy(isGenerating = false, streamingText = "") }
            }
        }
    }

    /** Detiene la generación en curso */
    fun stopGeneration() {
        // Cancelar el job de coroutine
        generationJob?.cancel()
        generationJob = null

        // Cancelar también a nivel de MediaPipe si está disponible
        val engine = engineManager.getEngine()
        if (engine is MediaPipeTextEngine) {
            engine.cancelGeneration()
        }

        _uiState.update { it.copy(isGenerating = false, streamingText = "") }
    }

    /** Copia toda la conversación al portapapeles */
    fun copyConversation(context: android.content.Context) {
        val messages = _uiState.value.messages
        if (messages.isEmpty()) return

        val text = buildString {
            messages.forEach { msg ->
                val role = when (msg.role) {
                    MessageRole.USER -> "Tú"
                    MessageRole.ASSISTANT -> "IA"
                    MessageRole.SYSTEM -> "Sistema"
                }
                appendLine("[$role]")
                appendLine(msg.content)
                appendLine()
            }
        }.trim()

        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
        val clip = android.content.ClipData.newPlainText("Conversación LocalIA", text)
        clipboard.setPrimaryClip(clip)

        _uiState.update { it.copy(copiedToClipboard = true) }
        // Resetear el estado tras 2 segundos
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _uiState.update { it.copy(copiedToClipboard = false) }
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

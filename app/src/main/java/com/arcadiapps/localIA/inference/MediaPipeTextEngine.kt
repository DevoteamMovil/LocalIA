package com.arcadiapps.localIA.inference

import android.content.Context
import android.util.Log
import com.arcadiapps.localIA.data.model.ChatMessage
import com.arcadiapps.localIA.data.model.MessageRole
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession.LlmInferenceSessionOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class MediaPipeTextEngine(private val context: Context) : InferenceEngine {

    private var llmInference: LlmInference? = null

    override var isLoaded: Boolean = false
        private set

    var temperature: Float = 0.8f
    var topK: Int = 40
    var maxTokens: Int = 512

    companion object {
        private const val TAG = "MediaPipeEngine"
    }

    override suspend fun loadModel(modelPath: String) {
        withContext(Dispatchers.Main) {
            Log.d(TAG, "Cargando modelo: $modelPath")
            unload()
            try {
                val options = LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(maxTokens)
                    .setMaxTopK(topK)
                    .build()
                llmInference = LlmInference.createFromOptions(context, options)
                isLoaded = true
                Log.d(TAG, "Modelo cargado OK")
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando modelo", e)
                isLoaded = false
                throw e
            }
        }
    }

    override fun generateStream(
        prompt: String,
        systemPrompt: String,
        history: List<ChatMessage>
    ): Flow<String> = callbackFlow {
        val inference = llmInference ?: run {
            close(IllegalStateException("Modelo no cargado"))
            awaitClose()
            return@callbackFlow
        }
        Log.d(TAG, "Iniciando streaming, historial: ${history.size} mensajes")
        try {
            val session = createSession(inference)
            val fullPrompt = buildFullPrompt(prompt, systemPrompt, history)
            Log.d(TAG, "Prompt (primeros 200 chars): ${fullPrompt.take(200)}")
            session.addQueryChunk(fullPrompt)
            val future = session.generateResponseAsync { partial, done ->
                if (!partial.isNullOrEmpty()) trySend(partial)
                if (done) close()
            }
            awaitClose {
                try { future.cancel(true) } catch (_: Exception) {}
                try { session.close() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en streaming", e)
            close(e)
            awaitClose()
        }
    }.flowOn(Dispatchers.Main)

    override suspend fun generate(
        prompt: String,
        systemPrompt: String,
        history: List<ChatMessage>
    ): String = withContext(Dispatchers.Main) {
        val inference = llmInference ?: throw IllegalStateException("Modelo no cargado")
        val session = createSession(inference)
        val fullPrompt = buildFullPrompt(prompt, systemPrompt, history)
        session.addQueryChunk(fullPrompt)
        val result = session.generateResponse()
        session.close()
        result
    }

    override fun unload() {
        try { llmInference?.close() } catch (_: Exception) {}
        llmInference = null
        isLoaded = false
    }

    fun cancelGeneration() { /* cancelación via future.cancel en awaitClose */ }

    private fun createSession(inference: LlmInference): LlmInferenceSession {
        val sessionOptions = LlmInferenceSessionOptions.builder()
            .setTopK(topK)
            .setTemperature(temperature)
            .setRandomSeed(101)
            .build()
        return LlmInferenceSession.createFromOptions(inference, sessionOptions)
    }

    /**
     * Construye el prompt completo en formato ChatML (Qwen 2.5):
     *
     *   <|im_start|>system
     *   {system}<|im_end|>
     *   <|im_start|>user
     *   {msg1}<|im_end|>
     *   <|im_start|>assistant
     *   {resp1}<|im_end|>
     *   ...
     *   <|im_start|>user
     *   {prompt}<|im_end|>
     *   <|im_start|>assistant
     *
     * El modelo para de generar cuando produce <|im_end|> o <|im_start|>user
     */
    private fun buildFullPrompt(
        currentMessage: String,
        systemPrompt: String,
        history: List<ChatMessage>
    ): String = buildString {
        // System prompt
        if (systemPrompt.isNotBlank()) {
            append("<|im_start|>system\n")
            append(systemPrompt)
            append("<|im_end|>\n")
        }

        // Historial de turnos anteriores
        history.forEach { msg ->
            when (msg.role) {
                MessageRole.USER -> {
                    append("<|im_start|>user\n")
                    append(msg.content)
                    append("<|im_end|>\n")
                }
                MessageRole.ASSISTANT -> {
                    append("<|im_start|>assistant\n")
                    append(msg.content)
                    append("<|im_end|>\n")
                }
                else -> {}
            }
        }

        // Mensaje actual del usuario
        append("<|im_start|>user\n")
        append(currentMessage)
        append("<|im_end|>\n")

        // Inicio del turno del asistente — el modelo completa desde aquí
        append("<|im_start|>assistant\n")
    }
}

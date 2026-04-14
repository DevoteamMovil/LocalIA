package com.arcadiapps.localIA.inference

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MediaPipeTextEngine(private val context: Context) : InferenceEngine {

    private var llmInference: LlmInference? = null
    override var isLoaded: Boolean = false
        private set

    // Parámetros de generación (se aplican al cargar el modelo)
    var temperature: Float = 0.8f
    var topK: Int = 40
    var maxTokens: Int = 1024

    override suspend fun loadModel(modelPath: String) {
        unload()
        val options = LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(maxTokens)
            .setTopK(topK)
            .setTemperature(temperature)
            .setRandomSeed(101)
            .build()
        llmInference = LlmInference.createFromOptions(context, options)
        isLoaded = true
    }

    override fun generateStream(prompt: String, systemPrompt: String): Flow<String> = callbackFlow {
        val engine = llmInference ?: throw IllegalStateException("Modelo no cargado")
        val fullPrompt = buildPrompt(prompt, systemPrompt)
        engine.generateResponseAsync(fullPrompt)
        // El listener se configura en el builder; aquí usamos generateResponse síncrono en flow
        // Para streaming real necesitamos recrear con listener — ver generateStreamWithListener
        close()
        awaitClose()
    }

    /**
     * Streaming real: recrea la instancia con ResultListener en el builder.
     * Esto es necesario porque MediaPipe requiere el listener en tiempo de construcción.
     */
    fun generateStreamWithListener(
        modelPath: String,
        prompt: String,
        systemPrompt: String
    ): Flow<String> = callbackFlow {
        val fullPrompt = buildPrompt(prompt, systemPrompt)
        val options = LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(maxTokens)
            .setTopK(topK)
            .setTemperature(temperature)
            .setRandomSeed(101)
            .setResultListener { partialResult, done ->
                if (partialResult != null) trySend(partialResult)
                if (done) close()
            }
            .build()
        val streamEngine = LlmInference.createFromOptions(context, options)
        streamEngine.generateResponseAsync(fullPrompt)
        awaitClose { streamEngine.close() }
    }

    override suspend fun generate(prompt: String, systemPrompt: String): String {
        val engine = llmInference ?: throw IllegalStateException("Modelo no cargado")
        val fullPrompt = buildPrompt(prompt, systemPrompt)
        return engine.generateResponse(fullPrompt)
    }

    override fun unload() {
        llmInference?.close()
        llmInference = null
        isLoaded = false
    }

    private fun buildPrompt(userMessage: String, systemPrompt: String): String {
        return if (systemPrompt.isNotBlank()) {
            "<start_of_turn>system\n$systemPrompt<end_of_turn>\n<start_of_turn>user\n$userMessage<end_of_turn>\n<start_of_turn>model\n"
        } else {
            "<start_of_turn>user\n$userMessage<end_of_turn>\n<start_of_turn>model\n"
        }
    }
}

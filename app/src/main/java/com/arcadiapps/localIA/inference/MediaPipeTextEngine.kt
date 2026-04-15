package com.arcadiapps.localIA.inference

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
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
    var maxTokens: Int = 1024

    companion object {
        private const val TAG = "MediaPipeEngine"
    }

    override suspend fun loadModel(modelPath: String) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Cargando modelo desde: $modelPath")
            unload()
            try {
                val options = LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(maxTokens)
                    .setTopK(topK)
                    .setTemperature(temperature)
                    .setRandomSeed(101)
                    .build()
                llmInference = LlmInference.createFromOptions(context, options)
                isLoaded = true
                Log.d(TAG, "Modelo cargado correctamente")
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar modelo", e)
                throw e
            }
        }
    }

    override fun generateStream(prompt: String, systemPrompt: String): Flow<String> =
        generateStreamWithListener(llmInference, buildPrompt(prompt, systemPrompt))

    fun generateStreamWithListener(
        modelPath: String,
        prompt: String,
        systemPrompt: String
    ): Flow<String> = callbackFlow<String> {
        val fullPrompt = buildPrompt(prompt, systemPrompt)
        Log.d(TAG, "Generando con listener, prompt length: ${fullPrompt.length}")
        try {
            val options = LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(maxTokens)
                .setTopK(topK)
                .setTemperature(temperature)
                .setRandomSeed(101)
                .setResultListener { partialResult, done ->
                    if (!partialResult.isNullOrEmpty()) trySend(partialResult)
                    if (done) close()
                }
                .setErrorListener { e ->
                    Log.e(TAG, "Error en generación", e)
                    close(e)
                }
                .build()
            val streamEngine = LlmInference.createFromOptions(context, options)
            streamEngine.generateResponseAsync(fullPrompt)
            awaitClose { streamEngine.close() }
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear engine para streaming", e)
            close(e)
            awaitClose()
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun generate(prompt: String, systemPrompt: String): String =
        withContext(Dispatchers.IO) {
            val engine = llmInference ?: throw IllegalStateException("Modelo no cargado")
            val fullPrompt = buildPrompt(prompt, systemPrompt)
            Log.d(TAG, "Generando respuesta síncrona")
            engine.generateResponse(fullPrompt)
        }

    override fun unload() {
        try {
            llmInference?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error al cerrar engine: ${e.message}")
        }
        llmInference = null
        isLoaded = false
    }

    private fun generateStreamWithListener(
        engine: LlmInference?,
        fullPrompt: String
    ): Flow<String> = callbackFlow {
        if (engine == null) {
            close(IllegalStateException("Modelo no cargado"))
            awaitClose()
            return@callbackFlow
        }
        // LlmInference sin listener solo soporta generateResponse síncrono
        // Emitimos el resultado completo como un solo token
        try {
            val result = engine.generateResponse(fullPrompt)
            trySend(result)
            close()
        } catch (e: Exception) {
            close(e)
        }
        awaitClose()
    }.flowOn(Dispatchers.IO)

    private fun buildPrompt(userMessage: String, systemPrompt: String): String {
        return if (systemPrompt.isNotBlank()) {
            "<|im_start|>system\n$systemPrompt<|im_end|>\n<|im_start|>user\n$userMessage<|im_end|>\n<|im_start|>assistant\n"
        } else {
            "<|im_start|>user\n$userMessage<|im_end|>\n<|im_start|>assistant\n"
        }
    }
}

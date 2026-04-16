package com.arcadiapps.localIA.inference

import android.content.Context
import android.util.Log
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

/**
 * Motor MediaPipe LLM Inference API v0.10.33+
 * Nueva arquitectura: LlmInference (carga modelo) + LlmInferenceSession (generación)
 */
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

    override fun generateStream(prompt: String, systemPrompt: String): Flow<String> = callbackFlow {
        val inference = llmInference ?: run { close(IllegalStateException("Modelo no cargado")); awaitClose(); return@callbackFlow }
        val fullPrompt = buildPrompt(prompt, systemPrompt)
        Log.d(TAG, "Iniciando streaming")
        try {
            val sessionOptions = LlmInferenceSessionOptions.builder()
                .setTopK(topK)
                .setTemperature(temperature)
                .setRandomSeed(101)
                .build()
            val session = LlmInferenceSession.createFromOptions(inference, sessionOptions)
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

    override suspend fun generate(prompt: String, systemPrompt: String): String =
        withContext(Dispatchers.Main) {
            val inference = llmInference ?: throw IllegalStateException("Modelo no cargado")
            val fullPrompt = buildPrompt(prompt, systemPrompt)
            val sessionOptions = LlmInferenceSessionOptions.builder()
                .setTopK(topK)
                .setTemperature(temperature)
                .setRandomSeed(101)
                .build()
            val session = LlmInferenceSession.createFromOptions(inference, sessionOptions)
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

    fun cancelGeneration() {
        // LlmInferenceSession.cancelGenerateResponseAsync() se llama desde el flow
        // El job de coroutine ya se cancela desde ChatViewModel
        // Esta función existe para compatibilidad futura con cancelación nativa
    }

    private fun buildPrompt(user: String, system: String): String =
        if (system.isNotBlank())
            "<|im_start|>system\n$system<|im_end|>\n<|im_start|>user\n$user<|im_end|>\n<|im_start|>assistant\n"
        else
            "<|im_start|>user\n$user<|im_end|>\n<|im_start|>assistant\n"
}

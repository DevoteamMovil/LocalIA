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

    override suspend fun loadModel(modelPath: String) {
        unload()
        val options = LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(1024)
            .setTopK(40)
            .setTemperature(0.8f)
            .setRandomSeed(101)
            .build()
        llmInference = LlmInference.createFromOptions(context, options)
        isLoaded = true
    }

    override fun generateStream(prompt: String, systemPrompt: String): Flow<String> = callbackFlow {
        val engine = llmInference ?: throw IllegalStateException("Modelo no cargado")
        val fullPrompt = buildPrompt(prompt, systemPrompt)
        engine.generateResponseAsync(fullPrompt) { partial, done ->
            trySend(partial)
            if (done) close()
        }
        awaitClose()
    }

    override suspend fun generate(prompt: String, systemPrompt: String): String =
        suspendCancellableCoroutine { cont ->
            val engine = llmInference
            if (engine == null) {
                cont.resumeWithException(IllegalStateException("Modelo no cargado"))
                return@suspendCancellableCoroutine
            }
            val fullPrompt = buildPrompt(prompt, systemPrompt)
            val sb = StringBuilder()
            engine.generateResponseAsync(fullPrompt) { partial, done ->
                sb.append(partial)
                if (done) cont.resume(sb.toString())
            }
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

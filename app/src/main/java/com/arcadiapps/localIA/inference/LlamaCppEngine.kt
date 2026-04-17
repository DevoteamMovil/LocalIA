package com.arcadiapps.localIA.inference

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Motor de inferencia usando llama.cpp via JNI.
 * Compatible con cualquier modelo en formato GGUF (LM Studio, HuggingFace, etc.)
 */
class LlamaCppEngine(
    private val nThreads: Int = 4,
    private val nCtx: Int = 2048
) : InferenceEngine {

    private var nativeHandle: Long = 0L
    override var isLoaded: Boolean = false
        private set

    companion object {
        private const val TAG = "LlamaCppEngine"
        private var libraryLoaded = false

        init {
            try {
                System.loadLibrary("localIA_jni")
                libraryLoaded = true
                Log.d(TAG, "Librería nativa cargada OK")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "No se pudo cargar librería nativa: ${e.message}")
            }
        }
    }

    // ── JNI ──────────────────────────────────────────────────────────────────
    private external fun nativeInit(modelPath: String, nThreads: Int, nCtx: Int): Long
    private external fun nativeGenerate(
        handle: Long, prompt: String, maxTokens: Int, temperature: Float, topK: Int
    ): String
    private external fun nativeFree(handle: Long)

    // ── InferenceEngine ───────────────────────────────────────────────────────
    override suspend fun loadModel(modelPath: String) {
        withContext(Dispatchers.IO) {
            if (!libraryLoaded) throw UnsupportedOperationException(
                "Librería nativa no disponible en esta build"
            )
            nativeFree(nativeHandle)
            nativeHandle = nativeInit(modelPath, nThreads, nCtx)
            isLoaded = nativeHandle != 0L
            if (!isLoaded) throw IllegalStateException("llama.cpp: no se pudo cargar el modelo")
            Log.d(TAG, "Modelo GGUF cargado OK")
        }
    }

    override fun generateStream(
        prompt: String,
        systemPrompt: String,
        history: List<com.arcadiapps.localIA.data.model.ChatMessage>
    ): Flow<String> = flow {
        val full = generate(prompt, systemPrompt, history)
        var i = 0
        while (i < full.length) {
            val end = minOf(i + 6, full.length)
            emit(full.substring(i, end))
            i = end
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun generate(
        prompt: String,
        systemPrompt: String,
        history: List<com.arcadiapps.localIA.data.model.ChatMessage>
    ): String = withContext(Dispatchers.IO) {
        if (!isLoaded) throw IllegalStateException("Modelo no cargado")
        val fullPrompt = buildFullPrompt(prompt, systemPrompt, history)
        nativeGenerate(nativeHandle, fullPrompt, 1024, 0.8f, 40)
    }

    override fun unload() {
        nativeFree(nativeHandle)
        nativeHandle = 0L
        isLoaded = false
    }

    private fun buildFullPrompt(
        user: String,
        system: String,
        history: List<com.arcadiapps.localIA.data.model.ChatMessage>
    ): String = buildString {
        if (system.isNotBlank()) {
            append("<|im_start|>system\n$system<|im_end|>\n")
        }
        history.forEach { msg ->
            when (msg.role) {
                com.arcadiapps.localIA.data.model.MessageRole.USER ->
                    append("<|im_start|>user\n${msg.content}<|im_end|>\n")
                com.arcadiapps.localIA.data.model.MessageRole.ASSISTANT ->
                    append("<|im_start|>assistant\n${msg.content}<|im_end|>\n")
                else -> {}
            }
        }
        append("<|im_start|>user\n$user<|im_end|>\n<|im_start|>assistant\n")
    }
}

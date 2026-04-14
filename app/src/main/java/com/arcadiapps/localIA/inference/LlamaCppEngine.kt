package com.arcadiapps.localIA.inference

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Motor de inferencia usando llama.cpp via JNI.
 * Requiere el submodule de llama.cpp en app/src/main/cpp/llama.cpp
 * y compilar con CMake (ver app/build.gradle.kts).
 *
 * Para activarlo:
 *   git submodule add https://github.com/ggerganov/llama.cpp app/src/main/cpp/llama.cpp
 *   git submodule update --init --recursive
 */
class LlamaCppEngine(
    private val nThreads: Int = 4,
    private val nCtx: Int = 2048
) : InferenceEngine {

    private var nativeHandle: Long = 0L
    override var isLoaded: Boolean = false
        private set

    companion object {
        init {
            try {
                System.loadLibrary("localIA_jni")
            } catch (e: UnsatisfiedLinkError) {
                // La librería nativa no está disponible en esta build
            }
        }
    }

    // ── JNI declarations ────────────────────────────────────────────────────
    private external fun nativeInit(modelPath: String, nThreads: Int, nCtx: Int): Long
    private external fun nativeGenerate(
        handle: Long, prompt: String, maxTokens: Int, temperature: Float, topK: Int
    ): String
    private external fun nativeFree(handle: Long)

    // ── InferenceEngine impl ─────────────────────────────────────────────────
    override suspend fun loadModel(modelPath: String) = withContext(Dispatchers.IO) {
        nativeFree(nativeHandle)
        nativeHandle = nativeInit(modelPath, nThreads, nCtx)
        isLoaded = nativeHandle != 0L
        if (!isLoaded) throw IllegalStateException("llama.cpp: no se pudo cargar el modelo")
    }

    override fun generateStream(prompt: String, systemPrompt: String): Flow<String> = flow {
        // llama.cpp JNI genera de forma síncrona; emitimos el resultado completo
        // Para streaming real se necesita un callback nativo (mejora futura)
        val full = generate(prompt, systemPrompt)
        // Simular streaming por chunks de ~4 chars para la UI
        var i = 0
        while (i < full.length) {
            val end = minOf(i + 4, full.length)
            emit(full.substring(i, end))
            i = end
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun generate(prompt: String, systemPrompt: String): String =
        withContext(Dispatchers.IO) {
            if (!isLoaded) throw IllegalStateException("Modelo no cargado")
            val fullPrompt = if (systemPrompt.isNotBlank())
                "[INST] <<SYS>>\n$systemPrompt\n<</SYS>>\n\n$prompt [/INST]"
            else
                "[INST] $prompt [/INST]"
            nativeGenerate(nativeHandle, fullPrompt, 1024, 0.8f, 40)
        }

    override fun unload() {
        nativeFree(nativeHandle)
        nativeHandle = 0L
        isLoaded = false
    }
}

package com.arcadiapps.localIA.inference

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Motor llama.cpp via JNI.
 * Actualmente usa stubs — para activar la inferencia nativa real:
 *   1. git submodule add https://github.com/ggerganov/llama.cpp app/src/main/cpp/llama.cpp
 *   2. Descomentar externalNativeBuild en app/build.gradle.kts
 *   3. Cambiar nativeAvailable = true
 */
class LlamaCppEngine(
    private val nThreads: Int = 4,
    private val nCtx: Int = 2048
) : InferenceEngine {

    override var isLoaded: Boolean = false
        private set

    private val nativeAvailable: Boolean = false // cambiar a true tras añadir submodule

    override suspend fun loadModel(modelPath: String) = withContext(Dispatchers.IO) {
        if (!nativeAvailable) throw UnsupportedOperationException(
            "llama.cpp nativo no disponible. Usa un modelo MediaPipe."
        )
        isLoaded = true
    }

    override fun generateStream(prompt: String, systemPrompt: String): Flow<String> = flow {
        emit("[llama.cpp no disponible en esta build]")
    }.flowOn(Dispatchers.IO)

    override suspend fun generate(prompt: String, systemPrompt: String): String =
        "[llama.cpp no disponible en esta build]"

    override fun unload() {
        isLoaded = false
    }
}

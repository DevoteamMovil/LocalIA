package com.arcadiapps.localIA.inference

import kotlinx.coroutines.flow.Flow

interface InferenceEngine {
    val isLoaded: Boolean
    suspend fun loadModel(modelPath: String)
    fun generateStream(prompt: String, systemPrompt: String = ""): Flow<String>
    suspend fun generate(prompt: String, systemPrompt: String = ""): String
    fun unload()
}

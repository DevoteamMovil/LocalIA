package com.arcadiapps.localIA.inference

import com.arcadiapps.localIA.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface InferenceEngine {
    val isLoaded: Boolean
    suspend fun loadModel(modelPath: String)
    fun generateStream(
        prompt: String,
        systemPrompt: String = "",
        history: List<ChatMessage> = emptyList()
    ): Flow<String>
    suspend fun generate(
        prompt: String,
        systemPrompt: String = "",
        history: List<ChatMessage> = emptyList()
    ): String
    fun unload()
}

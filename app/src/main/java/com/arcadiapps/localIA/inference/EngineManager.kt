package com.arcadiapps.localIA.inference

import android.content.Context
import com.arcadiapps.localIA.data.model.AIModel
import com.arcadiapps.localIA.data.model.ModelEngine
import com.arcadiapps.localIA.ui.settings.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class EngineState {
    object Idle : EngineState()
    data class Loading(val modelName: String) : EngineState()
    data class Ready(val model: AIModel) : EngineState()
    data class Error(val message: String) : EngineState()
}

@Singleton
class EngineManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var currentEngine: InferenceEngine? = null
    private var currentModel: AIModel? = null

    private val _state = MutableStateFlow<EngineState>(EngineState.Idle)
    val state: StateFlow<EngineState> = _state

    suspend fun loadModel(model: AIModel) {
        if (currentModel?.id == model.id && currentEngine?.isLoaded == true) return

        _state.value = EngineState.Loading(model.name)
        try {
            currentEngine?.unload()
            val modelFile = File(context.filesDir, "models/${model.fileName}")
            if (!modelFile.exists()) throw IllegalStateException("Archivo del modelo no encontrado")

            val engine: InferenceEngine = when (model.engine) {
                ModelEngine.MEDIAPIPE -> MediaPipeTextEngine(context)
                ModelEngine.LLAMA_CPP -> LlamaCppEngine(nThreads = 4, nCtx = model.contextLength)
                ModelEngine.WHISPER   -> MediaPipeTextEngine(context)
            }
            engine.loadModel(modelFile.absolutePath)
            currentEngine = engine
            currentModel = model
            _state.value = EngineState.Ready(model)
        } catch (e: Exception) {
            _state.value = EngineState.Error(e.message ?: "Error desconocido")
        }
    }

    fun getEngine(): InferenceEngine? = currentEngine

    fun unloadCurrent() {
        currentEngine?.unload()
        currentEngine = null
        currentModel = null
        _state.value = EngineState.Idle
    }
}

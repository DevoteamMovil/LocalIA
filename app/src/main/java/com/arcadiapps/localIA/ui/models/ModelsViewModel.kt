package com.arcadiapps.localIA.ui.models

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.arcadiapps.localIA.data.model.AIModel
import com.arcadiapps.localIA.data.model.ModelStatus
import com.arcadiapps.localIA.data.repository.ModelRepository
import com.arcadiapps.localIA.inference.EngineManager
import com.arcadiapps.localIA.worker.ModelDownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ModelsViewModel @Inject constructor(
    private val repository: ModelRepository,
    private val engineManager: EngineManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val models = repository.allModels.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val activeModel = repository.activeModel.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val engineState = engineManager.state.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), engineManager.state.value
    )

    private val _downloadError = MutableStateFlow<String?>(null)
    val downloadError: StateFlow<String?> = _downloadError

    init {
        viewModelScope.launch { repository.initCatalog() }
    }

    fun downloadModel(model: AIModel) {
        _downloadError.value = null
        val request = ModelDownloadWorker.buildRequest(model.id, model.downloadUrl, model.fileName)
        val workManager = WorkManager.getInstance(context)
        workManager.enqueue(request)

        viewModelScope.launch {
            repository.updateDownloadProgress(model.id, 0, ModelStatus.DOWNLOADING)
        }

        // Observar el resultado del worker para detectar fallos
        workManager.getWorkInfosByTagLiveData(model.id).observeForever { workInfos ->
            val info = workInfos?.firstOrNull() ?: return@observeForever
            when (info.state) {
                WorkInfo.State.FAILED -> {
                    val error = info.outputData.getString("error") ?: "Error desconocido en la descarga"
                    Log.e("ModelsViewModel", "Worker falló: $error")
                    _downloadError.value = error
                    viewModelScope.launch {
                        repository.updateDownloadProgress(model.id, 0, ModelStatus.ERROR)
                    }
                }
                WorkInfo.State.SUCCEEDED -> {
                    viewModelScope.launch {
                        repository.updateDownloadProgress(model.id, 100, ModelStatus.READY)
                    }
                }
                WorkInfo.State.BLOCKED -> {
                    _downloadError.value = "Sin conexión a internet"
                }
                else -> {}
            }
        }
    }

    fun cancelDownload(model: AIModel) {
        WorkManager.getInstance(context).cancelAllWorkByTag(model.id)
        viewModelScope.launch {
            repository.updateDownloadProgress(model.id, 0, ModelStatus.NOT_DOWNLOADED)
        }
    }

    fun deleteModel(model: AIModel) {
        viewModelScope.launch { repository.deleteModelFile(model) }
    }

    fun selectModel(model: AIModel) {
        viewModelScope.launch {
            repository.setActiveModel(model.id)
            engineManager.loadModel(model)
        }
    }

    fun clearDownloadError() { _downloadError.value = null }

    fun importLocalModel(
        uri: android.net.Uri,
        displayName: String,
        context: android.content.Context,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val modelsDir = java.io.File(context.filesDir, "models")
                modelsDir.mkdirs()
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("No se pudo abrir el archivo")
                val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "modelo_importado.gguf"
                val destFile = java.io.File(modelsDir, fileName)
                inputStream.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
                // Registrar en la base de datos
                val model = com.arcadiapps.localIA.data.model.AIModel(
                    id = "local_${System.currentTimeMillis()}",
                    name = displayName,
                    description = "Modelo importado localmente",
                    type = com.arcadiapps.localIA.data.model.ModelType.TEXT,
                    engine = com.arcadiapps.localIA.data.model.ModelEngine.LLAMA_CPP,
                    sizeBytes = destFile.length(),
                    downloadUrl = "",
                    fileName = fileName,
                    status = com.arcadiapps.localIA.data.model.ModelStatus.READY,
                    recommendedTier = com.arcadiapps.localIA.data.model.DeviceTier.MEDIUM
                )
                repository.insertModel(model)
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(true, "✓ Modelo importado correctamente")
                }
            } catch (e: Exception) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(false, "Error: ${e.message}")
                }
            }
        }
    }
}

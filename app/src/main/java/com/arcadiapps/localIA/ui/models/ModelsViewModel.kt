package com.arcadiapps.localIA.ui.models

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.arcadiapps.localIA.data.model.AIModel
import com.arcadiapps.localIA.data.model.ModelStatus
import com.arcadiapps.localIA.data.repository.ModelRepository
import com.arcadiapps.localIA.inference.EngineManager
import com.arcadiapps.localIA.worker.ModelDownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    init {
        viewModelScope.launch { repository.initCatalog() }
    }

    fun downloadModel(model: AIModel) {
        val request = ModelDownloadWorker.buildRequest(model.id, model.downloadUrl, model.fileName)
        WorkManager.getInstance(context).enqueue(request)
        viewModelScope.launch {
            repository.updateDownloadProgress(model.id, 0, ModelStatus.DOWNLOADING)
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
}

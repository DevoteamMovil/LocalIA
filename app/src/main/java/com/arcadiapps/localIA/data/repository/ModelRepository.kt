package com.arcadiapps.localIA.data.repository

import android.content.Context
import com.arcadiapps.localIA.data.catalog.ModelCatalog
import com.arcadiapps.localIA.data.dao.AIModelDao
import com.arcadiapps.localIA.data.model.AIModel
import com.arcadiapps.localIA.data.model.ModelStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRepository @Inject constructor(
    private val dao: AIModelDao,
    @ApplicationContext private val context: Context
) {
    val allModels: Flow<List<AIModel>> = dao.getAllModels()
    val activeModel: Flow<AIModel?> = dao.getActiveModel()

    suspend fun initCatalog() {
        val catalog = ModelCatalog.models.map { model ->
            val file = File(context.filesDir, "models/${model.fileName}")
            if (file.exists()) model.copy(status = ModelStatus.READY)
            else model
        }
        dao.insertModels(catalog)
    }

    suspend fun getModelById(id: String): AIModel? = dao.getModelById(id)

    suspend fun insertModel(model: AIModel) = dao.insertModel(model)

    suspend fun setActiveModel(id: String) {
        dao.deactivateAllModels()
        dao.setActiveModel(id)
    }

    suspend fun updateDownloadProgress(id: String, progress: Int, status: ModelStatus) {
        dao.updateDownloadProgress(id, status, progress)
    }

    suspend fun deleteModelFile(model: AIModel) {
        val file = File(context.filesDir, "models/${model.fileName}")
        if (file.exists()) file.delete()
        dao.updateModel(model.copy(status = ModelStatus.NOT_DOWNLOADED, downloadProgress = 0))
    }

    fun getModelFile(model: AIModel): File =
        File(context.filesDir, "models/${model.fileName}")
}

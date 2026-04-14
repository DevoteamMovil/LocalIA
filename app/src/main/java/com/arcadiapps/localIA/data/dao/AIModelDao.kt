package com.arcadiapps.localIA.data.dao

import androidx.room.*
import com.arcadiapps.localIA.data.model.AIModel
import com.arcadiapps.localIA.data.model.ModelStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AIModelDao {
    @Query("SELECT * FROM ai_models ORDER BY name ASC")
    fun getAllModels(): Flow<List<AIModel>>

    @Query("SELECT * FROM ai_models WHERE status = :status")
    fun getModelsByStatus(status: ModelStatus): Flow<List<AIModel>>

    @Query("SELECT * FROM ai_models WHERE id = :id")
    suspend fun getModelById(id: String): AIModel?

    @Query("SELECT * FROM ai_models WHERE isActive = 1 LIMIT 1")
    fun getActiveModel(): Flow<AIModel?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: AIModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(models: List<AIModel>)

    @Update
    suspend fun updateModel(model: AIModel)

    @Query("UPDATE ai_models SET status = :status, downloadProgress = :progress WHERE id = :id")
    suspend fun updateDownloadProgress(id: String, status: ModelStatus, progress: Int)

    @Query("UPDATE ai_models SET isActive = 0")
    suspend fun deactivateAllModels()

    @Query("UPDATE ai_models SET isActive = 1 WHERE id = :id")
    suspend fun setActiveModel(id: String)

    @Delete
    suspend fun deleteModel(model: AIModel)
}

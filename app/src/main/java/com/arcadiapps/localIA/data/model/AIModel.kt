package com.arcadiapps.localIA.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ModelType { TEXT, VISION, AUDIO, MULTIMODAL }
enum class ModelEngine { MEDIAPIPE, LLAMA_CPP, WHISPER }
enum class ModelStatus { NOT_DOWNLOADED, DOWNLOADING, READY, ERROR }
enum class DeviceTier { MEDIUM, HIGH }

@Entity(tableName = "ai_models")
data class AIModel(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val type: ModelType,
    val engine: ModelEngine,
    val sizeBytes: Long,
    val downloadUrl: String,
    val fileName: String,
    val status: ModelStatus = ModelStatus.NOT_DOWNLOADED,
    val downloadProgress: Int = 0,
    val recommendedTier: DeviceTier = DeviceTier.MEDIUM,
    val contextLength: Int = 2048,
    val parameterCount: String = "",
    val quantization: String = "",
    val isActive: Boolean = false
)

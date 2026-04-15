package com.arcadiapps.localIA.data.catalog

import com.arcadiapps.localIA.data.model.AIModel
import com.arcadiapps.localIA.data.model.DeviceTier
import com.arcadiapps.localIA.data.model.ModelEngine
import com.arcadiapps.localIA.data.model.ModelType

/**
 * Catálogo de modelos compatibles con MediaPipe LlmInference.
 * Formato .task (LiteRT) — URLs verificadas y públicas (sin login).
 * Fuente: https://huggingface.co/litert-community
 */
object ModelCatalog {

    val models = listOf(
        // ── TEXTO – Gama media (recomendado para empezar) ───────────────────
        AIModel(
            id = "qwen25-1b-q8-seq128",
            name = "Qwen 2.5 1.5B (ligero)",
            description = "Modelo de Alibaba. Rápido, ligero y en español. Ideal para gama media.",
            type = ModelType.TEXT,
            engine = ModelEngine.MEDIAPIPE,
            sizeBytes = 1_570_000_000L,
            downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_seq128_q8_ekv1280.task",
            fileName = "qwen25-1b-q8-seq128.task",
            recommendedTier = DeviceTier.MEDIUM,
            contextLength = 1280,
            parameterCount = "1.5B",
            quantization = "INT8"
        ),
        AIModel(
            id = "qwen25-1b-q8-multi",
            name = "Qwen 2.5 1.5B (rápido)",
            description = "Variante multi-prefill de Qwen 2.5. Más rápido en la primera respuesta.",
            type = ModelType.TEXT,
            engine = ModelEngine.MEDIAPIPE,
            sizeBytes = 1_600_000_000L,
            downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            fileName = "qwen25-1b-q8-multi.task",
            recommendedTier = DeviceTier.MEDIUM,
            contextLength = 1280,
            parameterCount = "1.5B",
            quantization = "INT8"
        ),
        // ── TEXTO – Gama alta ───────────────────────────────────────────────
        AIModel(
            id = "qwen25-1b-q8-ekv4096",
            name = "Qwen 2.5 1.5B (contexto largo)",
            description = "Qwen 2.5 con contexto de 4096 tokens. Para conversaciones largas.",
            type = ModelType.TEXT,
            engine = ModelEngine.MEDIAPIPE,
            sizeBytes = 1_680_000_000L,
            downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.task",
            fileName = "qwen25-1b-q8-ekv4096.task",
            recommendedTier = DeviceTier.HIGH,
            contextLength = 4096,
            parameterCount = "1.5B",
            quantization = "INT8"
        )
    )
}

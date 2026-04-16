package com.arcadiapps.localIA.data.catalog

import com.arcadiapps.localIA.data.model.AIModel
import com.arcadiapps.localIA.data.model.DeviceTier
import com.arcadiapps.localIA.data.model.ModelEngine
import com.arcadiapps.localIA.data.model.ModelType

object ModelCatalog {

    val models = listOf(

        // ════════════════════════════════════════════════════════════════════
        // MEDIAPIPE (.task) — litert-community HuggingFace, sin login
        // ════════════════════════════════════════════════════════════════════
        AIModel(
            id = "qwen25-1b-q8-seq128",
            name = "Qwen 2.5 1.5B (MediaPipe)",
            description = "Motor MediaPipe. Rápido, ligero, bueno en español. Recomendado para empezar.",
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
            name = "Qwen 2.5 1.5B Multi-prefill (MediaPipe)",
            description = "Motor MediaPipe. Variante más rápida en la primera respuesta.",
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
        AIModel(
            id = "qwen25-1b-q8-ekv4096",
            name = "Qwen 2.5 1.5B Contexto largo (MediaPipe)",
            description = "Motor MediaPipe. Contexto de 4096 tokens para conversaciones largas.",
            type = ModelType.TEXT,
            engine = ModelEngine.MEDIAPIPE,
            sizeBytes = 1_680_000_000L,
            downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.task",
            fileName = "qwen25-1b-q8-ekv4096.task",
            recommendedTier = DeviceTier.HIGH,
            contextLength = 4096,
            parameterCount = "1.5B",
            quantization = "INT8"
        ),

        // ════════════════════════════════════════════════════════════════════
        // LLAMA.CPP (.gguf) — compatible con LM Studio, sin login
        // ════════════════════════════════════════════════════════════════════
        AIModel(
            id = "qwen25-1b-gguf-q4",
            name = "Qwen 2.5 1.5B Q4 (llama.cpp)",
            description = "Motor llama.cpp. Formato GGUF compatible con LM Studio. Gama media.",
            type = ModelType.TEXT,
            engine = ModelEngine.LLAMA_CPP,
            sizeBytes = 986_000_000L,
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
            fileName = "qwen2.5-1.5b-q4_k_m.gguf",
            recommendedTier = DeviceTier.MEDIUM,
            contextLength = 2048,
            parameterCount = "1.5B",
            quantization = "Q4_K_M"
        ),
        AIModel(
            id = "qwen25-3b-gguf-q4",
            name = "Qwen 2.5 3B Q4 (llama.cpp)",
            description = "Motor llama.cpp. Más capaz que el 1.5B. Recomendado para gama alta.",
            type = ModelType.TEXT,
            engine = ModelEngine.LLAMA_CPP,
            sizeBytes = 1_900_000_000L,
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf",
            fileName = "qwen2.5-3b-q4_k_m.gguf",
            recommendedTier = DeviceTier.HIGH,
            contextLength = 4096,
            parameterCount = "3B",
            quantization = "Q4_K_M"
        ),
        AIModel(
            id = "phi35-mini-gguf-q4",
            name = "Phi-3.5 Mini Q4 (llama.cpp)",
            description = "Motor llama.cpp. Modelo de Microsoft, excelente en razonamiento.",
            type = ModelType.TEXT,
            engine = ModelEngine.LLAMA_CPP,
            sizeBytes = 2_200_000_000L,
            downloadUrl = "https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF/resolve/main/Phi-3.5-mini-instruct-Q4_K_M.gguf",
            fileName = "phi-3.5-mini-q4_k_m.gguf",
            recommendedTier = DeviceTier.HIGH,
            contextLength = 4096,
            parameterCount = "3.8B",
            quantization = "Q4_K_M"
        ),
        AIModel(
            id = "gemma3-1b-gguf-q4",
            name = "Gemma 3 1B Q4 (llama.cpp)",
            description = "Motor llama.cpp. Gemma 3 de Google, muy ligero y eficiente.",
            type = ModelType.TEXT,
            engine = ModelEngine.LLAMA_CPP,
            sizeBytes = 770_000_000L,
            downloadUrl = "https://huggingface.co/ggml-org/gemma-3-1b-it-GGUF/resolve/main/gemma-3-1b-it-Q4_K_M.gguf",
            fileName = "gemma-3-1b-q4_k_m.gguf",
            recommendedTier = DeviceTier.MEDIUM,
            contextLength = 2048,
            parameterCount = "1B",
            quantization = "Q4_K_M"
        )
    )
}

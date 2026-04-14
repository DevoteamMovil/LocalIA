package com.arcadiapps.localIA.data.catalog

import com.arcadiapps.localIA.data.model.AIModel
import com.arcadiapps.localIA.data.model.DeviceTier
import com.arcadiapps.localIA.data.model.ModelEngine
import com.arcadiapps.localIA.data.model.ModelType

object ModelCatalog {

    val models = listOf(
        // ── TEXTO – Gama media ──────────────────────────────────────────────
        AIModel(
            id = "gemma2-2b-q4",
            name = "Gemma 2 2B",
            description = "Modelo de texto de Google. Rápido y eficiente para gama media.",
            type = ModelType.TEXT,
            engine = ModelEngine.MEDIAPIPE,
            sizeBytes = 1_500_000_000L,
            downloadUrl = "https://storage.googleapis.com/mediapipe-models/llm_inference/gemma-2-2b-it-gpu-int4/float32/1/gemma-2-2b-it-gpu-int4.bin",
            fileName = "gemma-2-2b-it-gpu-int4.bin",
            recommendedTier = DeviceTier.MEDIUM,
            contextLength = 2048,
            parameterCount = "2B",
            quantization = "INT4"
        ),
        AIModel(
            id = "phi3-mini-q4",
            name = "Phi-3 Mini",
            description = "Modelo de Microsoft. Excelente relación calidad/tamaño.",
            type = ModelType.TEXT,
            engine = ModelEngine.MEDIAPIPE,
            sizeBytes = 2_200_000_000L,
            downloadUrl = "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf",
            fileName = "phi3-mini-q4.gguf",
            recommendedTier = DeviceTier.MEDIUM,
            contextLength = 4096,
            parameterCount = "3.8B",
            quantization = "Q4_K_M"
        ),
        // ── TEXTO – Gama alta ───────────────────────────────────────────────
        AIModel(
            id = "gemma2-9b-q4",
            name = "Gemma 2 9B",
            description = "Versión más potente de Gemma 2. Requiere gama alta.",
            type = ModelType.TEXT,
            engine = ModelEngine.MEDIAPIPE,
            sizeBytes = 5_500_000_000L,
            downloadUrl = "https://huggingface.co/google/gemma-2-9b-it-GGUF/resolve/main/gemma-2-9b-it-Q4_K_M.gguf",
            fileName = "gemma-2-9b-it-q4.gguf",
            recommendedTier = DeviceTier.HIGH,
            contextLength = 8192,
            parameterCount = "9B",
            quantization = "Q4_K_M"
        ),
        AIModel(
            id = "llama32-3b-q5",
            name = "Llama 3.2 3B",
            description = "Meta Llama 3.2 instrucciones. Muy capaz para su tamaño.",
            type = ModelType.TEXT,
            engine = ModelEngine.MEDIAPIPE,
            sizeBytes = 2_800_000_000L,
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q5_K_M.gguf",
            fileName = "llama32-3b-q5.gguf",
            recommendedTier = DeviceTier.HIGH,
            contextLength = 8192,
            parameterCount = "3B",
            quantization = "Q5_K_M"
        ),
        // ── AUDIO / TRANSCRIPCIÓN ───────────────────────────────────────────
        AIModel(
            id = "whisper-tiny",
            name = "Whisper Tiny",
            description = "Transcripción de voz a texto. Ligero y rápido.",
            type = ModelType.AUDIO,
            engine = ModelEngine.MEDIAPIPE,
            sizeBytes = 150_000_000L,
            downloadUrl = "https://storage.googleapis.com/mediapipe-models/audio_classifier/yamnet/float32/1/yamnet.tflite",
            fileName = "whisper-tiny.tflite",
            recommendedTier = DeviceTier.MEDIUM,
            parameterCount = "39M",
            quantization = "FP32"
        ),
        // ── VISIÓN ──────────────────────────────────────────────────────────
        AIModel(
            id = "gemma3-vision-2b",
            name = "Gemma 3 Vision 2B",
            description = "Análisis de imágenes con lenguaje natural.",
            type = ModelType.VISION,
            engine = ModelEngine.MEDIAPIPE,
            sizeBytes = 2_000_000_000L,
            downloadUrl = "https://storage.googleapis.com/mediapipe-models/image_classifier/efficientnet_lite0/float32/1/efficientnet_lite0.tflite",
            fileName = "gemma3-vision-2b.tflite",
            recommendedTier = DeviceTier.HIGH,
            parameterCount = "2B",
            quantization = "INT4"
        )
    )
}

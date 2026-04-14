#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "LocalIA_JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Incluir llama.cpp solo si está disponible como submodule
#if __has_include("llama.h")
#include "llama.h"
#include "common/common.h"
#define LLAMA_AVAILABLE 1
#else
#define LLAMA_AVAILABLE 0
#endif

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_arcadiapps_localIA_inference_LlamaCppEngine_nativeInit(
        JNIEnv *env, jobject /* this */, jstring modelPath, jint nThreads, jint nCtx) {
#if LLAMA_AVAILABLE
    const char *path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Cargando modelo: %s", path);

    llama_model_params mparams = llama_model_default_params();
    mparams.n_gpu_layers = 0; // CPU-only por defecto en Android

    llama_model *model = llama_load_model_from_file(path, mparams);
    env->ReleaseStringUTFChars(modelPath, path);

    if (!model) {
        LOGE("Error al cargar el modelo");
        return 0L;
    }

    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx = nCtx;
    cparams.n_threads = nThreads;
    cparams.n_threads_batch = nThreads;

    llama_context *ctx = llama_new_context_with_model(model, cparams);
    if (!ctx) {
        llama_free_model(model);
        LOGE("Error al crear contexto");
        return 0L;
    }

    // Empaquetar model + ctx en un struct heap-allocated
    struct LlamaHandle { llama_model *model; llama_context *ctx; };
    auto *handle = new LlamaHandle{model, ctx};
    return reinterpret_cast<jlong>(handle);
#else
    LOGE("llama.cpp no disponible en esta build");
    return 0L;
#endif
}

JNIEXPORT jstring JNICALL
Java_com_arcadiapps_localIA_inference_LlamaCppEngine_nativeGenerate(
        JNIEnv *env, jobject /* this */, jlong handle, jstring prompt,
        jint maxTokens, jfloat temperature, jint topK) {
#if LLAMA_AVAILABLE
    if (handle == 0L) return env->NewStringUTF("[Error: modelo no cargado]");

    struct LlamaHandle { llama_model *model; llama_context *ctx; };
    auto *h = reinterpret_cast<LlamaHandle *>(handle);

    const char *promptStr = env->GetStringUTFChars(prompt, nullptr);
    std::string result;

    // Tokenizar
    std::vector<llama_token> tokens(llama_n_ctx(h->ctx));
    int nTokens = llama_tokenize(h->model, promptStr, strlen(promptStr),
                                  tokens.data(), tokens.size(), true, true);
    env->ReleaseStringUTFChars(prompt, promptStr);

    if (nTokens < 0) return env->NewStringUTF("[Error: tokenización fallida]");
    tokens.resize(nTokens);

    // Batch
    llama_batch batch = llama_batch_get_one(tokens.data(), nTokens, 0, 0);
    if (llama_decode(h->ctx, batch) != 0) {
        return env->NewStringUTF("[Error: decode fallido]");
    }

    // Sampler
    auto sparams = llama_sampler_chain_default_params();
    llama_sampler *sampler = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(sampler, llama_sampler_init_top_k(topK));
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(temperature));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(42));

    int nPos = nTokens;
    for (int i = 0; i < maxTokens; i++) {
        llama_token token = llama_sampler_sample(sampler, h->ctx, -1);
        if (llama_token_is_eog(h->model, token)) break;

        char buf[256];
        int n = llama_token_to_piece(h->model, token, buf, sizeof(buf), 0, true);
        if (n > 0) result.append(buf, n);

        llama_batch next = llama_batch_get_one(&token, 1, nPos++, 0);
        if (llama_decode(h->ctx, next) != 0) break;
    }

    llama_sampler_free(sampler);
    return env->NewStringUTF(result.c_str());
#else
    return env->NewStringUTF("[llama.cpp no disponible]");
#endif
}

JNIEXPORT void JNICALL
Java_com_arcadiapps_localIA_inference_LlamaCppEngine_nativeFree(
        JNIEnv * /* env */, jobject /* this */, jlong handle) {
#if LLAMA_AVAILABLE
    if (handle == 0L) return;
    struct LlamaHandle { llama_model *model; llama_context *ctx; };
    auto *h = reinterpret_cast<LlamaHandle *>(handle);
    llama_free(h->ctx);
    llama_free_model(h->model);
    delete h;
    LOGI("Modelo liberado");
#endif
}

} // extern "C"

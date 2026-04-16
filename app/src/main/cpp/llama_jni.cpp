#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>

#define LOG_TAG "LlamaCpp_JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#include "llama.h"

struct LlamaHandle {
    llama_model   *model = nullptr;
    llama_context *ctx   = nullptr;
};

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_arcadiapps_localIA_inference_LlamaCppEngine_nativeInit(
        JNIEnv *env, jobject, jstring jModelPath, jint nThreads, jint nCtx) {

    const char *path = env->GetStringUTFChars(jModelPath, nullptr);
    LOGI("Cargando modelo: %s", path);

    llama_model_params mparams = llama_model_default_params();
    mparams.n_gpu_layers = 0;

    llama_model *model = llama_model_load_from_file(path, mparams);
    env->ReleaseStringUTFChars(jModelPath, path);

    if (!model) { LOGE("Error al cargar modelo"); return 0L; }

    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx           = (uint32_t)nCtx;
    cparams.n_threads       = (uint32_t)nThreads;
    cparams.n_threads_batch = (uint32_t)nThreads;

    llama_context *ctx = llama_init_from_model(model, cparams);
    if (!ctx) { llama_model_free(model); LOGE("Error al crear contexto"); return 0L; }

    auto *handle = new LlamaHandle{model, ctx};
    LOGI("Modelo cargado OK");
    return reinterpret_cast<jlong>(handle);
}

JNIEXPORT jstring JNICALL
Java_com_arcadiapps_localIA_inference_LlamaCppEngine_nativeGenerate(
        JNIEnv *env, jobject, jlong jHandle,
        jstring jPrompt, jint maxTokens, jfloat temperature, jint topK) {

    if (!jHandle) return env->NewStringUTF("[Error: modelo no cargado]");
    auto *h = reinterpret_cast<LlamaHandle *>(jHandle);

    const char *promptStr = env->GetStringUTFChars(jPrompt, nullptr);
    const llama_vocab *vocab = llama_model_get_vocab(h->model);

    // Tokenizar — primera pasada para obtener el tamaño
    int nTokens = -llama_tokenize(vocab, promptStr, (int32_t)strlen(promptStr),
                                   nullptr, 0, true, true);
    std::vector<llama_token> tokens(nTokens);
    llama_tokenize(vocab, promptStr, (int32_t)strlen(promptStr),
                   tokens.data(), nTokens, true, true);
    env->ReleaseStringUTFChars(jPrompt, promptStr);

    // Decode prompt
    llama_batch batch = llama_batch_get_one(tokens.data(), (int)tokens.size());
    if (llama_decode(h->ctx, batch) != 0) {
        return env->NewStringUTF("[Error: decode fallido]");
    }

    // Sampler
    auto sparams = llama_sampler_chain_default_params();
    llama_sampler *sampler = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(sampler, llama_sampler_init_top_k(topK));
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(temperature));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(42));

    std::string result;
    int nPos = (int)tokens.size();

    for (int i = 0; i < maxTokens; i++) {
        llama_token token = llama_sampler_sample(sampler, h->ctx, -1);
        if (llama_vocab_is_eog(vocab, token)) break;

        char buf[256] = {};
        int n = llama_token_to_piece(vocab, token, buf, sizeof(buf) - 1, 0, true);
        if (n > 0) result.append(buf, n);

        llama_batch next = llama_batch_get_one(&token, 1);
        if (llama_decode(h->ctx, next) != 0) break;
        nPos++;
    }

    llama_sampler_free(sampler);
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_com_arcadiapps_localIA_inference_LlamaCppEngine_nativeFree(
        JNIEnv *, jobject, jlong jHandle) {
    if (!jHandle) return;
    auto *h = reinterpret_cast<LlamaHandle *>(jHandle);
    llama_free(h->ctx);
    llama_model_free(h->model);
    delete h;
    LOGI("Modelo liberado");
}

} // extern "C"

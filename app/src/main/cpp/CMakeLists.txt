cmake_minimum_required(VERSION 3.22.1)
project("localIA")

# ── llama.cpp ────────────────────────────────────────────────────────────────
set(LLAMA_DIR ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)

set(LLAMA_BUILD_TESTS    OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER   OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS    OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP          OFF CACHE BOOL "" FORCE)

add_subdirectory(${LLAMA_DIR} llama_build)

# ── JNI bridge ───────────────────────────────────────────────────────────────
add_library(localIA_jni SHARED llama_jni.cpp)

target_include_directories(localIA_jni PRIVATE
    ${LLAMA_DIR}/include
    ${LLAMA_DIR}/ggml/include
    ${LLAMA_DIR}/common
)

target_link_libraries(localIA_jni
    llama
    ggml
    android
    log
)

target_compile_options(localIA_jni PRIVATE -O3 -ffast-math)

cmake_minimum_required(VERSION 3.22.1)
project("localIA")

# ── llama.cpp como submodule ────────────────────────────────────────────────
# Clona con: git submodule add https://github.com/ggerganov/llama.cpp app/src/main/cpp/llama.cpp
set(LLAMA_DIR ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)

if(EXISTS ${LLAMA_DIR}/CMakeLists.txt)
    set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
    add_subdirectory(${LLAMA_DIR} llama_build)
endif()

# ── JNI bridge ──────────────────────────────────────────────────────────────
add_library(localIA_jni SHARED llama_jni.cpp)

target_include_directories(localIA_jni PRIVATE
    ${LLAMA_DIR}
    ${LLAMA_DIR}/common
)

target_link_libraries(localIA_jni
    android
    log
    $<$<BOOL:${EXISTS ${LLAMA_DIR}/CMakeLists.txt}>:llama>
    $<$<BOOL:${EXISTS ${LLAMA_DIR}/CMakeLists.txt}>:common>
)

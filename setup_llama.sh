#!/bin/bash
# Inicializa llama.cpp como submodule para la build nativa JNI
# Ejecutar una sola vez desde la raíz del proyecto

set -e

echo "→ Añadiendo llama.cpp como submodule..."
git init 2>/dev/null || true
git submodule add https://github.com/ggerganov/llama.cpp app/src/main/cpp/llama.cpp
git submodule update --init --recursive

echo "✓ llama.cpp listo en app/src/main/cpp/llama.cpp"
echo "  Ahora sincroniza Gradle en Android Studio."

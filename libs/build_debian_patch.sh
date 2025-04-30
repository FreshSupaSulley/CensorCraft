#!/bin/bash
set -xe

# Check for required input
if [[ -z "$1" ]]; then
  echo "Usage: $0 <arch>"
  echo "Supported: amd64, arm64"
  exit 1
fi

AARCH="$1"

build_lib() {
  TMP_DIR=src/main/resources/debian
  TARGET_DIR=src/main/resources/debian-$AARCH
  mkdir -p "$TMP_DIR" "$TARGET_DIR"

  cmake -B build $CMAKE_ARGS \
        -DCMAKE_C_FLAGS="$CMAKE_CFLAGS" \
        -DCMAKE_INSTALL_PREFIX=$TMP_DIR

  cmake --build build --config Release
  cmake --install build

  cp "$TMP_DIR/libggml.so" "$TARGET_DIR/libggml$LIB_VARIANT.so"

  if [ "$ADD_WRAPPER" = true ]; then
    cp "$TMP_DIR/libwhisper.so.1" "$TARGET_DIR/libwhisper.so.1"
    cp "$TMP_DIR/libwhisper-jni.so" "$TARGET_DIR/libwhisper-jni.so"
  fi

  rm -rf "$TMP_DIR" build
}

case $AARCH in
  amd64)
    LIB_VARIANT="+mf16c+mfma+mavx+mavx2"
    CMAKE_ARGS="-DGGML_AVX=ON -DGGML_AVX2=ON -DGGML_FMA=ON -DGGML_F16C=ON"
    build_lib

    ADD_WRAPPER=true
    LIB_VARIANT=""  # or use a neutral suffix like "+compat"
    CMAKE_ARGS="-DGGML_AVX=OFF -DGGML_AVX2=OFF -DGGML_FMA=OFF -DGGML_F16C=OFF"
    build_lib
    ;;

  arm64)
    LIB_VARIANT="+fp16"
    CMAKE_CFLAGS="-march=armv8.2-a+fp16"
    CMAKE_ARGS=""
    build_lib

    ADD_WRAPPER=true
    LIB_VARIANT="+crc"
    CMAKE_CFLAGS="-march=armv8.1-a+crc"
    CMAKE_ARGS=""
    build_lib
    ;;

  *)
    echo "Unsupported architecture: $AARCH"
    exit 1
    ;;
esac

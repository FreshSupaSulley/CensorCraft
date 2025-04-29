#!/bin/bash
set -xe

# Force architecture assignment if not set
if [ -z "$AARCH" ]; then
    AARCH=$(dpkg --print-architecture)
fi

# Function to build the library
build_lib() {
  TMP_DIR=src/main/resources/debian
  TARGET_DIR=src/main/resources/debian-$AARCH

  if [ "$AARCH" = "arm64" ]; then
    export CC=aarch64-linux-gnu-gcc
    export CXX=aarch64-linux-gnu-g++
    cmake -B build -DCMAKE_SYSTEM_NAME=Linux -DCMAKE_SYSTEM_PROCESSOR=aarch64 \
          -DCMAKE_C_COMPILER=$CC -DCMAKE_CXX_COMPILER=$CXX \
          -DCMAKE_C_FLAGS="$CMAKE_CFLAGS" -DCMAKE_INSTALL_PREFIX=$TMP_DIR $CMAKE_ARGS
  else
    cmake -B build $CMAKE_ARGS -DCMAKE_C_FLAGS="$CMAKE_CFLAGS" -DCMAKE_INSTALL_PREFIX=$TMP_DIR
  fi

  cmake --build build --config Release
  cmake --install build

  cp $TMP_DIR/libggml.so $TARGET_DIR/libggml$LIB_VARIANT.so
  if [ "$ADD_WRAPPER" = true ]; then
    cp $TMP_DIR/libwhisper.so.1 $TARGET_DIR/libwhisper.so.1
    cp $TMP_DIR/libwhisper-jni.so $TARGET_DIR/libwhisper-jni.so
  fi
  rm -rf $TMP_DIR build
}

# Detect AARCH (ensure it's not wrong)
echo "Detected architecture: $AARCH"
case $AARCH in
  amd64)
    # Only apply AVX and F16C for amd64 architecture
    LIB_VARIANT="+mf16c+mfma+mavx+mavx2" CMAKE_ARGS="-DGGML_AVX=ON -DGGML_AVX2=ON -DGGML_FMA=ON -DGGML_F16C=ON" CMAKE_CFLAGS="" build_lib
    ADD_WRAPPER=true CMAKE_ARGS="-DGGML_AVX=OFF -DGGML_AVX2=OFF -DGGML_FMA=OFF -DGGML_F16C=OFF" CMAKE_CFLAGS="" build_lib
    ;;
  arm64)
    # ARM64 architecture: Ensure no x86-specific flags are used
    LIB_VARIANT="+fp16" CMAKE_ARGS="" CMAKE_CFLAGS="-march=armv8-a" build_lib  # More generic flag for ARM64
    ADD_WRAPPER=true LIB_VARIANT="+crc" CMAKE_CFLAGS="-march=armv8.1-a+crc" build_lib
    ;;
  armhf|armv7l)
    AARCH=armv7l
    # Ensure that we use the correct ARMv7 cross-compiler
    export CC=arm-linux-gnueabihf-gcc
    export CXX=arm-linux-gnueabihf-g++
    
    # ARMv7 architecture: Remove x86-specific flags and use ARMv7-specific ones
    LIB_VARIANT="+neon" CMAKE_CFLAGS="-march=armv7-a -mfpu=neon -mno-unaligned-access" build_lib
    ADD_WRAPPER=true CMAKE_CFLAGS="-mfpu=neon -mno-unaligned-access" build_lib
    ;;
  *)
    echo "Unknown architecture: $AARCH"
    exit 1
    ;;
esac

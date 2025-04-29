#!/bin/bash
set -xe

build_lib() {
  TMP_DIR=src/main/resources/debian
  TARGET_DIR=src/main/resources/debian-$AARCH
  
  # Apply architecture-specific flags
  if [ "$AARCH" == "amd64" ]; then
    # Intel-specific flags for AMD64
    LIB_VARIANT="+mf16c+mfma+mavx+mavx2"
    CMAKE_ARGS="-DGGML_AVX=ON -DGGML_AVX2=ON -DGGML_FMA=ON -DGGML_F16C=ON"
  elif [ "$AARCH" == "arm64" ]; then
    # ARM64 specific flags
    LIB_VARIANT="+fp16"
    CMAKE_CFLAGS="-march=armv8.2-a+fp16"
    CMAKE_ARGS=""
  elif [ "$AARCH" == "armv7l" ]; then
    # ARMv7L specific flags
    LIB_VARIANT="+crc"
    CMAKE_CFLAGS="-march=armv8-a+crc -mfpu=neon-fp-armv8 -mfp16-format=ieee -mno-unaligned-access"
    CMAKE_ARGS=""
  fi
  
  # Apply additional wrapper for ARM targets
  if [ "$ADD_WRAPPER" = true ]; then
    if [ "$AARCH" == "arm64" ]; then
      CMAKE_CFLAGS="-march=armv8-a+crc"  # Adjust ARM64 for crc variant
    elif [ "$AARCH" == "armv7l" ]; then
      CMAKE_CFLAGS="-mfpu=neon -mfp16-format=ieee -mno-unaligned-access"  # Adjust ARMv7 for crc variant
    fi
  fi
  
  # Run the build steps
  cmake -B build $CMAKE_ARGS -DCMAKE_C_FLAGS="$CMAKE_CFLAGS" -DCMAKE_INSTALL_PREFIX=$TMP_DIR
  cmake --build build --config Release
  cmake --install build
  
  # Copy the shared library to the target directory
  cp $TMP_DIR/libggml.so $TARGET_DIR/libggml$LIB_VARIANT.so
  
  if [ "$ADD_WRAPPER" = true ]; then
    cp $TMP_DIR/libwhisper.so.1 $TARGET_DIR/libwhisper.so.1
    cp $TMP_DIR/libwhisper-jni.so $TARGET_DIR/libwhisper-jni.so
  fi  

  # Clean up temporary files
  rm -rf $TMP_DIR
  rm -rf build
}

# Detect architecture
AARCH=$(dpkg --print-architecture)
case $AARCH in
  amd64)
    # Build for AMD64 architecture
    ADD_WRAPPER=true
    build_lib
    ADD_WRAPPER=false
    build_lib
    ;;
  arm64)
    # Build for ARM64 architecture
    ADD_WRAPPER=true
    build_lib
    ADD_WRAPPER=false
    build_lib
    ;;
  armhf|armv7l)
    # Build for ARMv7L architecture
    AARCH=armv7l
    ADD_WRAPPER=true
    build_lib
    ADD_WRAPPER=false
    build_lib
    ;;
esac

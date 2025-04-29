build_lib() {
  TMP_DIR=src/main/resources/debian
  TARGET_DIR=src/main/resources/debian-$AARCH

  # Select cross-compilation toolchain
  case $AARCH in
    arm64)
      export CC=aarch64-linux-gnu-gcc
      export CXX=aarch64-linux-gnu-g++
      CMAKE_TOOLCHAIN_ARGS="-DCMAKE_SYSTEM_NAME=Linux -DCMAKE_SYSTEM_PROCESSOR=aarch64"
      ;;
    armv7l)
      export CC=arm-linux-gnueabihf-gcc
      export CXX=arm-linux-gnueabihf-g++
      CMAKE_TOOLCHAIN_ARGS="-DCMAKE_SYSTEM_NAME=Linux -DCMAKE_SYSTEM_PROCESSOR=armv7l"
      ;;
    amd64)
      CMAKE_TOOLCHAIN_ARGS=""
      ;;
    *)
      echo "Unknown architecture: $AARCH"
      exit 1
      ;;
  esac

  cmake -B build $CMAKE_ARGS $CMAKE_TOOLCHAIN_ARGS -DCMAKE_C_FLAGS="$CMAKE_CFLAGS" -DCMAKE_INSTALL_PREFIX=$TMP_DIR
  cmake --build build --config Release
  cmake --install build

  cp $TMP_DIR/libggml.so $TARGET_DIR/libggml$LIB_VARIANT.so
  if [ "$ADD_WRAPPER" = true ]; then
    cp $TMP_DIR/libwhisper.so.1 $TARGET_DIR/libwhisper.so.1
    cp $TMP_DIR/libwhisper-jni.so $TARGET_DIR/libwhisper-jni.so
  fi
  rm -rf $TMP_DIR build
}

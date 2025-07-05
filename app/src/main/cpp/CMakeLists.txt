cmake_minimum_required(VERSION 3.22.1)

project("datarescue_native")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add include directories
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/filesystem
    ${CMAKE_CURRENT_SOURCE_DIR}/recovery
)

# Source files
set(NATIVE_SOURCES
    native_scanner.cpp
    filesystem/ext4_scanner.cpp
    filesystem/f2fs_scanner.cpp
    filesystem/fat32_scanner.cpp
    recovery/file_carver.cpp
    recovery/signature_detector.cpp
    utils/root_utils.cpp
    utils/disk_utils.cpp
    jni_bridge.cpp
)

# Create the native library
add_library(
    datarescue_native
    SHARED
    ${NATIVE_SOURCES}
)

# Link libraries
target_link_libraries(
    datarescue_native
    android
    log
)

# Compiler flags
target_compile_options(datarescue_native PRIVATE
    -Wall
    -Wextra
    -O3
    -ffast-math
    -funroll-loops
)
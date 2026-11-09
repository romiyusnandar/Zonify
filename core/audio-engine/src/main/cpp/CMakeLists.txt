cmake_minimum_required(VERSION 3.22.1)

project("zonify_audio_engine" LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(
    zonify_audio_engine
    SHARED
    BiquadFilter.cpp
    ParametricEqualizer.cpp
    AudioEngine.cpp
    jni_bridge.cpp
)

find_library(log-lib log)
find_library(aaudio-lib aaudio)

target_link_libraries(
    zonify_audio_engine
    ${log-lib}
    ${aaudio-lib}
)

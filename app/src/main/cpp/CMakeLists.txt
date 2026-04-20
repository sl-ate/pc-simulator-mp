cmake_minimum_required(VERSION 3.22.1)
project(modhooks LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_library(
    modhooks
    SHARED
    module.cpp
    chook.cpp
    hooks.cpp
    mp_hooks.cpp
    patches.cpp
    memory_utils.cpp
)

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(
    modhooks
    PRIVATE
    ${log-lib}
    ${android-lib}
    dl
)

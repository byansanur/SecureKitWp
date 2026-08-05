cmake_minimum_required(VERSION 3.22.1)

project("secure_kit")

add_library(
        secure_kit
        SHARED
        secure_kit.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        secure_kit
        ${log-lib})

target_link_options(secure_kit PRIVATE "-Wl,-z,max-page-size=16384")

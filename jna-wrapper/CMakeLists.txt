cmake_minimum_required(VERSION 3.10)
project(imagequant_jna VERSION 1.0.0 LANGUAGES C)

# 设置C标准
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

# 设置编译选项
if(MSVC)
    add_compile_options(/W4)
else()
    add_compile_options(-Wall -Wextra -pedantic)
endif()

# 寻找现有的 libimagequant 静态库
find_library(IMAGEQUANT_LIB 
    NAMES imagequant_sys libimagequant_sys
    PATHS 
        ${CMAKE_CURRENT_SOURCE_DIR}/../target/release
        ${CMAKE_CURRENT_SOURCE_DIR}/../imagequant-sys/target/release
    NO_DEFAULT_PATH
)

if(NOT IMAGEQUANT_LIB)
    message(FATAL_ERROR "Could not find libimagequant_sys static library. Please build it first with 'cargo build --release'")
endif()

message(STATUS "Found libimagequant_sys: ${IMAGEQUANT_LIB}")

# 包含目录
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/../imagequant-sys
)

# 添加源文件
set(SOURCES
    src/jna_wrapper.c
)

# 创建动态链接库
add_library(imagequant_jna SHARED ${SOURCES})

# 设置输出名称
set_target_properties(imagequant_jna PROPERTIES
    OUTPUT_NAME "imagequant_jna"
    VERSION ${PROJECT_VERSION}
    SOVERSION 1
)

# 链接静态库
target_link_libraries(imagequant_jna ${IMAGEQUANT_LIB})

# 导出符号设置
if(WIN32)
    target_compile_definitions(imagequant_jna PRIVATE BUILDING_JNA_WRAPPER)
    set_target_properties(imagequant_jna PROPERTIES
        WINDOWS_EXPORT_ALL_SYMBOLS ON
    )
else()
    set_target_properties(imagequant_jna PROPERTIES
        C_VISIBILITY_PRESET hidden
    )
endif()

# 安装规则
install(TARGETS imagequant_jna
    LIBRARY DESTINATION lib
    RUNTIME DESTINATION bin
    ARCHIVE DESTINATION lib
)

install(FILES include/jna_wrapper.h
    DESTINATION include
)

# 添加测试目标（可选）
enable_testing()

# 创建一个简单的测试
add_executable(test_jna_wrapper test/test_basic.c)
target_link_libraries(test_jna_wrapper imagequant_jna)
add_test(NAME basic_test COMMAND test_jna_wrapper)

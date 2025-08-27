#!/bin/bash

# JNA 包装器构建脚本
# 自动构建 libimagequant 的 JNA 动态链接库

set -e  # 遇到错误时退出

echo "=== Building libimagequant JNA Wrapper ==="

# 定义颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 函数：打印状态信息
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查依赖
check_dependencies() {
    print_status "Checking dependencies..."
    
    # 检查 Rust/Cargo
    if ! command -v cargo &> /dev/null; then
        print_error "Cargo not found. Please install Rust first."
        exit 1
    fi
    
    # 检查 GCC
    if ! command -v gcc &> /dev/null; then
        print_error "GCC not found. Please install build tools."
        exit 1
    fi
    
    # 检查 make
    if ! command -v make &> /dev/null; then
        print_warning "Make not found. Will try to use cmake."
        USE_CMAKE=1
    fi
    
    print_status "Dependencies check passed."
}

# 构建 Rust 静态库
build_rust_lib() {
    print_status "Building Rust static library..."
    
    cd ..
    
    # 构建 imagequant-sys 静态库
    print_status "Building imagequant-sys..."
    cd imagequant-sys
    cargo build --release
    cd ..
    
    # 也构建主库（可选）
    print_status "Building main imagequant library..."
    cargo build --release
    
    cd jna-wrapper
    
    print_status "Rust libraries built successfully."
}

# 构建 JNA 包装器
build_jna_wrapper() {
    print_status "Building JNA wrapper..."
    
    if [ "$USE_CMAKE" = "1" ]; then
        build_with_cmake
    else
        build_with_make
    fi
}

# 使用 Make 构建
build_with_make() {
    print_status "Building with Make..."
    make clean
    make all
    print_status "Make build completed."
}

# 使用 CMake 构建
build_with_cmake() {
    print_status "Building with CMake..."
    
    mkdir -p build
    cd build
    cmake ..
    make
    cd ..
    
    # 复制到标准位置
    cp build/libimagequant_jna.* target/ 2>/dev/null || true
    
    print_status "CMake build completed."
}

# 运行测试
run_tests() {
    print_status "Running tests..."
    
    # C 测试
    if [ -f target/libimagequant_jna.so ] || [ -f target/libimagequant_jna.dylib ]; then
        print_status "Running C tests..."
        if [ "$USE_CMAKE" = "1" ]; then
            cd build && ctest && cd ..
        else
            make test
        fi
    else
        print_warning "Dynamic library not found, skipping C tests."
    fi
    
    print_status "Tests completed."
}

# 显示构建信息
show_build_info() {
    print_status "Build information:"
    
    echo "Platform: $(uname -s)"
    echo "Architecture: $(uname -m)"
    
    if [ -f target/libimagequant_jna.so ]; then
        echo "Library: target/libimagequant_jna.so"
        file target/libimagequant_jna.so
    elif [ -f target/libimagequant_jna.dylib ]; then
        echo "Library: target/libimagequant_jna.dylib"
        file target/libimagequant_jna.dylib
    elif [ -f target/libimagequant_jna.dll ]; then
        echo "Library: target/libimagequant_jna.dll"
        file target/libimagequant_jna.dll
    else
        print_warning "No dynamic library found."
    fi
}

# 主构建流程
main() {
    echo "Starting build process..."
    
    # 检查参数
    SKIP_RUST=0
    SKIP_TESTS=0
    USE_CMAKE=0
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            --skip-rust)
                SKIP_RUST=1
                shift
                ;;
            --skip-tests)
                SKIP_TESTS=1
                shift
                ;;
            --use-cmake)
                USE_CMAKE=1
                shift
                ;;
            --help)
                echo "Usage: $0 [options]"
                echo "Options:"
                echo "  --skip-rust   Skip building Rust libraries"
                echo "  --skip-tests  Skip running tests"
                echo "  --use-cmake   Use CMake instead of Make"
                echo "  --help        Show this help"
                exit 0
                ;;
            *)
                print_error "Unknown option: $1"
                exit 1
                ;;
        esac
    done
    
    check_dependencies
    
    if [ "$SKIP_RUST" = "0" ]; then
        build_rust_lib
    else
        print_warning "Skipping Rust library build."
    fi
    
    build_jna_wrapper
    
    if [ "$SKIP_TESTS" = "0" ]; then
        run_tests
    else
        print_warning "Skipping tests."
    fi
    
    show_build_info
    
    print_status "Build completed successfully!"
    echo
    echo "Next steps:"
    echo "1. Copy the dynamic library to your Java library path"
    echo "2. Include the JNA dependency in your Java project"
    echo "3. Use the Java wrapper classes in your application"
}

# 运行主函数
main "$@"

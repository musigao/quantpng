#!/bin/bash

# 本地测试构建脚本# 清理之前的构建
echo "🧹 清理之前的构建..."
rm -rf target/release
rm -rf jna-wrapper/target
mkdiif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    echo "🔗 依赖检查:"
    ldd "$LIB_FILE" || echo "静态链接检查完成"
    
    echo "📋 导出符号:"
    nm -D "$LIB_FILE" | grep jna_ | head -5 || echo "未找到 jna_ 符号"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    echo "🔗 依赖检查:"
    otool -L "$LIB_FILE" || echo "依赖检查完成"
    
    echo "📋 导出符号:"
    nm -D "$LIB_FILE" | grep jna_ | head -5 || echo "未找到 jna_ 符号"
fiper/target

# 构建 Rust 静态库
echo "🏗️ 构建 Rust 静态库..."
cargo build --release -p imagequant-sys

# 检查是否生成了静态库 (文件在workspace的target目录中)
if [ ! -f "target/release/libimagequant_sys.a" ]; then
    echo "❌ Rust 静态库构建失败"
    echo "📋 查找可能的库文件:"
    find target -name "*.a" | head -5
    exit 1
fi

echo "✅ Rust 静态库构建成功"测试构建 ImageQuant JNA 库..."

# 检查必要工具
echo "📋 检查构建环境..."

if ! command -v cargo &> /dev/null; then
    echo "❌ 未找到 Rust/Cargo"
    exit 1
fi

if ! command -v gcc &> /dev/null; then
    echo "❌ 未找到 GCC"
    exit 1
fi

echo "✅ 构建环境检查通过"

# 清理之前的构建
echo "� 清理之前的构建..."
rm -rf imagequant-sys/target/release
rm -rf jna-wrapper/target
mkdir -p jna-wrapper/target

# 构建 Rust 静态库
echo "🏗️ 构建 Rust 静态库..."
cd imagequant-sys
cargo build --release
cd ..

# 检查是否生成了静态库
if [ ! -f "imagequant-sys/target/release/libimagequant_sys.a" ]; then
    echo "❌ Rust 静态库构建失败"
    echo "📋 查找可能的库文件:"
    find imagequant-sys/target -name "*.a" -o -name "*.rlib" | head -5
    exit 1
fi

echo "✅ Rust 静态库构建成功"

# 构建 JNA 包装器
echo "🔨 构建 JNA 包装器..."
cd jna-wrapper

# 编译 C 代码
gcc -std=c11 -Wall -Wextra -O3 -fPIC -fvisibility=hidden \
    -DBUILDING_JNA_WRAPPER \
    -Iinclude -I../imagequant-sys \
    -c src/jna_wrapper.c -o target/jna_wrapper.o

if [ $? -ne 0 ]; then
    echo "❌ C 代码编译失败"
    exit 1
fi

# 创建版本脚本
echo '{ global: jna_*; local: *; };' > target/version.map

# 链接共享库 (适用于 macOS/Linux)
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    gcc -shared -undefined dynamic_lookup \
        -o target/libimagequant_jna.dylib \
        target/jna_wrapper.o \
        ../target/release/libimagequant_sys.a
    LIB_FILE="target/libimagequant_jna.dylib"
else
    # Linux
    gcc -shared -Wl,--version-script=target/version.map \
        -o target/libimagequant_jna.so \
        target/jna_wrapper.o \
        ../target/release/libimagequant_sys.a
    LIB_FILE="target/libimagequant_jna.so"
fi

if [ ! -f "$LIB_FILE" ]; then
    echo "❌ 动态库构建失败"
    exit 1
fi

echo "✅ 动态库构建成功: $LIB_FILE"

# 验证库文件
echo "📄 库文件信息:"
ls -la "$LIB_FILE"
file "$LIB_FILE"

if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    echo "🔗 依赖检查:"
    ldd "$LIB_FILE" || echo "静态链接检查完成"
    
    echo "� 导出符号:"
    nm -D "$LIB_FILE" | grep jna_ | head -5 || echo "未找到 jna_ 符号"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    echo "🔗 依赖检查:"
    otool -L "$LIB_FILE" || echo "依赖检查完成"
    
    echo "📋 导出符号:"
    nm -D "$LIB_FILE" | grep jna_ | head -5 || echo "未找到 jna_ 符号"
fi

echo ""
echo "🎉 本地构建测试完成！"
echo "📦 生成的库文件: jna-wrapper/$LIB_FILE"
echo ""
echo "🚀 现在可以提交代码并在 GitHub Actions 中构建其他平台的库了！"

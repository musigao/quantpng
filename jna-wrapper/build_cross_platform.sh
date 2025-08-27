#!/bin/bash

# 交叉编译脚本 - 生成Windows DLL和Linux SO
# 使用方法: ./build_cross_platform.sh

set -e  # 遇到错误时退出

echo "=== 交叉编译libimagequant JNA包装器 ==="

# 检查必要工具
check_tool() {
    if ! command -v $1 &> /dev/null; then
        echo "❌ 错误: 未找到 $1"
        echo "请安装: $2"
        exit 1
    fi
}

# 检查Rust工具链
echo "📋 检查依赖..."
check_tool "cargo" "Rust (https://rustup.rs/)"

# 首先构建所需的目标平台
TARGETS=("x86_64-pc-windows-gnu" "x86_64-unknown-linux-gnu")

echo "📦 添加Rust目标平台..."
for target in "${TARGETS[@]}"; do
    echo "  添加目标: $target"
    rustup target add $target || echo "  目标 $target 已存在"
done

# 检查交叉编译工具
echo "🔧 检查交叉编译工具..."
if command -v x86_64-w64-mingw32-gcc &> /dev/null; then
    MINGW_CC="x86_64-w64-mingw32-gcc"
    echo "  ✅ Windows交叉编译器: $MINGW_CC"
else
    echo "  ❌ 未找到Windows交叉编译器"
    echo "  macOS安装: brew install mingw-w64"
    echo "  Ubuntu安装: sudo apt-get install gcc-mingw-w64-x86-64"
    exit 1
fi

if command -v x86_64-linux-gnu-gcc &> /dev/null; then
    LINUX_CC="x86_64-linux-gnu-gcc"
    echo "  ✅ Linux交叉编译器: $LINUX_CC"
else
    echo "  ❌ 未找到Linux交叉编译器"
    echo "  macOS安装: brew install x86_64-unknown-linux-gnu"
    echo "  或者使用Docker进行编译"
    LINUX_CC=""
fi

# 清理之前的构建
echo "🧹 清理之前的构建..."
rm -rf target build
mkdir -p target

# 构建基础库
echo "🏗️  构建基础imagequant库..."
cd ../imagequant-sys

# 为Windows构建
echo "  构建Windows版本..."
cargo build --release --target x86_64-pc-windows-gnu
WINDOWS_LIB="target/x86_64-pc-windows-gnu/release/libimagequant_sys.a"

# 为Linux构建 (如果有交叉编译器)
if [ -n "$LINUX_CC" ]; then
    echo "  构建Linux版本..."
    cargo build --release --target x86_64-unknown-linux-gnu
    LINUX_LIB="target/x86_64-unknown-linux-gnu/release/libimagequant_sys.a"
fi

# 回到JNA目录
cd ../jna-wrapper

# 通用编译参数
CFLAGS="-std=c11 -Wall -Wextra -O3 -fPIC -fvisibility=hidden -Iinclude -I../imagequant-sys"

# 创建构建目录
mkdir -p build target

echo "🔨 开始交叉编译..."

# 编译Windows DLL
echo "  编译Windows DLL..."
$MINGW_CC $CFLAGS -DBUILDING_JNA_WRAPPER -c src/jna_wrapper.c -o build/jna_wrapper_windows.o
$MINGW_CC -shared -o target/libimagequant_jna.dll build/jna_wrapper_windows.o "../imagequant-sys/$WINDOWS_LIB" -static-libgcc
echo "  ✅ 生成: target/libimagequant_jna.dll"

# 编译Linux SO (如果可能)
if [ -n "$LINUX_CC" ] && [ -f "../imagequant-sys/$LINUX_LIB" ]; then
    echo "  编译Linux SO..."
    $LINUX_CC $CFLAGS -c src/jna_wrapper.c -o build/jna_wrapper_linux.o
    $LINUX_CC -shared -Wl,--version-script=<(echo "{ global: jna_*; local: *; };") -o target/libimagequant_jna.so build/jna_wrapper_linux.o "../imagequant-sys/$LINUX_LIB"
    echo "  ✅ 生成: target/libimagequant_jna.so"
else
    echo "  ⚠️  跳过Linux SO编译 (缺少交叉编译器或库)"
fi

# 编译当前平台版本 (macOS)
echo "  编译macOS dylib..."
gcc $CFLAGS -c src/jna_wrapper.c -o build/jna_wrapper_macos.o
gcc -shared -undefined dynamic_lookup -o target/libimagequant_jna.dylib build/jna_wrapper_macos.o ../target/release/libimagequant_sys.a
echo "  ✅ 生成: target/libimagequant_jna.dylib"

echo ""
echo "🎉 编译完成！生成的文件:"
ls -la target/libimagequant_jna.*

echo ""
echo "📊 文件大小:"
du -h target/libimagequant_jna.*

echo ""
echo "🔍 文件类型:"
file target/libimagequant_jna.*

echo ""
echo "✅ 交叉编译完成！"
echo "现在可以将这些库文件分发到不同平台使用。"

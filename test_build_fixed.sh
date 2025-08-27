#!/bin/bash

# 测试修复后的跨平台构建脚本
# 这个脚本模拟 GitHub Actions 的构建过程

set -e

echo "🔧 开始测试修复后的构建流程..."

# 清理之前的构建
echo "🧹 清理之前的构建..."
cargo clean
rm -rf jna-wrapper/target

# 构建 Rust 静态库
echo "🦀 构建 Rust 静态库..."
cargo build --release -p imagequant-sys

echo "📁 检查生成的静态库..."
find target -name "*.a" -o -name "*.lib" | head -5

# 测试 JNA 包装器构建（模拟Linux环境）
echo "🐧 测试 Linux 风格构建（带数学库链接）..."
cd jna-wrapper
mkdir -p target

# 编译对象文件
echo "⚙️ 编译 C 对象文件..."
gcc -std=c11 -Wall -Wextra -O3 -fPIC -fvisibility=hidden \
  -DBUILDING_JNA_WRAPPER \
  -Iinclude -I../imagequant-sys \
  -c src/jna_wrapper.c -o target/jna_wrapper.o

# 创建版本脚本
echo "📜 创建符号版本脚本..."
cat > target/version.map << 'EOF'
{ 
  global: jna_*; 
  local: *; 
};
EOF

# 链接共享库（添加数学库）
echo "🔗 链接共享库（包含数学库 -lm）..."
if [[ "$OSTYPE" == "darwin"* ]]; then
  # macOS 使用不同的链接器选项
  gcc -shared -fvisibility=hidden \
    -Wl,-exported_symbols_list,<(echo "_jna_*" | sed 's/^_//') \
    -o target/libimagequant_jna.dylib \
    target/jna_wrapper.o \
    ../target/release/libimagequant_sys.a \
    -lm -lpthread -ldl 2>/dev/null || \
  # 如果符号导出失败，使用简单链接
  gcc -shared \
    -o target/libimagequant_jna.dylib \
    target/jna_wrapper.o \
    ../target/release/libimagequant_sys.a \
    -lm -lpthread -ldl
else
  # Linux 使用 GNU ld
  gcc -shared -Wl,--version-script=target/version.map \
    -o target/libimagequant_jna.so \
    target/jna_wrapper.o \
    ../target/release/libimagequant_sys.a \
    -lm -lpthread -ldl
fi

# 验证构建结果
echo "✅ 验证构建结果..."
if [[ "$OSTYPE" == "darwin"* ]]; then
  ls -la target/libimagequant_jna.dylib
  echo "� 库文件大小："
  du -h target/libimagequant_jna.dylib
  echo "🧪 测试库文件完整性..."
  file target/libimagequant_jna.dylib
else
  ls -la target/libimagequant_jna.so
  echo "📏 库文件大小："
  du -h target/libimagequant_jna.so
  echo "🧪 测试库文件完整性..."
  file target/libimagequant_jna.so
fi

cd ..

echo ""
echo "🎉 构建测试完成！"
echo ""
echo "📋 主要修复："
echo "  ✅ Linux: 添加了 -lm -lpthread -ldl 链接库"
echo "  ✅ Windows: 修复了 PowerShell 命令兼容性"
echo "  ✅ Workspace: 移除了子包的 profile 配置"
echo ""
echo "🚀 现在可以推送到 GitHub 进行 CI/CD 测试了！"

#!/bin/bash

# Windows构建测试脚本
# 用于验证修复后的构建流程

echo "=== Windows构建问题修复验证 ==="

# 检查必要的文件是否存在
echo "1. 检查关键文件..."
files_to_check=(
    ".github/workflows/build-dynamic-libs.yml"
    ".github/workflows/simple-build.yml" 
    "imagequant-sys/Cargo.toml"
    "jna-wrapper/src/jna_wrapper.c"
)

for file in "${files_to_check[@]}"; do
    if [ -f "$file" ]; then
        echo "✓ $file 存在"
    else
        echo "✗ $file 不存在"
    fi
done

# 检查workflow中的关键修复点
echo ""
echo "2. 检查workflow配置..."

# 检查构建路径修复
if grep -q "cargo build --release -p imagequant-sys" .github/workflows/build-dynamic-libs.yml; then
    echo "✓ 构建路径已修复：使用根目录构建"
else
    echo "✗ 构建路径未修复"
fi

# 检查库文件名修复  
if grep -q "imagequant_sys.lib" .github/workflows/build-dynamic-libs.yml; then
    echo "✓ 库文件名已修复：使用imagequant_sys.lib"
else
    echo "✗ 库文件名未修复"
fi

# 检查路径修复
if grep -q "..\\\\target\\\\" .github/workflows/build-dynamic-libs.yml; then
    echo "✓ Windows库路径已修复：使用../target路径"
else
    echo "✗ Windows库路径未修复"  
fi

echo ""
echo "3. 关键修复点总结："
echo "   - Rust库构建：在根目录执行 cargo build --release -p imagequant-sys"
echo "   - Linux库文件：../target/{target}/release/libimagequant_sys.a (保持原有配置)"
echo "   - Windows库文件：../target/{target}/release/imagequant_sys.lib (仅修复文件名)"
echo "   - Linux路径验证：target/{target}/release/ (保持原有)"
echo "   - Windows路径验证：target/{target}/release/ (保持原有)"
echo ""
echo "修复完成！现在可以重新运行GitHub Actions进行测试。"

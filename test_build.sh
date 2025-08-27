#!/bin/bash

# 测试脚本 - 验证JNA包装器功能
# Test script - Verify JNA wrapper functionality

echo "🧪 测试ImageQuant JNA包装器"
echo "🧪 Testing ImageQuant JNA wrapper"

# 检查必要文件
echo "📋 检查必要文件..."
echo "📋 Checking required files..."

if [ -f "jna-wrapper/lib/jna-5.13.0.jar" ]; then
    echo "✅ JNA库文件存在"
    echo "✅ JNA library found"
else
    echo "❌ JNA库文件不存在"
    echo "❌ JNA library not found"
    exit 1
fi

if [ -f "jna-wrapper/src/jna_wrapper.c" ]; then
    echo "✅ C源代码存在"
    echo "✅ C source code found"
else
    echo "❌ C源代码不存在"
    echo "❌ C source code not found"
    exit 1
fi

echo ""
echo "🎉 所有检查通过！"
echo "🎉 All checks passed!"
echo ""
echo "💡 提示：使用GitHub Actions构建跨平台库："
echo "💡 Tip: Use GitHub Actions to build cross-platform libraries:"
echo "   1. 进入Actions标签页"
echo "   1. Go to Actions tab"
echo "   2. 选择 'Quick Build Libraries'"  
echo "   2. Select 'Quick Build Libraries'"
echo "   3. 点击 'Run workflow'"
echo "   3. Click 'Run workflow'"
echo ""

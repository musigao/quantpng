#!/bin/bash

# 简单的Java测试脚本
# 设置环境并编译运行Java测试

set -e

echo "=== Testing Java JNA Interface ==="

# 设置库路径
export LD_LIBRARY_PATH=/Users/sunflow/libimagequant/jna-wrapper/target:$LD_LIBRARY_PATH
export DYLD_LIBRARY_PATH=/Users/sunflow/libimagequant/jna-wrapper/target:$DYLD_LIBRARY_PATH

# 创建临时Java项目目录
TEST_DIR="java_test"
mkdir -p $TEST_DIR/lib

# 下载JNA库（如果不存在）
JNA_JAR="$TEST_DIR/lib/jna-5.13.0.jar"
if [ ! -f "$JNA_JAR" ]; then
    echo "Downloading JNA library..."
    curl -L -o "$JNA_JAR" "https://repo1.maven.org/maven2/net/java/dev/jna/jna/5.13.0/jna-5.13.0.jar"
fi

# 复制Java源文件
cp -r java/org $TEST_DIR/
cp test/JNAWrapperTest.java $TEST_DIR/

# 编译Java代码
echo "Compiling Java code..."
cd $TEST_DIR
javac -cp "lib/jna-5.13.0.jar" org/pngquant/jna/*.java JNAWrapperTest.java

# 运行测试
echo "Running Java test..."
java -cp ".:lib/jna-5.13.0.jar" -Djava.library.path="../target" JNAWrapperTest

echo "Java test completed!"

# 清理
cd ..
rm -rf $TEST_DIR

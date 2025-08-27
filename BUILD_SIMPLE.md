# 简化构建指南

## 概述

本项目现在只保留一个 GitHub Actions 工作流：**Build Dynamic Libraries**，专门用于构建 Windows DLL 和 Linux SO 动态链接库。

## 自动构建

### 触发方式
1. 推送代码到 `main` 分支
2. 手动触发 (GitHub Actions 页面的 "Run workflow" 按钮)

### 构建产物
- `libimagequant_jna.dll` - Windows 64位动态库
- `libimagequant_jna.so` - Linux 64位动态库

### 下载方式
1. 进入 GitHub Actions 页面
2. 点击最新的 "Build Dynamic Libraries" 构建
3. 在 Artifacts 部分下载：
   - `windows-library` - 包含 DLL 文件
   - `linux-library` - 包含 SO 文件
   - `libimagequant-jna-complete` - 完整包(包含库文件、JNA jar、Java代码等)

## 本地测试构建

如果需要在本地测试构建过程，可以运行：

```bash
./test_build_clean.sh
```

这会在 macOS 上构建 `libimagequant_jna.dylib` 文件。

## 技术特点

✅ **修复的问题**：
- Windows 指针类型转换警告 (C4311/C4312)
- Linux 数学库链接问题 (`undefined symbol: powf`)
- PowerShell 命令兼容性问题
- Workspace profile 配置警告

✅ **优化**：
- 静态链接减少依赖
- 符号版本控制和可见性优化
- 完整的错误处理和验证

## 库使用

生成的动态库可以通过 JNA (Java Native Access) 在 Java 项目中使用：

```java
import org.pngquant.jna.PngCompressor;

PngCompressor compressor = new PngCompressor();
boolean success = compressor.compress("input.png", "output.png", 80, 256, 3);
```

## 构建架构

```
libimagequant (Rust crate)
        ↓
imagequant-sys (静态库: .a/.lib)
        ↓
jna-wrapper (C 代码)
        ↓
libimagequant_jna (动态库: .so/.dll/.dylib)
        ↓
Java 应用 (通过 JNA)
```

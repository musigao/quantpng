# 🚀 Quick Start Guide

## 📦 获取跨平台库文件 / Get Cross-Platform Libraries

### 🔧 使用GitHub Actions (推荐)

1. **在GitHub仓库中**：
   - 点击 `Actions` 标签
   - 选择 `Quick Build Libraries` 工作流
   - 点击 `Run workflow`
   - 选择平台：`windows,linux` 或其他组合

2. **等待构建完成** (约3-5分钟)

3. **下载构建结果**：
   - 在构建完成页面下载 `Artifacts`
   - 获得 `.dll`, `.so`, `.dylib` 文件

### 📋 支持的平台

| 平台 | 文件名 | 描述 |
|------|--------|------|
| Windows | `libimagequant_jna.dll` | 64位Windows动态库 |
| Linux | `libimagequant_jna.so` | 64位Linux共享库 |
| macOS | `libimagequant_jna.dylib` | macOS动态库 |

### 💻 本地使用

```java
// Java示例
import org.pngquant.jna.PngCompressor;

PngCompressor compressor = new PngCompressor();
boolean success = compressor.compress("input.png", "output.png", 80, 256, 3);
```

```bash
# 运行时设置库路径
java -Djava.library.path=/path/to/libs -cp jna-5.13.0.jar:. YourApp
```

### 🔧 已修复的问题

- ✅ 颜色通道顺序 (RGBA/BGRA)
- ✅ 跨平台兼容性
- ✅ 内存管理和泄漏
- ✅ 静态链接依赖

---

**最后更新**: $(date)

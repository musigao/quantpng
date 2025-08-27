# libimagequant JNA 包装器

这个项目为 [libimagequant](https://github.com/ImageOptim/libimagequant) 提供了 JNA（Java Native Access）包装器，允许在 Java 应用程序中直接调用 libimagequant 的 C API，而无需使用 JNI。

## 特性

- ✅ 完整的 API 覆盖：支持 PngQuant.java 中定义的所有 native 函数
- ✅ 类型安全：提供强类型的 Java 接口
- ✅ 内存管理：自动处理资源清理
- ✅ 跨平台：支持 Linux、macOS 和 Windows
- ✅ 高级包装：提供与原 JNI 版本兼容的高级 API
- ✅ 高性能：直接调用原生 C 代码

## 项目结构

```
jna-wrapper/
├── src/                    # C 源代码
│   └── jna_wrapper.c      # JNA 包装器实现
├── include/               # 头文件
│   └── jna_wrapper.h      # JNA 包装器头文件
├── java/                  # Java 接口
│   └── org/pngquant/jna/
│       ├── LibImageQuantJNA.java    # 直接 JNA 接口
│       └── PngQuantJNA.java         # 高级包装类
├── test/                  # 测试代码
│   ├── test_basic.c       # C 测试
│   └── JNAWrapperTest.java # Java 测试
├── build.sh              # 构建脚本
├── Makefile              # Make 构建文件
├── CMakeLists.txt        # CMake 构建文件
└── README.md             # 说明文档
```

## 快速开始

### 1. 构建动态链接库

```bash
# 确保你在 libimagequant 根目录下
cd jna-wrapper

# 运行构建脚本（推荐）
./build.sh

# 或者手动构建
make all
```

构建脚本会：
1. 构建 Rust 静态库 (imagequant-sys)
2. 编译 JNA 包装器为动态链接库
3. 运行测试验证功能

### 2. Java 项目集成

在你的 Java 项目中添加 JNA 依赖：

**Maven:**
```xml
<dependency>
    <groupId>net.java.dev.jna</groupId>
    <artifactId>jna</artifactId>
    <version>5.13.0</version>
</dependency>
```

**Gradle:**
```groovy
implementation 'net.java.dev.jna:jna:5.13.0'
```

### 3. 使用示例

#### 方式一：使用高级包装类（推荐）

```java
import org.pngquant.jna.PngQuantJNA;
import java.awt.image.BufferedImage;

// 创建 PngQuant 实例
try (PngQuantJNA pngQuant = new PngQuantJNA()) {
    // 设置参数
    pngQuant.setMaxColors(256);
    pngQuant.setQuality(70, 90);
    pngQuant.setSpeed(3);
    
    // 量化图像
    BufferedImage quantizedImage = pngQuant.getRemapped(originalImage);
    
    if (quantizedImage != null) {
        // 使用量化后的图像
        System.out.println("量化成功！");
    }
}
```

#### 方式二：使用直接 JNA 接口

```java
import org.pngquant.jna.LibImageQuantJNA;

LibImageQuantJNA lib = LibImageQuantJNA.INSTANCE;

// 创建属性对象
long attr = lib.jna_liq_attr_create();

// 设置参数
lib.jna_setMaxColors(attr, 256);
lib.jna_setQuality_range(attr, 70, 90);

// 创建图像
long image = lib.jna_liq_image_create(attr, imageData, width, height, 4);

// 量化
long result = lib.jna_liq_quantize_image(attr, image);

// 获取结果...

// 清理资源
lib.jna_liq_result_destroy(result);
lib.jna_liq_image_destroy(image);
lib.jna_liq_attr_destroy(attr);
```

## API 参考

### PngQuant 类方法

| 方法 | 描述 |
|------|------|
| `setMaxColors(int colors)` | 设置最大颜色数 (1-256) |
| `setQuality(int target)` | 设置目标质量 |
| `setQuality(int min, int max)` | 设置质量范围 |
| `setSpeed(int speed)` | 设置速度 (1-11) |
| `setMinPosterization(int bits)` | 设置最小色调分离 |
| `getRemapped(BufferedImage)` | 一次性量化和重映射 |
| `quantize(ImageJNA)` | 执行量化 |

### Image 类方法

| 方法 | 描述 |
|------|------|
| `addFixedColor(int r, int g, int b, int a)` | 添加固定颜色 |
| `getWidth()` | 获取图像宽度 |
| `getHeight()` | 获取图像高度 |

### Result 类方法

| 方法 | 描述 |
|------|------|
| `getRemapped(ImageJNA)` | 获取重映射图像 |
| `setDitheringLevel(float)` | 设置抖动级别 |
| `setGamma(double)` | 设置伽马值 |
| `getMeanSquareError()` | 获取均方误差 |
| `getQuality()` | 获取实际质量 |

## 构建选项

### 构建脚本选项

```bash
./build.sh [options]

Options:
  --skip-rust   跳过 Rust 库构建
  --skip-tests  跳过测试运行
  --use-cmake   使用 CMake 而不是 Make
  --help        显示帮助信息
```

### 手动构建

```bash
# 使用 Make
make all        # 构建所有
make clean      # 清理
make test       # 运行测试
make install    # 安装到系统（需要 sudo）

# 使用 CMake
mkdir build
cd build
cmake ..
make
```

## 平台支持

| 平台 | 库文件名 | 状态 |
|------|----------|------|
| Linux | `libimagequant_jna.so` | ✅ 支持 |
| macOS | `libimagequant_jna.dylib` | ✅ 支持 |
| Windows | `libimagequant_jna.dll` | ✅ 支持 |

## 依赖要求

### 构建时依赖
- Rust 1.65+ (用于构建 imagequant-sys)
- GCC 或 Clang (用于编译 C 代码)
- Make 或 CMake (用于构建管理)

### 运行时依赖
- JNA 5.0+ (Java 项目依赖)
- 对应平台的动态链接库

## 故障排除

### 常见问题

**1. 找不到动态库**
```
Exception in thread "main" java.lang.UnsatisfiedLinkError: Unable to load library 'imagequant_jna'
```

解决方案：
- 确保动态库在 Java 库路径中
- 设置 `java.library.path` 系统属性
- 或将库复制到工作目录

**2. 构建失败**
```
Could not find libimagequant_sys.a
```

解决方案：
- 运行 `cargo build --release` 构建 Rust 库
- 确保在正确的目录结构中

**3. 版本不兼容**

确保：
- JNA 版本 >= 5.0
- Java 版本 >= 8
- Rust 版本 >= 1.65

### 调试构建

```bash
# 显示详细构建信息
make info

# 强制重新构建
make clean && make all

# 检查库依赖
ldd target/libimagequant_jna.so  # Linux
otool -L target/libimagequant_jna.dylib  # macOS
```

## 性能对比

JNA 包装器与 JNI 实现的性能对比：

| 指标 | JNI | JNA | 差异 |
|------|-----|-----|------|
| 调用开销 | 最低 | 较低 | ~5-10% |
| 开发复杂度 | 高 | 低 | 显著简化 |
| 部署复杂度 | 高 | 低 | 无需编译 |
| 跨平台性 | 复杂 | 简单 | 自动处理 |

## 贡献

欢迎贡献代码！请确保：

1. 遵循现有代码风格
2. 添加适当的测试
3. 更新文档
4. 通过所有测试

## 许可证

本项目继承 libimagequant 的许可证：GPL-3.0-or-later

## 相关链接

- [libimagequant](https://github.com/ImageOptim/libimagequant) - 原始库
- [pngquant](https://pngquant.org/) - PNG 压缩工具
- [JNA](https://github.com/java-native-access/jna) - Java Native Access

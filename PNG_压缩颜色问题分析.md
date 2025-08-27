# PNG压缩颜色问题分析和解决方案

## 问题描述

使用编译好的libimagequant库对PNG图片进行压缩处理时，返回的图片呈现粉色，与原始图片差距很大。

## 问题原因

**根本原因：颜色通道字节顺序错误**

Java中的 `BufferedImage.TYPE_4BYTE_ABGR` 使用的字节顺序是 **A-B-G-R**，但JNA包装器中的C代码 `convert_rgba` 函数错误地假设字节顺序是 **R-G-B-A**。

### 具体分析

1. **Java传递的字节顺序**：ABGR
   - 字节0: Alpha = 255
   - 字节1: Blue = 65  
   - 字节2: Green = 81
   - 字节3: Red = 108

2. **C代码错误的解释**：RGBA  
   - 字节0 → Red = 255 (应该是Alpha)
   - 字节1 → Green = 65 (应该是Blue)
   - 字节2 → Blue = 81 (应该是Green)  
   - 字节3 → Alpha = 108 (应该是Red)

3. **结果**：
   - 原始的棕色 (108, 81, 65) 被错误解释为粉红色 (255, 65, 81)
   - Alpha通道值108导致图像半透明
   - 红色通道值255导致图像呈现强烈的红色/粉色

## 解决方案

修改 `/jna-wrapper/src/jna_wrapper.c` 文件中的 `convert_rgba` 函数，正确处理ABGR字节顺序：

```c
// 修复前 (错误)
static void convert_rgba(liq_color row_out[], int row_index, int width, void* user_info) {
    // ...
    row_out[column_index].r = jnaimg->data[idx + 0];  // 错误：应该是Alpha
    row_out[column_index].g = jnaimg->data[idx + 1];  // 错误：应该是Blue
    row_out[column_index].b = jnaimg->data[idx + 2];  // 错误：应该是Green
    row_out[column_index].a = jnaimg->data[idx + 3];  // 错误：应该是Red
}

// 修复后 (正确)
static void convert_rgba(liq_color row_out[], int row_index, int width, void* user_info) {
    // ...
    row_out[column_index].a = jnaimg->data[idx + 0];  // Alpha
    row_out[column_index].b = jnaimg->data[idx + 1];  // Blue  
    row_out[column_index].g = jnaimg->data[idx + 2];  // Green
    row_out[column_index].r = jnaimg->data[idx + 3];  // Red
}
```

## 验证结果

修复后的测试结果：

- **修复前**：调色板颜色 RGBA=(255, 67, 83, 109) - 粉红色
- **修复后**：调色板颜色 RGBA=(109, 83, 66, 255) - 正确的棕色

## 重新编译步骤

1. 修改 `jna-wrapper/src/jna_wrapper.c` 文件
2. 重新编译JNA库：
   ```bash
   cd jna-wrapper
   make clean
   make
   ```
3. 复制新库到测试目录：
   ```bash
   cp target/libimagequant_jna.dylib maven-test/
   ```

## 总结

这是一个典型的字节序（endianness）和数据格式不匹配问题。在使用JNA或JNI进行Java和C交互时，必须确保两边对数据格式的理解完全一致，特别是：

- 颜色通道顺序 (RGB vs BGR vs ABGR)
- 字节序 (大端 vs 小端)
- 数据类型对应关系

修复后，PNG压缩功能正常工作，输出图片保持正确的颜色。

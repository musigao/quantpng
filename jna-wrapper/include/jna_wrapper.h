#ifndef JNA_WRAPPER_H
#define JNA_WRAPPER_H

#ifdef __cplusplus
extern "C" {
#endif

// 导出符号定义
#if defined(_WIN32) || defined(__CYGWIN__)
    #ifdef BUILDING_JNA_WRAPPER
        #define JNA_EXPORT __declspec(dllexport)
    #else
        #define JNA_EXPORT __declspec(dllimport)
    #endif
#else
    #define JNA_EXPORT __attribute__((visibility("default")))
#endif

// 错误码定义
#define LIQ_JNA_OK 0
#define LIQ_JNA_ERROR 1

// =============================================================================
// PngQuant 类的 native 函数
// =============================================================================

/**
 * 创建 liq_attr 对象
 * @return 属性对象句柄，失败返回0
 */
JNA_EXPORT long jna_liq_attr_create(void);

/**
 * 复制 liq_attr 对象
 * @param orig 原始属性对象句柄
 * @return 新的属性对象句柄，失败返回0
 */
JNA_EXPORT long jna_liq_attr_copy(long orig);

/**
 * 销毁 liq_attr 对象
 * @param handle 属性对象句柄
 */
JNA_EXPORT void jna_liq_attr_destroy(long handle);

/**
 * 设置最大颜色数
 * @param handle 属性对象句柄
 * @param colors 最大颜色数 (1-256)
 * @return LIQ_JNA_OK 成功，LIQ_JNA_ERROR 失败
 */
JNA_EXPORT int jna_setMaxColors(long handle, int colors);

/**
 * 设置质量（单参数版本）
 * @param handle 属性对象句柄
 * @param target 目标质量 (0-100)
 * @return LIQ_JNA_OK 成功，LIQ_JNA_ERROR 失败
 */
JNA_EXPORT int jna_setQuality_single(long handle, int target);

/**
 * 设置质量（双参数版本）
 * @param handle 属性对象句柄
 * @param min 最小质量 (0-100)
 * @param max 最大质量 (0-100)
 * @return LIQ_JNA_OK 成功，LIQ_JNA_ERROR 失败
 */
JNA_EXPORT int jna_setQuality_range(long handle, int min, int max);

/**
 * 设置速度
 * @param handle 属性对象句柄
 * @param speed 速度 (1-11，3为最佳平衡)
 * @return LIQ_JNA_OK 成功，LIQ_JNA_ERROR 失败
 */
JNA_EXPORT int jna_setSpeed(long handle, int speed);

/**
 * 设置最小色调分离
 * @param handle 属性对象句柄
 * @param bits 位数
 * @return LIQ_JNA_OK 成功，LIQ_JNA_ERROR 失败
 */
JNA_EXPORT int jna_setMinPosterization(long handle, int bits);

// =============================================================================
// Image 类的 native 函数
// =============================================================================

/**
 * 创建图像对象
 * @param attr 属性对象句柄
 * @param bitmap 图像数据
 * @param width 图像宽度
 * @param height 图像高度
 * @param components 颜色通道数 (3=RGB, 4=RGBA)
 * @return 图像对象句柄，失败返回0
 */
JNA_EXPORT long jna_liq_image_create(long attr, unsigned char* bitmap, int width, int height, int components);

/**
 * 销毁图像对象
 * @param handle 图像对象句柄
 */
JNA_EXPORT void jna_liq_image_destroy(long handle);

/**
 * 添加固定颜色
 * @param handle 图像对象句柄
 * @param r 红色分量 (0-255)
 * @param g 绿色分量 (0-255)
 * @param b 蓝色分量 (0-255)
 * @param a 透明度分量 (0-255)
 * @return LIQ_JNA_OK 成功，LIQ_JNA_ERROR 失败
 */
JNA_EXPORT int jna_addFixedColor(long handle, int r, int g, int b, int a);

/**
 * 获取图像宽度
 * @param handle 图像对象句柄
 * @return 图像宽度
 */
JNA_EXPORT int jna_getWidth(long handle);

/**
 * 获取图像高度
 * @param handle 图像对象句柄
 * @return 图像高度
 */
JNA_EXPORT int jna_getHeight(long handle);

// =============================================================================
// Result 类的 native 函数
// =============================================================================

/**
 * 量化图像
 * @param attr 属性对象句柄
 * @param image_handle 图像对象句柄
 * @return 结果对象句柄，失败返回0
 */
JNA_EXPORT long jna_liq_quantize_image(long attr, long image_handle);

/**
 * 获取调色板
 * @param result_handle 结果对象句柄
 * @return 调色板指针，失败返回NULL
 */
JNA_EXPORT const void* jna_liq_get_palette(long result_handle);

/**
 * 获取调色板颜色数量
 * @param palette 调色板指针
 * @return 颜色数量
 */
JNA_EXPORT int jna_get_palette_count(const void* palette);

/**
 * 复制调色板数据到缓冲区
 * @param palette 调色板指针
 * @param buffer 目标缓冲区
 * @param buffer_size 缓冲区大小
 * @return LIQ_JNA_OK 成功，LIQ_JNA_ERROR 失败
 */
JNA_EXPORT int jna_copy_palette_data(const void* palette, unsigned char* buffer, int buffer_size);

/**
 * 写入重新映射的图像
 * @param result_handle 结果对象句柄
 * @param image_handle 图像对象句柄
 * @param buffer 输出缓冲区
 * @param buffer_size 缓冲区大小
 * @return LIQ_JNA_OK 成功，LIQ_JNA_ERROR 失败
 */
JNA_EXPORT int jna_liq_write_remapped_image(long result_handle, long image_handle, unsigned char* buffer, int buffer_size);

/**
 * 销毁结果对象
 * @param handle 结果对象句柄
 */
JNA_EXPORT void jna_liq_result_destroy(long handle);

/**
 * 设置抖动级别
 * @param handle 结果对象句柄
 * @param dither_level 抖动级别 (0.0-1.0)
 * @return LIQ_JNA_OK 成功，LIQ_JNA_ERROR 失败
 */
JNA_EXPORT int jna_setDitheringLevel(long handle, float dither_level);

/**
 * 设置输出伽马值
 * @param handle 结果对象句柄
 * @param gamma 伽马值
 * @return LIQ_JNA_OK 成功，LIQ_JNA_ERROR 失败
 */
JNA_EXPORT int jna_setGamma(long handle, double gamma);

/**
 * 获取输出伽马值
 * @param handle 结果对象句柄
 * @return 伽马值
 */
JNA_EXPORT double jna_getGamma(long handle);

/**
 * 获取量化均方误差
 * @param handle 结果对象句柄
 * @return 均方误差，失败返回-1.0
 */
JNA_EXPORT double jna_getMeanSquareError(long handle);

/**
 * 获取量化质量
 * @param handle 结果对象句柄
 * @return 质量值 (0-100)
 */
JNA_EXPORT int jna_getQuality(long handle);

// =============================================================================
// 实用函数
// =============================================================================

/**
 * 获取库版本
 * @return 版本号
 */
JNA_EXPORT int jna_liq_version(void);

/**
 * 检查句柄是否有效
 * @param handle 对象句柄
 * @return LIQ_JNA_OK 有效，LIQ_JNA_ERROR 无效
 */
JNA_EXPORT int jna_is_valid_handle(long handle);

#ifdef __cplusplus
}
#endif

#endif // JNA_WRAPPER_H

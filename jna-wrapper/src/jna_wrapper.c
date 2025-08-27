#include "libimagequant.h"
#include <stdlib.h>
#include <string.h>

// 导出宏定义
#if defined(__GNUC__) || defined(__clang__)
    #define JNA_EXPORT __attribute__((visibility("default")))
#elif defined(_WIN32)
    #define JNA_EXPORT __declspec(dllexport)
#else
    #define JNA_EXPORT
#endif

// 定义简化的错误码，与JNA兼容
#define LIQ_JNA_OK 0
#define LIQ_JNA_ERROR 1

// 简化的图像数据结构，用于JNA
typedef struct {
    liq_image *image;
    unsigned char *data;
    int width;
    int height;
    int components;
} liq_jna_image;

// =============================================================================
// PngQuant 类的 native 函数实现
// =============================================================================

/**
 * 创建 liq_attr 对象
 * 对应 Java: private static native long liq_attr_create();
 */
JNA_EXPORT long jna_liq_attr_create() {
    liq_attr *attr = liq_attr_create();
    return (long)attr;
}

/**
 * 复制 liq_attr 对象
 * 对应 Java: private static native long liq_attr_copy(long orig);
 */
JNA_EXPORT long jna_liq_attr_copy(long orig) {
    liq_attr *attr = liq_attr_copy((liq_attr*)orig);
    return (long)attr;
}

/**
 * 销毁 liq_attr 对象
 * 对应 Java: private static native void liq_attr_destroy(long handle);
 */
JNA_EXPORT void jna_liq_attr_destroy(long handle) {
    if (handle != 0) {
        liq_attr_destroy((liq_attr*)handle);
    }
}

/**
 * 设置最大颜色数
 * 对应 Java: public native boolean setMaxColors(int colors);
 */
JNA_EXPORT int jna_setMaxColors(long handle, int colors) {
    if (handle == 0) return LIQ_JNA_ERROR;
    liq_error error = liq_set_max_colors((liq_attr*)handle, colors);
    return (error == LIQ_OK) ? LIQ_JNA_OK : LIQ_JNA_ERROR;
}

/**
 * 设置质量（单参数版本）
 * 对应 Java: public native boolean setQuality(int target);
 */
JNA_EXPORT int jna_setQuality_single(long handle, int target) {
    if (handle == 0) return LIQ_JNA_ERROR;
    liq_error error = liq_set_quality((liq_attr*)handle, target/2, target);
    return (error == LIQ_OK) ? LIQ_JNA_OK : LIQ_JNA_ERROR;
}

/**
 * 设置质量（双参数版本）
 * 对应 Java: public native boolean setQuality(int min, int max);
 */
JNA_EXPORT int jna_setQuality_range(long handle, int min, int max) {
    if (handle == 0) return LIQ_JNA_ERROR;
    liq_error error = liq_set_quality((liq_attr*)handle, min, max);
    return (error == LIQ_OK) ? LIQ_JNA_OK : LIQ_JNA_ERROR;
}

/**
 * 设置速度
 * 对应 Java: public native boolean setSpeed(int speed);
 */
JNA_EXPORT int jna_setSpeed(long handle, int speed) {
    if (handle == 0) return LIQ_JNA_ERROR;
    liq_error error = liq_set_speed((liq_attr*)handle, speed);
    return (error == LIQ_OK) ? LIQ_JNA_OK : LIQ_JNA_ERROR;
}

/**
 * 设置最小色调分离
 * 对应 Java: public native boolean setMinPosterization(int bits);
 */
JNA_EXPORT int jna_setMinPosterization(long handle, int bits) {
    if (handle == 0) return LIQ_JNA_ERROR;
    liq_error error = liq_set_min_posterization((liq_attr*)handle, bits);
    return (error == LIQ_OK) ? LIQ_JNA_OK : LIQ_JNA_ERROR;
}

// =============================================================================
// Image 类的 native 函数实现
// =============================================================================

// 像素转换函数：RGBA (修复ABGR字节顺序)
static void convert_rgba(liq_color row_out[], int row_index, int width, void* user_info) {
    liq_jna_image *jnaimg = (liq_jna_image*)user_info;
    int column_index;
    for(column_index = 0; column_index < width; column_index++) {
        int idx = 4 * (width * row_index + column_index);
        // Java BufferedImage.TYPE_4BYTE_ABGR 字节顺序是 A-B-G-R
        row_out[column_index].a = jnaimg->data[idx + 0];  // Alpha
        row_out[column_index].b = jnaimg->data[idx + 1];  // Blue  
        row_out[column_index].g = jnaimg->data[idx + 2];  // Green
        row_out[column_index].r = jnaimg->data[idx + 3];  // Red
    }
}

// 像素转换函数：RGB
static void convert_rgb(liq_color row_out[], int row_index, int width, void* user_info) {
    liq_jna_image *jnaimg = (liq_jna_image*)user_info;
    int column_index;
    for(column_index = 0; column_index < width; column_index++) {
        int idx = 3 * (width * row_index + column_index);
        row_out[column_index].r = jnaimg->data[idx + 0];
        row_out[column_index].g = jnaimg->data[idx + 1];
        row_out[column_index].b = jnaimg->data[idx + 2];
        row_out[column_index].a = 255;
    }
}

/**
 * 创建图像对象
 * 对应 Java: private static native long liq_image_create(long attr, byte[] bitmap, int width, int height, int components);
 */
JNA_EXPORT long jna_liq_image_create(long attr, unsigned char* bitmap, int width, int height, int components) {
    if (attr == 0 || bitmap == NULL) return 0;
    
    liq_jna_image *jnaimg = malloc(sizeof(liq_jna_image));
    if (!jnaimg) return 0;
    
    // 复制图像数据
    int size = width * height * components;
    jnaimg->data = malloc(size);
    if (!jnaimg->data) {
        free(jnaimg);
        return 0;
    }
    memcpy(jnaimg->data, bitmap, size);
    
    jnaimg->width = width;
    jnaimg->height = height;
    jnaimg->components = components;
    
    // 根据组件数选择合适的转换函数并创建图像
    if (components == 4) {
        jnaimg->image = liq_image_create_custom((liq_attr*)attr, convert_rgba, jnaimg, width, height, 0);
    } else {
        jnaimg->image = liq_image_create_custom((liq_attr*)attr, convert_rgb, jnaimg, width, height, 0);
    }
    
    if (!jnaimg->image) {
        free(jnaimg->data);
        free(jnaimg);
        return 0;
    }
    
    return (long)jnaimg;
}

/**
 * 销毁图像对象
 * 对应 Java: private static native void liq_image_destroy(long handle);
 */
JNA_EXPORT void jna_liq_image_destroy(long handle) {
    if (handle == 0) return;
    
    liq_jna_image *jnaimg = (liq_jna_image*)handle;
    if (jnaimg->image) {
        liq_image_destroy(jnaimg->image);
    }
    if (jnaimg->data) {
        free(jnaimg->data);
    }
    free(jnaimg);
}

/**
 * 添加固定颜色
 * 对应 Java: public native boolean addFixedColor(int r, int g, int b, int a);
 */
JNA_EXPORT int jna_addFixedColor(long handle, int r, int g, int b, int a) {
    if (handle == 0) return LIQ_JNA_ERROR;
    
    liq_jna_image *jnaimg = (liq_jna_image*)handle;
    liq_color color = {r, g, b, a};
    liq_error error = liq_image_add_fixed_color(jnaimg->image, color);
    return (error == LIQ_OK) ? LIQ_JNA_OK : LIQ_JNA_ERROR;
}

/**
 * 获取图像宽度
 * 对应 Java: public native int getWidth();
 */
JNA_EXPORT int jna_getWidth(long handle) {
    if (handle == 0) return 0;
    liq_jna_image *jnaimg = (liq_jna_image*)handle;
    return liq_image_get_width(jnaimg->image);
}

/**
 * 获取图像高度
 * 对应 Java: public native int getHeight();
 */
JNA_EXPORT int jna_getHeight(long handle) {
    if (handle == 0) return 0;
    liq_jna_image *jnaimg = (liq_jna_image*)handle;
    return liq_image_get_height(jnaimg->image);
}

// =============================================================================
// Result 类的 native 函数实现
// =============================================================================

/**
 * 量化图像
 * 对应 Java: private static native long liq_quantize_image(long attr, long image);
 */
JNA_EXPORT long jna_liq_quantize_image(long attr, long image_handle) {
    if (attr == 0 || image_handle == 0) return 0;
    
    liq_jna_image *jnaimg = (liq_jna_image*)image_handle;
    liq_result *result = liq_quantize_image((liq_attr*)attr, jnaimg->image);
    return (long)result;
}

/**
 * 获取调色板
 * 对应 Java: private static native byte[] liq_get_palette(long handle);
 * 注意：JNA版本返回调色板大小和数据指针，需要在Java端处理
 */
JNA_EXPORT const liq_palette* jna_liq_get_palette(long result_handle) {
    if (result_handle == 0) return NULL;
    return liq_get_palette((liq_result*)result_handle);
}

/**
 * 获取调色板数据
 * 辅助函数：获取调色板的颜色数量
 */
JNA_EXPORT int jna_get_palette_count(const liq_palette* palette) {
    return palette ? palette->count : 0;
}

/**
 * 获取调色板字节数据 (模拟原始JNI的方式)
 * 返回调色板的实际大小，调色板数据写入buffer
 */
JNA_EXPORT int jna_get_palette_bytes(long result_handle, unsigned char* buffer, int buffer_size) {
    if (result_handle == 0) return -1;
    
    const liq_palette *pal = liq_get_palette((liq_result*)result_handle);
    if (!pal) return -1;
    
    int required_size = pal->count * 4;
    if (buffer == NULL) return required_size; // 只返回需要的大小
    
    if (buffer_size < required_size) return -1;
    
    // 直接复制内存，就像原始JNI实现
    for (unsigned int i = 0; i < pal->count; i++) {
        memcpy(buffer + i * 4, &pal->entries[i], 4);
    }
    return required_size;
}

/**
 * 获取调色板数据
 * 辅助函数：将调色板数据复制到缓冲区
 */
JNA_EXPORT int jna_copy_palette_data(const liq_palette* palette, unsigned char* buffer, int buffer_size) {
    if (!palette || !buffer || buffer_size < (int)(palette->count * 4)) return LIQ_JNA_ERROR;
    
    // 直接复制内存，就像原始JNI实现一样
    for (unsigned int i = 0; i < palette->count; i++) {
        memcpy(buffer + i * 4, &palette->entries[i], 4);
    }
    return LIQ_JNA_OK;
}

/**
 * 写入重新映射的图像
 * 对应 Java: private static native boolean liq_write_remapped_image(long handle, long image, byte[] buffer);
 */
JNA_EXPORT int jna_liq_write_remapped_image(long result_handle, long image_handle, unsigned char* buffer, int buffer_size) {
    if (result_handle == 0 || image_handle == 0 || buffer == NULL) return LIQ_JNA_ERROR;
    
    liq_jna_image *jnaimg = (liq_jna_image*)image_handle;
    liq_error error = liq_write_remapped_image((liq_result*)result_handle, jnaimg->image, buffer, buffer_size);
    return (error == LIQ_OK) ? LIQ_JNA_OK : LIQ_JNA_ERROR;
}

/**
 * 销毁结果对象
 * 对应 Java: private static native void liq_result_destroy(long handle);
 */
JNA_EXPORT void jna_liq_result_destroy(long handle) {
    if (handle != 0) {
        liq_result_destroy((liq_result*)handle);
    }
}

/**
 * 设置抖动级别
 * 对应 Java: public native boolean setDitheringLevel(float dither_level);
 */
JNA_EXPORT int jna_setDitheringLevel(long handle, float dither_level) {
    if (handle == 0) return LIQ_JNA_ERROR;
    liq_error error = liq_set_dithering_level((liq_result*)handle, dither_level);
    return (error == LIQ_OK) ? LIQ_JNA_OK : LIQ_JNA_ERROR;
}

/**
 * 设置输出伽马值
 * 对应 Java: public native boolean setGamma(double gamma);
 */
JNA_EXPORT int jna_setGamma(long handle, double gamma) {
    if (handle == 0) return LIQ_JNA_ERROR;
    liq_error error = liq_set_output_gamma((liq_result*)handle, gamma);
    return (error == LIQ_OK) ? LIQ_JNA_OK : LIQ_JNA_ERROR;
}

/**
 * 获取输出伽马值
 * 对应 Java: public native double getGamma();
 */
JNA_EXPORT double jna_getGamma(long handle) {
    if (handle == 0) return 0.0;
    return liq_get_output_gamma((liq_result*)handle);
}

/**
 * 获取量化均方误差
 * 对应 Java: public native double getMeanSquareError();
 */
JNA_EXPORT double jna_getMeanSquareError(long handle) {
    if (handle == 0) return -1.0;
    return liq_get_quantization_error((liq_result*)handle);
}

/**
 * 获取量化质量
 * 对应 Java: public native int getQuality();
 */
JNA_EXPORT int jna_getQuality(long handle) {
    if (handle == 0) return 0;
    return liq_get_quantization_quality((liq_result*)handle);
}

// =============================================================================
// 额外的实用函数
// =============================================================================

/**
 * 获取库版本
 */
JNA_EXPORT int jna_liq_version() {
    return liq_version();
}

/**
 * 检查句柄是否有效
 */
JNA_EXPORT int jna_is_valid_handle(long handle) {
    return (handle != 0) ? LIQ_JNA_OK : LIQ_JNA_ERROR;
}

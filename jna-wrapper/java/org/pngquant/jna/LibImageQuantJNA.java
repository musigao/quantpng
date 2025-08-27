package org.pngquant.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * JNA接口，用于直接调用libimagequant的C函数
 * 
 * 使用示例：
 * <pre>
 * LibImageQuantJNA lib = LibImageQuantJNA.INSTANCE;
 * long attr = lib.jna_liq_attr_create();
 * lib.jna_setMaxColors(attr, 256);
 * // ... 其他操作
 * lib.jna_liq_attr_destroy(attr);
 * </pre>
 */
public interface LibImageQuantJNA extends Library {
    
    /**
     * JNA库实例
     */
    LibImageQuantJNA INSTANCE = Native.load("imagequant_jna", LibImageQuantJNA.class);
    
    // 错误码常量
    int LIQ_JNA_OK = 0;
    int LIQ_JNA_ERROR = 1;
    
    // =============================================================================
    // PngQuant 类的 native 函数
    // =============================================================================
    
    /**
     * 创建 liq_attr 对象
     * @return 属性对象句柄，失败返回0
     */
    long jna_liq_attr_create();
    
    /**
     * 复制 liq_attr 对象
     * @param orig 原始属性对象句柄
     * @return 新的属性对象句柄，失败返回0
     */
    long jna_liq_attr_copy(long orig);
    
    /**
     * 销毁 liq_attr 对象
     * @param handle 属性对象句柄
     */
    void jna_liq_attr_destroy(long handle);
    
    /**
     * 设置最大颜色数
     * @param handle 属性对象句柄
     * @param colors 最大颜色数 (1-256)
     * @return LIQ_JNA_OK 成功，LIQ_JNA_ERROR 失败
     */
    int jna_setMaxColors(long handle, int colors);
    
    /**
     * 设置质量（单参数版本）
     * @param handle 属性对象句柄
     * @param target 目标质量 (0-100)
     * @return LIQ_JNA_OK 成功，LIQ_JNA_ERROR 失败
     */
    int jna_setQuality_single(long handle, int target);
    
    /**
     * 设置质量（双参数版本）
     * @param handle 属性对象句柄
     * @param min 最小质量 (0-100)
     * @param max 最大质量 (0-100)
     * @return LIQ_JNA_OK 成功，LIQ_JNA_ERROR 失败
     */
    int jna_setQuality_range(long handle, int min, int max);
    
    /**
     * 设置速度
     * @param handle 属性对象句柄
     * @param speed 速度 (1-11，3为最佳平衡)
     * @return LIQ_JNA_OK 成功，LIQ_JNA_ERROR 失败
     */
    int jna_setSpeed(long handle, int speed);
    
    /**
     * 设置最小色调分离
     * @param handle 属性对象句柄
     * @param bits 位数
     * @return LIQ_JNA_OK 成功，LIQ_JNA_ERROR 失败
     */
    int jna_setMinPosterization(long handle, int bits);
    
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
    long jna_liq_image_create(long attr, byte[] bitmap, int width, int height, int components);
    
    /**
     * 销毁图像对象
     * @param handle 图像对象句柄
     */
    void jna_liq_image_destroy(long handle);
    
    /**
     * 添加固定颜色
     * @param handle 图像对象句柄
     * @param r 红色分量 (0-255)
     * @param g 绿色分量 (0-255)
     * @param b 蓝色分量 (0-255)
     * @param a 透明度分量 (0-255)
     * @return LIQ_JNA_OK 成功，LIQ_JNA_ERROR 失败
     */
    int jna_addFixedColor(long handle, int r, int g, int b, int a);
    
    /**
     * 获取图像宽度
     * @param handle 图像对象句柄
     * @return 图像宽度
     */
    int jna_getWidth(long handle);
    
    /**
     * 获取图像高度
     * @param handle 图像对象句柄
     * @return 图像高度
     */
    int jna_getHeight(long handle);
    
    // =============================================================================
    // Result 类的 native 函数
    // =============================================================================
    
    /**
     * 量化图像
     * @param attr 属性对象句柄
     * @param image_handle 图像对象句柄
     * @return 结果对象句柄，失败返回0
     */
    long jna_liq_quantize_image(long attr, long image_handle);
    
    /**
     * 获取调色板
     * @param result_handle 结果对象句柄
     * @return 调色板指针，失败返回null
     */
    Pointer jna_liq_get_palette(long result_handle);
    
    /**
     * 获取调色板颜色数量
     * @param palette 调色板指针
     * @return 颜色数量
     */
    int jna_get_palette_count(Pointer palette);
    
    /**
     * 复制调色板数据到缓冲区
     * @param palette 调色板指针
     * @param buffer 目标缓冲区
     * @param buffer_size 缓冲区大小
     * @return LIQ_JNA_OK 成功，LIQ_JNA_ERROR 失败
     */
    int jna_copy_palette_data(Pointer palette, byte[] buffer, int buffer_size);
    
    /**
     * 写入重新映射的图像
     * @param result_handle 结果对象句柄
     * @param image_handle 图像对象句柄
     * @param buffer 输出缓冲区
     * @param buffer_size 缓冲区大小
     * @return LIQ_JNA_OK 成功，LIQ_JNA_ERROR 失败
     */
    int jna_liq_write_remapped_image(long result_handle, long image_handle, byte[] buffer, int buffer_size);
    
    /**
     * 销毁结果对象
     * @param handle 结果对象句柄
     */
    void jna_liq_result_destroy(long handle);
    
    /**
     * 设置抖动级别
     * @param handle 结果对象句柄
     * @param dither_level 抖动级别 (0.0-1.0)
     * @return LIQ_JNA_OK 成功，LIQ_JNA_ERROR 失败
     */
    int jna_setDitheringLevel(long handle, float dither_level);
    
    /**
     * 设置输出伽马值
     * @param handle 结果对象句柄
     * @param gamma 伽马值
     * @return LIQ_JNA_OK 成功，LIQ_JNA_ERROR 失败
     */
    int jna_setGamma(long handle, double gamma);
    
    /**
     * 获取输出伽马值
     * @param handle 结果对象句柄
     * @return 伽马值
     */
    double jna_getGamma(long handle);
    
    /**
     * 获取量化均方误差
     * @param handle 结果对象句柄
     * @return 均方误差，失败返回-1.0
     */
    double jna_getMeanSquareError(long handle);
    
    /**
     * 获取量化质量
     * @param handle 结果对象句柄
     * @return 质量值 (0-100)
     */
    int jna_getQuality(long handle);
    
    // =============================================================================
    // 实用函数
    // =============================================================================
    
    /**
     * 获取库版本
     * @return 版本号
     */
    int jna_liq_version();
    
    /**
     * 检查句柄是否有效
     * @param handle 对象句柄
     * @return LIQ_JNA_OK 有效，LIQ_JNA_ERROR 无效
     */
    int jna_is_valid_handle(long handle);
}

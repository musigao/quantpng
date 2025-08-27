package org.pngquant.jna;

import com.sun.jna.Pointer;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;

/**
 * 基于JNA的PngQuant高级包装类
 * 提供与原JNI版本兼容的API，但使用JNA进行底层调用
 */
public class PngQuantJNA {
    
    private static final LibImageQuantJNA lib = LibImageQuantJNA.INSTANCE;
    private long handle;
    
    /**
     * 创建新的PngQuant实例
     */
    public PngQuantJNA() {
        handle = lib.jna_liq_attr_create();
        if (handle == 0) {
            throw new RuntimeException("Failed to create liq_attr");
        }
    }
    
    /**
     * 复制构造函数
     */
    public PngQuantJNA(PngQuantJNA other) {
        handle = lib.jna_liq_attr_copy(other.handle);
        if (handle == 0) {
            throw new RuntimeException("Failed to copy liq_attr");
        }
    }
    
    /**
     * 一次性量化和重映射
     * @param bufimg 输入图像
     * @return 8位索引图像或null（失败时）
     */
    public BufferedImage getRemapped(BufferedImage bufimg) {
        try {
            ImageJNA liqimg = new ImageJNA(this, bufimg);
            BufferedImage remapped = getRemapped(liqimg);
            liqimg.close();
            return remapped;
        } catch(Exception e) {
            return null;
        }
    }
    
    /**
     * 使用已创建的Image对象进行重映射
     */
    public BufferedImage getRemapped(ImageJNA liqimg) {
        ResultJNA result = quantize(liqimg);
        if (result == null) return null;
        BufferedImage remapped = result.getRemapped(liqimg);
        result.close();
        return remapped;
    }
    
    /**
     * 执行量化（选择最优调色板）
     */
    public ResultJNA quantize(ImageJNA img) {
        try {
            return new ResultJNA(this, img);
        } catch(Exception e) {
            return null;
        }
    }
    
    /**
     * 设置最大颜色数
     */
    public boolean setMaxColors(int colors) {
        return lib.jna_setMaxColors(handle, colors) == LibImageQuantJNA.LIQ_JNA_OK;
    }
    
    /**
     * 设置质量（单参数版本）
     */
    public boolean setQuality(int target) {
        return lib.jna_setQuality_single(handle, target) == LibImageQuantJNA.LIQ_JNA_OK;
    }
    
    /**
     * 设置质量范围
     */
    public boolean setQuality(int min, int max) {
        return lib.jna_setQuality_range(handle, min, max) == LibImageQuantJNA.LIQ_JNA_OK;
    }
    
    /**
     * 设置速度
     */
    public boolean setSpeed(int speed) {
        return lib.jna_setSpeed(handle, speed) == LibImageQuantJNA.LIQ_JNA_OK;
    }
    
    /**
     * 设置最小色调分离
     */
    public boolean setMinPosterization(int bits) {
        return lib.jna_setMinPosterization(handle, bits) == LibImageQuantJNA.LIQ_JNA_OK;
    }
    
    /**
     * 释放资源
     */
    public void close() {
        if (handle != 0) {
            lib.jna_liq_attr_destroy(handle);
            handle = 0;
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
    
    // 包内部访问
    long getHandle() {
        return handle;
    }
    
    /**
     * 图像包装类
     */
    public static class ImageJNA {
        private long handle;
        private int width;
        private int height;
        
        public ImageJNA(BufferedImage image) throws Exception {
            this(new PngQuantJNA(), image);
        }
        
        public ImageJNA(PngQuantJNA attr, BufferedImage image) throws Exception {
            this.handle = createFromBufferedImage(attr.getHandle(), image);
            if (handle == 0) {
                // 尝试转换为标准格式
                BufferedImage converted = new BufferedImage(
                    image.getWidth(), image.getHeight(), 
                    BufferedImage.TYPE_4BYTE_ABGR);
                converted.getGraphics().drawImage(image, 0, 0, null);
                this.handle = createFromBufferedImage(attr.getHandle(), converted);
                if (handle == 0) {
                    throw new Exception("Failed to create image");
                }
            }
            this.width = lib.jna_getWidth(handle);
            this.height = lib.jna_getHeight(handle);
        }
        
        private static long createFromBufferedImage(long attr, BufferedImage image) {
            // 支持的格式检查
            int type = image.getType();
            if (type != BufferedImage.TYPE_3BYTE_BGR &&
                type != BufferedImage.TYPE_4BYTE_ABGR &&
                type != BufferedImage.TYPE_4BYTE_ABGR_PRE) {
                return 0;
            }
            
            // 获取像素数据
            DataBufferByte buffer = (DataBufferByte) image.getRaster().getDataBuffer();
            byte[] imageData = buffer.getData();
            
            int components = (type == BufferedImage.TYPE_3BYTE_BGR) ? 3 : 4;
            
            return lib.jna_liq_image_create(attr, imageData, 
                image.getWidth(), image.getHeight(), components);
        }
        
        /**
         * 添加固定颜色
         */
        public boolean addFixedColor(int r, int g, int b, int a) {
            return lib.jna_addFixedColor(handle, r, g, b, a) == LibImageQuantJNA.LIQ_JNA_OK;
        }
        
        public boolean addFixedColor(int r, int g, int b) {
            return addFixedColor(r, g, b, 255);
        }
        
        public int getWidth() {
            return width;
        }
        
        public int getHeight() {
            return height;
        }
        
        public void close() {
            if (handle != 0) {
                lib.jna_liq_image_destroy(handle);
                handle = 0;
            }
        }
        
        @Override
        protected void finalize() throws Throwable {
            close();
            super.finalize();
        }
        
        long getHandle() {
            return handle;
        }
    }
    
    /**
     * 量化结果包装类
     */
    public static class ResultJNA {
        private long handle;
        
        public ResultJNA(PngQuantJNA pngquant, ImageJNA image) throws Exception {
            handle = lib.jna_liq_quantize_image(pngquant.getHandle(), image.getHandle());
            if (handle == 0) {
                throw new Exception("Quantization failed");
            }
        }
        
        /**
         * 获取重映射的图像
         */
        public BufferedImage getRemapped(ImageJNA origImage) {
            // 获取调色板
            Pointer palette = lib.jna_liq_get_palette(handle);
            if (palette == null) return null;
            
            int paletteSize = lib.jna_get_palette_count(palette);
            byte[] paletteData = new byte[paletteSize * 4];
            
            if (lib.jna_copy_palette_data(palette, paletteData, paletteData.length) 
                != LibImageQuantJNA.LIQ_JNA_OK) {
                return null;
            }
            
            // 创建IndexColorModel
            IndexColorModel colorModel = new IndexColorModel(8, paletteSize, 
                paletteData, 0, true);
            
            // 创建输出图像
            BufferedImage img = new BufferedImage(
                origImage.getWidth(), origImage.getHeight(),
                BufferedImage.TYPE_BYTE_INDEXED, colorModel);
            
            // 获取8位数据缓冲区
            DataBufferByte buffer = (DataBufferByte) img.getRaster().getDataBuffer();
            byte[] data = buffer.getData();
            
            // 写入重映射数据
            if (lib.jna_liq_write_remapped_image(handle, origImage.getHandle(), 
                data, data.length) != LibImageQuantJNA.LIQ_JNA_OK) {
                return null;
            }
            
            return img;
        }
        
        /**
         * 设置抖动级别
         */
        public boolean setDitheringLevel(float dither_level) {
            return lib.jna_setDitheringLevel(handle, dither_level) == LibImageQuantJNA.LIQ_JNA_OK;
        }
        
        /**
         * 设置伽马值
         */
        public boolean setGamma(double gamma) {
            return lib.jna_setGamma(handle, gamma) == LibImageQuantJNA.LIQ_JNA_OK;
        }
        
        public double getGamma() {
            return lib.jna_getGamma(handle);
        }
        
        /**
         * 获取量化均方误差
         */
        public double getMeanSquareError() {
            return lib.jna_getMeanSquareError(handle);
        }
        
        /**
         * 获取量化质量
         */
        public int getQuality() {
            return lib.jna_getQuality(handle);
        }
        
        public void close() {
            if (handle != 0) {
                lib.jna_liq_result_destroy(handle);
                handle = 0;
            }
        }
        
        @Override
        protected void finalize() throws Throwable {
            close();
            super.finalize();
        }
        
        long getHandle() {
            return handle;
        }
    }
}

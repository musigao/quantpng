package org.pngquant.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;

/**
 * PNG图像压缩工具 - 主类
 * 集成了所有必要的功能，可以直接使用
 * 自动检测平台并加载对应的动态库
 */
public class PngCompressor {
    
    // JNA接口定义
    interface LibImageQuantJNA extends Library {
        LibImageQuantJNA INSTANCE = loadLibrary();
        
        // 常量
        int LIQ_JNA_OK = 0;
        int LIQ_JNA_ERROR = 1;
        
        // 核心函数
        long jna_liq_attr_create();
        void jna_liq_attr_destroy(long handle);
        int jna_setMaxColors(long handle, int colors);
        int jna_setQuality_single(long handle, int quality);
        int jna_setQuality_range(long handle, int min, int max);
        int jna_setSpeed(long handle, int speed);
        int jna_setMinPosterization(long handle, int bits);
        
        long jna_liq_image_create(long attr, byte[] bitmap, int width, int height, int components);
        void jna_liq_image_destroy(long handle);
        
        long jna_liq_quantize_image(long attr, long image_handle);
        Pointer jna_liq_get_palette(long result_handle);
        int jna_get_palette_count(Pointer palette);
        int jna_copy_palette_data(Pointer palette, byte[] buffer, int buffer_size);
        int jna_liq_write_remapped_image(long result_handle, long image_handle, byte[] buffer, int buffer_size);
        void jna_liq_result_destroy(long handle);
        
        int jna_liq_version();
        double jna_getMeanSquareError(long handle);
        int jna_getQuality(long handle);
        
        /**
         * 加载平台特定的库
         */
        static LibImageQuantJNA loadLibrary() {
            String osName = System.getProperty("os.name").toLowerCase();
            String libName = "imagequant_jna";
            
            try {
                // 首先尝试标准库名
                return Native.load(libName, LibImageQuantJNA.class);
            } catch (UnsatisfiedLinkError e) {
                System.err.println("无法加载标准库名，尝试平台特定的库文件...");
                
                // 确定平台特定的库文件名
                String platformLib;
                if (osName.contains("windows")) {
                    platformLib = "./libimagequant_jna.dll";
                } else if (osName.contains("linux")) {
                    platformLib = "./libimagequant_jna.so";
                } else if (osName.contains("mac")) {
                    platformLib = "./libimagequant_jna.dylib";
                } else {
                    throw new RuntimeException("不支持的操作系统: " + osName);
                }
                
                try {
                    return Native.load(platformLib, LibImageQuantJNA.class);
                } catch (UnsatisfiedLinkError e2) {
                    System.err.println("无法加载平台库: " + platformLib);
                    System.err.println("请确保库文件在当前目录或java.library.path中");
                    System.err.println("当前目录: " + System.getProperty("user.dir"));
                    System.err.println("java.library.path: " + System.getProperty("java.library.path"));
                    throw new RuntimeException("无法加载ImageQuant JNA库", e2);
                }
            }
        }
    }
    
    private final LibImageQuantJNA lib;
    
    // 压缩参数
    private int maxColors = 256;
    private int qualityMin = 70;
    private int qualityMax = 90;
    private int speed = 3;
    private int minPosterization = 0;
    
    public PngCompressor() {
        this.lib = LibImageQuantJNA.INSTANCE;
    }
    
    // 设置参数的方法
    public PngCompressor setMaxColors(int colors) {
        this.maxColors = Math.max(1, Math.min(256, colors));
        return this;
    }
    
    public PngCompressor setQuality(int quality) {
        this.qualityMin = Math.max(0, Math.min(100, quality));
        this.qualityMax = this.qualityMin;
        return this;
    }
    
    public PngCompressor setQualityRange(int min, int max) {
        this.qualityMin = Math.max(0, Math.min(100, min));
        this.qualityMax = Math.max(this.qualityMin, Math.min(100, max));
        return this;
    }
    
    public PngCompressor setSpeed(int speed) {
        this.speed = Math.max(1, Math.min(11, speed));
        return this;
    }
    
    public PngCompressor setMinPosterization(int bits) {
        this.minPosterization = Math.max(0, Math.min(4, bits));
        return this;
    }
    
    /**
     * 压缩PNG图像
     * @param inputFile 输入文件路径
     * @param outputFile 输出文件路径
     * @return 压缩结果信息
     */
    public CompressionResult compress(String inputFile, String outputFile) {
        return compress(new File(inputFile), new File(outputFile));
    }
    
    /**
     * 压缩PNG图像
     * @param inputFile 输入文件
     * @param outputFile 输出文件
     * @return 压缩结果信息
     */
    public CompressionResult compress(File inputFile, File outputFile) {
        long attr = 0;
        long image = 0;
        long result = 0;
        
        try {
            // 1. 读取输入图像
            BufferedImage originalImage = ImageIO.read(inputFile);
            if (originalImage == null) {
                return new CompressionResult(false, "无法读取输入图像: " + inputFile.getPath());
            }
            
            // 2. 转换为RGBA格式 (使用TYPE_4BYTE_ABGR，已修复字节顺序问题)
            BufferedImage rgbaImage = convertToRGBA(originalImage);
            
            // 3. 提取像素数据
            DataBufferByte buffer = (DataBufferByte) rgbaImage.getRaster().getDataBuffer();
            byte[] imageData = buffer.getData();
            
            // 4. 创建libimagequant属性
            attr = lib.jna_liq_attr_create();
            if (attr == 0) {
                return new CompressionResult(false, "无法创建量化属性");
            }
            
            // 5. 设置参数
            lib.jna_setMaxColors(attr, maxColors);
            if (qualityMin == qualityMax) {
                lib.jna_setQuality_single(attr, qualityMin);
            } else {
                lib.jna_setQuality_range(attr, qualityMin, qualityMax);
            }
            lib.jna_setSpeed(attr, speed);
            lib.jna_setMinPosterization(attr, minPosterization);
            
            // 6. 创建图像对象
            image = lib.jna_liq_image_create(attr, imageData, rgbaImage.getWidth(), rgbaImage.getHeight(), 4);
            if (image == 0) {
                return new CompressionResult(false, "无法创建图像对象");
            }
            
            // 7. 量化图像
            result = lib.jna_liq_quantize_image(attr, image);
            if (result == 0) {
                return new CompressionResult(false, "图像量化失败");
            }
            
            // 8. 获取调色板
            Pointer palette = lib.jna_liq_get_palette(result);
            int paletteSize = lib.jna_get_palette_count(palette);
            byte[] paletteData = new byte[paletteSize * 4];
            lib.jna_copy_palette_data(palette, paletteData, paletteData.length);
            
            // 9. 创建索引颜色模型
            byte[] red = new byte[paletteSize];
            byte[] green = new byte[paletteSize];
            byte[] blue = new byte[paletteSize];
            byte[] alpha = new byte[paletteSize];
            
            for (int i = 0; i < paletteSize; i++) {
                red[i] = paletteData[i * 4];
                green[i] = paletteData[i * 4 + 1];
                blue[i] = paletteData[i * 4 + 2];
                alpha[i] = paletteData[i * 4 + 3];
            }
            
            IndexColorModel colorModel = new IndexColorModel(8, paletteSize, red, green, blue, alpha);
            
            // 10. 创建输出图像
            BufferedImage outputImage = new BufferedImage(rgbaImage.getWidth(), rgbaImage.getHeight(), 
                                                         BufferedImage.TYPE_BYTE_INDEXED, colorModel);
            DataBufferByte outputBuffer = (DataBufferByte) outputImage.getRaster().getDataBuffer();
            byte[] indexData = outputBuffer.getData();
            
            // 11. 重映射像素
            lib.jna_liq_write_remapped_image(result, image, indexData, indexData.length);
            
            // 12. 保存图像
            ImageIO.write(outputImage, "PNG", outputFile);
            
            // 13. 计算压缩统计
            long inputSize = inputFile.length();
            long outputSize = outputFile.length();
            double compressionRatio = 1.0 - (double) outputSize / inputSize;
            double mse = lib.jna_getMeanSquareError(result);
            int quality = lib.jna_getQuality(result);
            
            return new CompressionResult(true, inputSize, outputSize, compressionRatio, 
                                       paletteSize, quality, mse);
            
        } catch (IOException e) {
            return new CompressionResult(false, "IO错误: " + e.getMessage());
        } catch (Exception e) {
            return new CompressionResult(false, "压缩错误: " + e.getMessage());
        } finally {
            // 清理资源
            if (result != 0) lib.jna_liq_result_destroy(result);
            if (image != 0) lib.jna_liq_image_destroy(image);
            if (attr != 0) lib.jna_liq_attr_destroy(attr);
        }
    }
    
    /**
     * 将图像转换为RGBA格式
     */
    private BufferedImage convertToRGBA(BufferedImage originalImage) {
        BufferedImage rgbaImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), 
                                                   BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = rgbaImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(originalImage, 0, 0, null);
        g.dispose();
        return rgbaImage;
    }
    
    /**
     * 压缩结果类
     */
    public static class CompressionResult {
        private final boolean success;
        private final String errorMessage;
        private final long inputSize;
        private final long outputSize;
        private final double compressionRatio;
        private final int colorsUsed;
        private final int quality;
        private final double mse;
        
        // 失败结果构造函数
        private CompressionResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.inputSize = 0;
            this.outputSize = 0;
            this.compressionRatio = 0;
            this.colorsUsed = 0;
            this.quality = 0;
            this.mse = 0;
        }
        
        // 成功结果构造函数
        private CompressionResult(boolean success, long inputSize, long outputSize, 
                                double compressionRatio, int colorsUsed, int quality, double mse) {
            this.success = success;
            this.errorMessage = null;
            this.inputSize = inputSize;
            this.outputSize = outputSize;
            this.compressionRatio = compressionRatio;
            this.colorsUsed = colorsUsed;
            this.quality = quality;
            this.mse = mse;
        }
        
        // Getter方法
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public long getInputSize() { return inputSize; }
        public long getOutputSize() { return outputSize; }
        public double getCompressionRatio() { return compressionRatio; }
        public int getColorsUsed() { return colorsUsed; }
        public int getQuality() { return quality; }
        public double getMse() { return mse; }
        
        @Override
        public String toString() {
            if (!success) {
                return "压缩失败: " + errorMessage;
            }
            return String.format("压缩成功 - 大小: %,d → %,d bytes (%.1f%%), 颜色: %d, 质量: %d, MSE: %.3f",
                               inputSize, outputSize, compressionRatio * 100, colorsUsed, quality, mse);
        }
    }
    
    /**
     * 主方法 - 示例用法
     */
    public static void main(String[] args) {
        // 创建压缩器实例
        PngCompressor compressor = new PngCompressor()
            .setMaxColors(256)      // 最大256色
            .setQuality(80)         // 质量80
            .setSpeed(3);           // 速度3 (平衡模式)
        
        // 压缩图像
        CompressionResult result = compressor.compress("test.png", "test_compressed.png");
        
        // 输出结果
        System.out.println("=== PNG压缩工具 ===");
        System.out.println("libimagequant版本: " + LibImageQuantJNA.INSTANCE.jna_liq_version());
        System.out.println(result);
        
        if (result.isSuccess()) {
            System.out.println("✅ 压缩完成！");
        } else {
            System.err.println("❌ 压缩失败！");
        }
        
        // 示例：不同参数的压缩
        System.out.println("\n=== 高质量压缩 ===");
        CompressionResult highQuality = new PngCompressor()
            .setMaxColors(256)
            .setQuality(95)
            .setSpeed(1)    // 最慢但质量最好
            .compress("test.png", "test_high_quality.png");
        System.out.println(highQuality);
        
        System.out.println("\n=== 快速压缩 ===");
        CompressionResult fastCompress = new PngCompressor()
            .setMaxColors(128)  // 减少颜色数
            .setQuality(80)     // 降低质量
            .setSpeed(3)       // 最快速度
            .compress("test.png", "test_fast.png");
        System.out.println(fastCompress);
    }
}

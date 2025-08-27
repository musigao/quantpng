package org.pngquant.jna.test;

import org.pngquant.jna.PngQuantJNA;
import org.pngquant.jna.LibImageQuantJNA;

import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.DataBufferByte;

/**
 * JNA包装器的Java测试示例
 */
public class JNAWrapperTest {
    
    public static void main(String[] args) {
        System.out.println("Testing PngQuant JNA Wrapper...\n");
        
        try {
            // 测试直接JNA接口
            testDirectJNA();
            
            System.out.println("\n" + "=".repeat(50) + "\n");
            
            // 测试高级包装类
            testHighLevelWrapper();
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试直接JNA接口
     */
    private static void testDirectJNA() {
        System.out.println("=== Testing Direct JNA Interface ===");
        
        LibImageQuantJNA lib = LibImageQuantJNA.INSTANCE;
        
        // 测试版本
        System.out.println("Library version: " + lib.jna_liq_version());
        
        long attr = 0;
        long image = 0;
        long result = 0;
        
        try {
            // 创建属性对象
            attr = lib.jna_liq_attr_create();
            System.out.println("Attribute created: " + attr);
            
            // 设置参数
            System.out.println("Setting max colors: " + 
                (lib.jna_setMaxColors(attr, 32) == LibImageQuantJNA.LIQ_JNA_OK ? "OK" : "FAIL"));
            System.out.println("Setting quality: " + 
                (lib.jna_setQuality_range(attr, 80, 95) == LibImageQuantJNA.LIQ_JNA_OK ? "OK" : "FAIL"));
            System.out.println("Setting speed: " + 
                (lib.jna_setSpeed(attr, 3) == LibImageQuantJNA.LIQ_JNA_OK ? "OK" : "FAIL"));
            
            // 创建测试图像数据 (4x4 RGBA)
            byte[] imageData = createTestImageData(4, 4);
            image = lib.jna_liq_image_create(attr, imageData, 4, 4, 4);
            System.out.println("Image created: " + image);
            System.out.println("Image size: " + lib.jna_getWidth(image) + "x" + lib.jna_getHeight(image));
            
            // 量化
            result = lib.jna_liq_quantize_image(attr, image);
            System.out.println("Quantization result: " + result);
            
            if (result != 0) {
                // 获取质量信息
                System.out.println("Quality: " + lib.jna_getQuality(result));
                System.out.println("MSE: " + lib.jna_getMeanSquareError(result));
                
                // 测试重映射
                byte[] outputBuffer = new byte[16]; // 4x4 pixels
                int writeResult = lib.jna_liq_write_remapped_image(result, image, outputBuffer, outputBuffer.length);
                System.out.println("Write remapped: " + (writeResult == LibImageQuantJNA.LIQ_JNA_OK ? "OK" : "FAIL"));
            }
            
        } finally {
            // 清理资源
            if (result != 0) lib.jna_liq_result_destroy(result);
            if (image != 0) lib.jna_liq_image_destroy(image);
            if (attr != 0) lib.jna_liq_attr_destroy(attr);
            System.out.println("Resources cleaned up");
        }
    }
    
    /**
     * 测试高级包装类
     */
    private static void testHighLevelWrapper() {
        System.out.println("=== Testing High-Level Wrapper ===");
        
        // 创建测试图像
        BufferedImage testImage = createTestBufferedImage(8, 8);
        
        try (PngQuantJNA pngQuant = new PngQuantJNA()) {
            // 设置参数
            System.out.println("Setting max colors: " + pngQuant.setMaxColors(64));
            System.out.println("Setting quality: " + pngQuant.setQuality(70, 90));
            System.out.println("Setting speed: " + pngQuant.setSpeed(3));
            
            // 进行量化和重映射
            BufferedImage result = pngQuant.getRemapped(testImage);
            
            if (result != null) {
                System.out.println("Quantization successful!");
                System.out.println("Original: " + testImage.getWidth() + "x" + testImage.getHeight() + 
                    " (" + testImage.getType() + ")");
                System.out.println("Result: " + result.getWidth() + "x" + result.getHeight() + 
                    " (" + result.getType() + ")");
                
                // 获取调色板信息
                if (result.getColorModel() != null) {
                    System.out.println("Palette size: " + result.getColorModel().getMapSize());
                }
                
            } else {
                System.out.println("Quantization failed");
            }
            
            // 测试分步操作
            System.out.println("\n--- Testing step-by-step process ---");
            try (PngQuantJNA.ImageJNA image = new PngQuantJNA.ImageJNA(pngQuant, testImage)) {
                System.out.println("Image created: " + image.getWidth() + "x" + image.getHeight());
                
                // 添加固定颜色
                System.out.println("Adding fixed colors: " + 
                    image.addFixedColor(255, 255, 255)); // 白色
                
                try (PngQuantJNA.ResultJNA quantResult = pngQuant.quantize(image)) {
                    if (quantResult != null) {
                        System.out.println("Quantization successful");
                        System.out.println("Quality: " + quantResult.getQuality());
                        System.out.println("MSE: " + quantResult.getMeanSquareError());
                        
                        // 设置抖动
                        System.out.println("Setting dithering: " + 
                            quantResult.setDitheringLevel(0.8f));
                        
                        // 获取最终图像
                        BufferedImage finalImage = quantResult.getRemapped(image);
                        System.out.println("Final image: " + (finalImage != null ? "OK" : "FAIL"));
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("High-level test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建测试用的RGBA字节数组
     */
    private static byte[] createTestImageData(int width, int height) {
        byte[] data = new byte[width * height * 4];
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = (y * width + x) * 4;
                
                // 创建彩色渐变
                data[index] = (byte)(255 * x / width);     // R
                data[index + 1] = (byte)(255 * y / height); // G
                data[index + 2] = (byte)(128);              // B
                data[index + 3] = (byte)(255);              // A
            }
        }
        
        return data;
    }
    
    /**
     * 创建测试用的BufferedImage
     */
    private static BufferedImage createTestBufferedImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2d = image.createGraphics();
        
        // 创建彩色图案
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float r = (float)x / width;
                float g = (float)y / height;
                float b = 0.5f;
                g2d.setColor(new Color(r, g, b));
                g2d.fillRect(x, y, 1, 1);
            }
        }
        
        g2d.dispose();
        return image;
    }
}

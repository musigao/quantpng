package org.pngquant.jna;

import com.sun.jna.Pointer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.File;

/**
 * 详细分析PNG压缩问题
 */
public class CompressionAnalyzer {
    
    public static void main(String[] args) {
        LibImageQuantJNA lib = LibImageQuantJNA.INSTANCE;
        
        try {
            // 1. 读取原始图像
            BufferedImage original = ImageIO.read(new File("test.png"));
            System.out.printf("原始图像: %dx%d, 类型=%d\n", original.getWidth(), original.getHeight(), original.getType());
            
            // 2. 创建一个小的测试图像 (10x10) 来分析
            BufferedImage testImg = original.getSubimage(100, 100, 10, 10);
            
            // 3. 分析原始子图像的颜色
            System.out.println("\n=== 原始子图像颜色分析 ===");
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    int rgb = testImg.getRGB(x, y);
                    int a = (rgb >> 24) & 0xFF;
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    System.out.printf("(%d,%d): ARGB=(%3d,%3d,%3d,%3d) ", x, y, a, r, g, b);
                }
                System.out.println();
            }
            
            // 4. 转换为RGBA格式
            BufferedImage rgba = new BufferedImage(testImg.getWidth(), testImg.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D g = rgba.createGraphics();
            g.drawImage(testImg, 0, 0, null);
            g.dispose();
            
            // 5. 分析字节数据
            DataBufferByte buffer = (DataBufferByte) rgba.getRaster().getDataBuffer();
            byte[] imageData = buffer.getData();
            
            System.out.println("\n=== 字节数据分析 (ABGR顺序) ===");
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    int idx = 4 * (testImg.getWidth() * y + x);
                    int a2 = imageData[idx + 0] & 0xFF;
                    int b2 = imageData[idx + 1] & 0xFF;
                    int g2 = imageData[idx + 2] & 0xFF;
                    int r2 = imageData[idx + 3] & 0xFF;
                    System.out.printf("(%d,%d): ABGR=(%3d,%3d,%3d,%3d) ", x, y, a2, b2, g2, r2);
                }
                System.out.println();
            }
            
            // 6. 测试压缩
            System.out.println("\n=== 压缩测试 ===");
            
            long attr = lib.jna_liq_attr_create();
            lib.jna_setMaxColors(attr, 16);  // 少数颜色便于分析
            lib.jna_setQuality_single(attr, 80);
            lib.jna_setSpeed(attr, 3);
            lib.jna_setMinPosterization(attr, 0);
            
            long image = lib.jna_liq_image_create(attr, imageData, testImg.getWidth(), testImg.getHeight(), 4);
            long result = lib.jna_liq_quantize_image(attr, image);
            
            if (result != 0) {
                // 获取调色板
                Pointer palette = lib.jna_liq_get_palette(result);
                int paletteSize = lib.jna_get_palette_count(palette);
                byte[] paletteData = new byte[paletteSize * 4];
                lib.jna_copy_palette_data(palette, paletteData, paletteData.length);
                
                System.out.printf("调色板 (%d 颜色):\n", paletteSize);
                for (int i = 0; i < paletteSize; i++) {
                    int r3 = paletteData[i * 4 + 0] & 0xFF;
                    int g3 = paletteData[i * 4 + 1] & 0xFF;
                    int b3 = paletteData[i * 4 + 2] & 0xFF;
                    int a3 = paletteData[i * 4 + 3] & 0xFF;
                    System.out.printf("颜色%2d: RGBA=(%3d,%3d,%3d,%3d)\n", i, r3, g3, b3, a3);
                }
                
                // 获取重映射的索引
                byte[] indexData = new byte[testImg.getWidth() * testImg.getHeight()];
                lib.jna_liq_write_remapped_image(result, image, indexData, indexData.length);
                
                System.out.println("\n映射索引:");
                for (int y = 0; y < 3; y++) {
                    for (int x = 0; x < 3; x++) {
                        int idx = testImg.getWidth() * y + x;
                        int colorIdx = indexData[idx] & 0xFF;
                        System.out.printf("(%d,%d): 索引=%3d ", x, y, colorIdx);
                    }
                    System.out.println();
                }
                
                // 7. 创建输出图像
                byte[] red = new byte[paletteSize];
                byte[] green = new byte[paletteSize]; 
                byte[] blue = new byte[paletteSize];
                byte[] alpha = new byte[paletteSize];
                
                for (int i = 0; i < paletteSize; i++) {
                    red[i] = paletteData[i * 4 + 0];
                    green[i] = paletteData[i * 4 + 1];
                    blue[i] = paletteData[i * 4 + 2];
                    alpha[i] = paletteData[i * 4 + 3];
                }
                
                IndexColorModel colorModel = new IndexColorModel(8, paletteSize, red, green, blue, alpha);
                BufferedImage output = new BufferedImage(testImg.getWidth(), testImg.getHeight(), BufferedImage.TYPE_BYTE_INDEXED, colorModel);
                DataBufferByte outputBuffer = (DataBufferByte) output.getRaster().getDataBuffer();
                byte[] outputIndexData = outputBuffer.getData();
                System.arraycopy(indexData, 0, outputIndexData, 0, indexData.length);
                
                // 保存测试图像
                ImageIO.write(testImg, "PNG", new File("test_original_10x10.png"));
                ImageIO.write(output, "PNG", new File("test_compressed_10x10.png"));
                
                System.out.println("\n分析完成！保存了 test_original_10x10.png 和 test_compressed_10x10.png");
                
                // 8. 检查输出图像的实际颜色
                System.out.println("\n=== 输出图像颜色验证 ===");
                for (int y = 0; y < 3; y++) {
                    for (int x = 0; x < 3; x++) {
                        int rgb = output.getRGB(x, y);
                        int a4 = (rgb >> 24) & 0xFF;
                        int r4 = (rgb >> 16) & 0xFF;
                        int g4 = (rgb >> 8) & 0xFF;
                        int b4 = rgb & 0xFF;
                        System.out.printf("(%d,%d): ARGB=(%3d,%3d,%3d,%3d) ", x, y, a4, r4, g4, b4);
                    }
                    System.out.println();
                }
                
                lib.jna_liq_result_destroy(result);
            } else {
                System.out.println("量化失败");
            }
            
            lib.jna_liq_image_destroy(image);
            lib.jna_liq_attr_destroy(attr);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

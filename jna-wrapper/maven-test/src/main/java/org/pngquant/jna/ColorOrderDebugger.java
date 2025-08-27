package org.pngquant.jna;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.File;

/**
 * 调试颜色顺序问题
 */
public class ColorOrderDebugger {
    
    public static void main(String[] args) {
        try {
            // 1. 读取原始图像
            BufferedImage original = ImageIO.read(new File("test.png"));
            System.out.printf("原始图像: %dx%d, 类型=%d\n", original.getWidth(), original.getHeight(), original.getType());
            System.out.printf("ColorModel: %s\n", original.getColorModel().getClass().getSimpleName());
            System.out.printf("SampleModel: %s\n", original.getSampleModel().getClass().getSimpleName());
            
            // 2. 检查第一个像素的颜色
            int rgb = original.getRGB(0, 0);
            int alpha = (rgb >> 24) & 0xFF;
            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = rgb & 0xFF;
            System.out.printf("第一个像素 RGB: (%d, %d, %d, %d)\n", red, green, blue, alpha);
            
            // 3. 转换为不同格式并检查
            System.out.println("\n=== 测试不同的图像格式转换 ===");
            
            // TYPE_4BYTE_ABGR (最常用的格式)
            testFormat(original, BufferedImage.TYPE_4BYTE_ABGR, "TYPE_4BYTE_ABGR");
            
            // TYPE_INT_ARGB
            testFormat(original, BufferedImage.TYPE_INT_ARGB, "TYPE_INT_ARGB");
            
            // TYPE_3BYTE_BGR
            testFormat(original, BufferedImage.TYPE_3BYTE_BGR, "TYPE_3BYTE_BGR");
            
            // TYPE_INT_RGB
            testFormat(original, BufferedImage.TYPE_INT_RGB, "TYPE_INT_RGB");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void testFormat(BufferedImage original, int imageType, String typeName) {
        try {
            // 转换图像格式
            BufferedImage converted = new BufferedImage(original.getWidth(), original.getHeight(), imageType);
            Graphics2D g = converted.createGraphics();
            g.drawImage(original, 0, 0, null);
            g.dispose();
            
            // 获取像素数据
            DataBuffer buffer = converted.getRaster().getDataBuffer();
            if (buffer instanceof DataBufferByte) {
                byte[] data = ((DataBufferByte) buffer).getData();
                int components = converted.getColorModel().getNumComponents();
                
                System.out.printf("\n%s: %d通道\n", typeName, components);
                System.out.printf("前8个字节: ");
                for (int i = 0; i < Math.min(8, data.length); i++) {
                    System.out.printf("%02X ", data[i] & 0xFF);
                }
                System.out.println();
                
                // 检查第一个像素
                if (data.length >= components) {
                    System.out.printf("第一个像素字节: ");
                    for (int i = 0; i < components; i++) {
                        System.out.printf("%02X ", data[i] & 0xFF);
                    }
                    System.out.println();
                }
                
                // 保存测试图像
                String filename = "debug_" + typeName.toLowerCase() + ".png";
                ImageIO.write(converted, "PNG", new File(filename));
                System.out.printf("保存为: %s\n", filename);
                
            } else {
                System.out.printf("%s: 不是DataBufferByte类型\n", typeName);
            }
            
        } catch (Exception e) {
            System.out.printf("%s: 转换失败 - %s\n", typeName, e.getMessage());
        }
    }
}

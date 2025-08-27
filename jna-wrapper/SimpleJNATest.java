import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * 简化的JNA接口测试
 */
interface LibImageQuantJNA extends Library {
    LibImageQuantJNA INSTANCE = Native.load("imagequant_jna", LibImageQuantJNA.class);
    
    // 错误码常量
    int LIQ_JNA_OK = 0;
    int LIQ_JNA_ERROR = 1;
    
    // 基本函数
    long jna_liq_attr_create();
    void jna_liq_attr_destroy(long handle);
    int jna_setMaxColors(long handle, int colors);
    int jna_setQuality_range(long handle, int min, int max);
    int jna_setSpeed(long handle, int speed);
    
    long jna_liq_image_create(long attr, byte[] bitmap, int width, int height, int components);
    void jna_liq_image_destroy(long handle);
    int jna_getWidth(long handle);
    int jna_getHeight(long handle);
    
    long jna_liq_quantize_image(long attr, long image_handle);
    Pointer jna_liq_get_palette(long result_handle);
    int jna_get_palette_count(Pointer palette);
    int jna_copy_palette_data(Pointer palette, byte[] buffer, int buffer_size);
    void jna_liq_result_destroy(long handle);
    
    int jna_liq_version();
}

/**
 * JNA包装器测试类
 */
public class SimpleJNATest {
    
    public static void main(String[] args) {
        System.out.println("=== Simple JNA Test ===");
        
        try {
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
                int setColorsResult = lib.jna_setMaxColors(attr, 32);
                System.out.println("Set max colors: " + (setColorsResult == LibImageQuantJNA.LIQ_JNA_OK ? "OK" : "FAIL"));
                
                int setQualityResult = lib.jna_setQuality_range(attr, 80, 95);
                System.out.println("Set quality: " + (setQualityResult == LibImageQuantJNA.LIQ_JNA_OK ? "OK" : "FAIL"));
                
                int setSpeedResult = lib.jna_setSpeed(attr, 3);
                System.out.println("Set speed: " + (setSpeedResult == LibImageQuantJNA.LIQ_JNA_OK ? "OK" : "FAIL"));
                
                // 创建测试图像数据 (4x4 RGBA)
                byte[] imageData = createTestImageData(4, 4);
                image = lib.jna_liq_image_create(attr, imageData, 4, 4, 4);
                System.out.println("Image created: " + image);
                
                if (image != 0) {
                    System.out.println("Image size: " + lib.jna_getWidth(image) + "x" + lib.jna_getHeight(image));
                    
                    // 量化
                    result = lib.jna_liq_quantize_image(attr, image);
                    System.out.println("Quantization result: " + result);
                    
                    if (result != 0) {
                        // 获取调色板
                        Pointer palette = lib.jna_liq_get_palette(result);
                        System.out.println("Palette pointer: " + palette);
                        
                        if (palette != null) {
                            int paletteCount = lib.jna_get_palette_count(palette);
                            System.out.println("Palette colors: " + paletteCount);
                            
                            // 获取调色板数据
                            byte[] paletteData = new byte[paletteCount * 4];
                            int copyResult = lib.jna_copy_palette_data(palette, paletteData, paletteData.length);
                            System.out.println("Copy palette data: " + (copyResult == LibImageQuantJNA.LIQ_JNA_OK ? "OK" : "FAIL"));
                            
                            // 显示前几个颜色
                            System.out.println("First few colors (RGBA):");
                            for (int i = 0; i < Math.min(4, paletteCount); i++) {
                                int idx = i * 4;
                                System.out.printf("  Color %d: (%d, %d, %d, %d)%n", i,
                                    paletteData[idx] & 0xFF, paletteData[idx + 1] & 0xFF,
                                    paletteData[idx + 2] & 0xFF, paletteData[idx + 3] & 0xFF);
                            }
                        }
                    }
                }
                
                System.out.println("\nTest completed successfully!");
                
            } finally {
                // 清理资源
                if (result != 0) {
                    lib.jna_liq_result_destroy(result);
                    System.out.println("Result destroyed");
                }
                if (image != 0) {
                    lib.jna_liq_image_destroy(image);
                    System.out.println("Image destroyed");
                }
                if (attr != 0) {
                    lib.jna_liq_attr_destroy(attr);
                    System.out.println("Attribute destroyed");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
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
}

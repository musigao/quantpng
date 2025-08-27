#include "jna_wrapper.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// 简单的测试用图像数据 (2x2 RGBA)
static unsigned char test_image_data[] = {
    255, 0, 0, 255,    // 红色
    0, 255, 0, 255,    // 绿色
    0, 0, 255, 255,    // 蓝色
    255, 255, 0, 255   // 黄色
};

int main() {
    printf("Testing JNA wrapper for libimagequant...\n");
    
    // 测试版本
    printf("Library version: %d\n", jna_liq_version());
    
    // 测试基本工作流程
    printf("\n1. Creating attribute object...\n");
    long attr = jna_liq_attr_create();
    if (attr == 0) {
        printf("Failed to create attribute object\n");
        return 1;
    }
    printf("Attribute created: %ld\n", attr);
    
    printf("\n2. Setting parameters...\n");
    if (jna_setMaxColors(attr, 16) != LIQ_JNA_OK) {
        printf("Failed to set max colors\n");
        goto cleanup_attr;
    }
    printf("Max colors set to 16\n");
    
    if (jna_setQuality_range(attr, 70, 99) != LIQ_JNA_OK) {
        printf("Failed to set quality\n");
        goto cleanup_attr;
    }
    printf("Quality set to 70-99\n");
    
    if (jna_setSpeed(attr, 3) != LIQ_JNA_OK) {
        printf("Failed to set speed\n");
        goto cleanup_attr;
    }
    printf("Speed set to 3\n");
    
    printf("\n3. Creating image...\n");
    long image = jna_liq_image_create(attr, test_image_data, 2, 2, 4);
    if (image == 0) {
        printf("Failed to create image\n");
        goto cleanup_attr;
    }
    printf("Image created: %ld\n", image);
    
    printf("Image dimensions: %dx%d\n", 
           jna_getWidth(image), 
           jna_getHeight(image));
    
    printf("\n4. Adding fixed color...\n");
    if (jna_addFixedColor(image, 255, 255, 255, 255) != LIQ_JNA_OK) {
        printf("Failed to add fixed color\n");
    } else {
        printf("Fixed color (white) added\n");
    }
    
    printf("\n5. Quantizing image...\n");
    long result = jna_liq_quantize_image(attr, image);
    if (result == 0) {
        printf("Failed to quantize image\n");
        goto cleanup_image;
    }
    printf("Quantization result: %ld\n", result);
    
    printf("\n6. Getting palette...\n");
    const void* palette = jna_liq_get_palette(result);
    if (palette == NULL) {
        printf("Failed to get palette\n");
        goto cleanup_result;
    }
    
    int palette_count = jna_get_palette_count(palette);
    printf("Palette has %d colors\n", palette_count);
    
    unsigned char palette_data[256 * 4];
    if (jna_copy_palette_data(palette, palette_data, sizeof(palette_data)) == LIQ_JNA_OK) {
        printf("First 4 palette colors (RGBA):\n");
        for (int i = 0; i < 4 && i < palette_count; i++) {
            printf("  Color %d: (%d, %d, %d, %d)\n", i,
                   palette_data[i*4], palette_data[i*4+1], 
                   palette_data[i*4+2], palette_data[i*4+3]);
        }
    }
    
    printf("\n7. Setting result parameters...\n");
    if (jna_setDitheringLevel(result, 0.5f) == LIQ_JNA_OK) {
        printf("Dithering level set to 0.5\n");
    }
    
    if (jna_setGamma(result, 2.2) == LIQ_JNA_OK) {
        printf("Gamma set to 2.2\n");
        printf("Current gamma: %f\n", jna_getGamma(result));
    }
    
    printf("\n8. Getting quality metrics...\n");
    double mse = jna_getMeanSquareError(result);
    int quality = jna_getQuality(result);
    printf("Mean Square Error: %f\n", mse);
    printf("Quality: %d\n", quality);
    
    printf("\n9. Writing remapped image...\n");
    unsigned char output_buffer[4]; // 2x2 = 4 pixels
    if (jna_liq_write_remapped_image(result, image, output_buffer, sizeof(output_buffer)) == LIQ_JNA_OK) {
        printf("Remapped image data: ");
        for (int i = 0; i < 4; i++) {
            printf("%d ", output_buffer[i]);
        }
        printf("\n");
    } else {
        printf("Failed to write remapped image\n");
    }
    
    printf("\n10. Testing handle validation...\n");
    printf("Valid attr handle: %s\n", 
           jna_is_valid_handle(attr) == LIQ_JNA_OK ? "YES" : "NO");
    printf("Invalid handle (0): %s\n", 
           jna_is_valid_handle(0) == LIQ_JNA_OK ? "YES" : "NO");
    
    printf("\nAll tests completed successfully!\n");
    
    // 清理资源
cleanup_result:
    jna_liq_result_destroy(result);
    printf("Result destroyed\n");

cleanup_image:
    jna_liq_image_destroy(image);
    printf("Image destroyed\n");

cleanup_attr:
    jna_liq_attr_destroy(attr);
    printf("Attribute destroyed\n");
    
    return 0;
}

package org.pngquant.jna;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 多线程PNG压缩工具
 * 同时处理多个PNG文件，提高压缩效率
 */
public class MultiThreadPngCompressor {
    
    private final PngCompressor compressor;
    private final ExecutorService executorService;
    private final int threadCount;
    
    // 统计信息
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong totalOriginalSize = new AtomicLong(0);
    private final AtomicLong totalCompressedSize = new AtomicLong(0);
    
    /**
     * 压缩任务结果
     */
    public static class CompressionResult {
        public final String inputFile;
        public final String outputFile;
        public final boolean success;
        public final long originalSize;
        public final long compressedSize;
        public final double compressionRatio;
        public final long processingTime;
        public final String errorMessage;
        
        public CompressionResult(String inputFile, String outputFile, boolean success,
                               long originalSize, long compressedSize, long processingTime,
                               String errorMessage) {
            this.inputFile = inputFile;
            this.outputFile = outputFile;
            this.success = success;
            this.originalSize = originalSize;
            this.compressedSize = compressedSize;
            this.compressionRatio = success ? (1.0 - (double)compressedSize / originalSize) : 0.0;
            this.processingTime = processingTime;
            this.errorMessage = errorMessage;
        }
    }
    
    /**
     * 压缩任务
     */
    private class CompressionTask implements Callable<CompressionResult> {
        private final String inputFile;
        private final String outputFile;
        private final int quality;
        private final int maxColors;
        private final int speed;
        
        public CompressionTask(String inputFile, String outputFile, int quality, int maxColors, int speed) {
            this.inputFile = inputFile;
            this.outputFile = outputFile;
            this.quality = quality;
            this.maxColors = maxColors;
            this.speed = speed;
        }
        
        @Override
        public CompressionResult call() {
            long startTime = System.currentTimeMillis();
            String threadName = Thread.currentThread().getName();
            
            try {
                System.out.printf("[%s] 🚀 开始压缩: %s\n", threadName, inputFile);
                
                // 获取原始文件大小
                File original = new File(inputFile);
                if (!original.exists()) {
                    String error = "输入文件不存在: " + inputFile;
                    System.err.printf("[%s] ❌ %s\n", threadName, error);
                    failureCount.incrementAndGet();
                    return new CompressionResult(inputFile, outputFile, false, 0, 0, 
                                               System.currentTimeMillis() - startTime, error);
                }
                
                long originalSize = original.length();
                totalOriginalSize.addAndGet(originalSize);
                
                // 执行压缩
                PngCompressor.CompressionResult result = compressor
                    .setMaxColors(maxColors)
                    .setQuality(quality)
                    .setSpeed(speed)
                    .compress(inputFile, outputFile);
                
                if (result.isSuccess()) {
                    long compressedSize = result.getOutputSize();
                    totalCompressedSize.addAndGet(compressedSize);
                    
                    long processingTime = System.currentTimeMillis() - startTime;
                    double ratio = result.getCompressionRatio() * 100;
                    
                    System.out.printf("[%s] ✅ 压缩成功: %s → %s (%.1f%%, %dms)\n", 
                                     threadName, inputFile, outputFile, ratio, processingTime);
                    
                    successCount.incrementAndGet();
                    return new CompressionResult(inputFile, outputFile, true, originalSize, 
                                               compressedSize, processingTime, null);
                } else {
                    String error = result.getErrorMessage() != null ? result.getErrorMessage() : "压缩失败";
                    System.err.printf("[%s] ❌ 压缩失败: %s - %s\n", threadName, inputFile, error);
                    failureCount.incrementAndGet();
                    return new CompressionResult(inputFile, outputFile, false, originalSize, 0,
                                               System.currentTimeMillis() - startTime, error);
                }
                
            } catch (Exception e) {
                String error = "处理异常: " + e.getMessage();
                System.err.printf("[%s] ❌ 异常: %s - %s\n", threadName, inputFile, error);
                failureCount.incrementAndGet();
                return new CompressionResult(inputFile, outputFile, false, 0, 0,
                                           System.currentTimeMillis() - startTime, error);
            }
        }
    }
    
    /**
     * 构造函数
     * @param threadCount 线程数量，建议使用CPU核心数
     */
    public MultiThreadPngCompressor(int threadCount) {
        this.threadCount = threadCount;
        this.compressor = new PngCompressor();
        this.executorService = Executors.newFixedThreadPool(threadCount, r -> {
            Thread t = new Thread(r);
            t.setName("PNG-Compressor-" + System.nanoTime());
            return t;
        });
        
        System.out.printf("🔧 多线程PNG压缩器已初始化 (线程数: %d)\n", threadCount);
    }
    
    /**
     * 默认构造函数，使用CPU核心数作为线程数
     */
    public MultiThreadPngCompressor() {
        this(Runtime.getRuntime().availableProcessors());
    }
    
    /**
     * 批量压缩文件
     * @param tasks 压缩任务列表 (输入文件, 输出文件, 质量, 最大颜色数, 速度)
     * @return 压缩结果列表
     */
    public List<CompressionResult> compressBatch(List<CompressionTask> tasks) {
        if (tasks.isEmpty()) {
            System.out.println("⚠️  没有压缩任务");
            return new ArrayList<>();
        }
        
        System.out.printf("📋 开始批量压缩 %d 个文件...\n", tasks.size());
        long startTime = System.currentTimeMillis();
        
        try {
            // 提交所有任务
            List<Future<CompressionResult>> futures = executorService.invokeAll(tasks);
            
            // 收集结果
            List<CompressionResult> results = new ArrayList<>();
            for (Future<CompressionResult> future : futures) {
                try {
                    results.add(future.get());
                } catch (ExecutionException e) {
                    System.err.println("❌ 任务执行异常: " + e.getMessage());
                }
            }
            
            long totalTime = System.currentTimeMillis() - startTime;
            printSummary(results, totalTime);
            
            return results;
            
        } catch (InterruptedException e) {
            System.err.println("❌ 批量压缩被中断: " + e.getMessage());
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        }
    }
    
    /**
     * 便捷方法：压缩指定的文件
     * @param inputFiles 输入文件列表
     * @param outputPrefix 输出文件前缀
     * @param quality 质量 (0-100)
     * @param maxColors 最大颜色数 (1-256)
     * @param speed 速度 (1-11)
     */
    public List<CompressionResult> compressFiles(String[] inputFiles, String outputPrefix, 
                                                int quality, int maxColors, int speed) {
        List<CompressionTask> tasks = new ArrayList<>();
        
        for (String inputFile : inputFiles) {
            String fileName = new File(inputFile).getName();
            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
            String outputFile = outputPrefix + baseName + "_compressed.png";
            
            tasks.add(new CompressionTask(inputFile, outputFile, quality, maxColors, speed));
        }
        
        return compressBatch(tasks);
    }
    
    /**
     * 打印统计摘要
     */
    private void printSummary(List<CompressionResult> results, long totalTime) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📊 压缩统计摘要");
        System.out.println("=".repeat(60));
        
        System.out.printf("⏱️  总耗时: %d ms (%.2f 秒)\n", totalTime, totalTime / 1000.0);
        System.out.printf("✅ 成功: %d 个文件\n", successCount.get());
        System.out.printf("❌ 失败: %d 个文件\n", failureCount.get());
        System.out.printf("🧵 使用线程: %d 个\n", threadCount);
        
        if (successCount.get() > 0) {
            long originalTotal = totalOriginalSize.get();
            long compressedTotal = totalCompressedSize.get();
            double overallRatio = (1.0 - (double)compressedTotal / originalTotal) * 100;
            
            System.out.printf("📦 总原始大小: %,d bytes (%.2f MB)\n", 
                             originalTotal, originalTotal / (1024.0 * 1024.0));
            System.out.printf("📦 总压缩大小: %,d bytes (%.2f MB)\n", 
                             compressedTotal, compressedTotal / (1024.0 * 1024.0));
            System.out.printf("📈 总压缩率: %.1f%%\n", overallRatio);
            System.out.printf("💾 节省空间: %,d bytes (%.2f MB)\n", 
                             originalTotal - compressedTotal, 
                             (originalTotal - compressedTotal) / (1024.0 * 1024.0));
        }
        
        System.out.println("\n📋 详细结果:");
        for (CompressionResult result : results) {
            if (result.success) {
                System.out.printf("  ✅ %s → %s (%.1f%%, %dms)\n", 
                                 result.inputFile, result.outputFile, 
                                 result.compressionRatio * 100, result.processingTime);
            } else {
                System.out.printf("  ❌ %s - %s\n", result.inputFile, result.errorMessage);
            }
        }
        
        System.out.println("=".repeat(60));
    }
    
    /**
     * 关闭线程池
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("🔚 多线程压缩器已关闭");
    }
    
    /**
     * 主函数 - 演示多线程压缩
     */
    public static void main(String[] args) {
        System.out.println("🚀 多线程PNG压缩演示");
        
        // 创建多线程压缩器
        MultiThreadPngCompressor multiCompressor = new MultiThreadPngCompressor();
        
        try {
            // 要压缩的文件
            String[] inputFiles = {"test.png", "test1.png", "test2.png"};
            
            System.out.println("\n💡 测试1: 标准质量压缩");
            multiCompressor.compressFiles(inputFiles, "mt_standard_", 80, 256, 3);
            
            System.out.println("\n💡 测试2: 高质量压缩");
            multiCompressor.compressFiles(inputFiles, "mt_high_", 90, 256, 1);
            
            System.out.println("\n💡 测试3: 快速压缩");
            multiCompressor.compressFiles(inputFiles, "mt_fast_", 70, 128, 6);
            
        } finally {
            // 确保关闭线程池
            multiCompressor.shutdown();
        }
    }
}

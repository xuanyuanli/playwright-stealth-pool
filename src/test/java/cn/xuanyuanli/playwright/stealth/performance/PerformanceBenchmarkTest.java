package cn.xuanyuanli.playwright.stealth.performance;

import cn.xuanyuanli.playwright.stealth.config.PlaywrightConfig;
import cn.xuanyuanli.playwright.stealth.config.StealthMode;
import cn.xuanyuanli.playwright.stealth.manager.PlaywrightBrowserManager;
import cn.xuanyuanli.playwright.stealth.manager.PlaywrightManager;
import cn.xuanyuanli.playwright.stealth.TestConditions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static cn.xuanyuanli.playwright.stealth.performance.PerformanceBaseline.*;

/**
 * 性能基准测试
 *
 * <p>测试连接池的性能表现，包括：</p>
 * <ul>
 *   <li>连接池创建性能</li>
 *   <li>并发访问性能</li>
 *   <li>不同反检测模式的性能影响</li>
 *   <li>内存使用情况</li>
 *   <li>资源清理性能</li>
 * </ul>
 *
 * @author xuanyuanli
 */
@DisplayName("性能基准测试")
@EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isPerformanceTestsEnabled")
@Tag("performance")
@Tag("slow")
class PerformanceBenchmarkTest {

    private static final int WARMUP_ITERATIONS = 3;
    private static final int BENCHMARK_ITERATIONS = 10;

    @Nested
    @DisplayName("PlaywrightManager性能测试")
    class PlaywrightManagerPerformanceTests {

        @Test
        @DisplayName("测试不同StealthMode的性能影响")
        @Timeout(60)
        void shouldMeasureStealthModePerformanceImpact() {
            StealthMode[] modes = {StealthMode.DISABLED, StealthMode.LIGHT, StealthMode.FULL};
            
            for (StealthMode mode : modes) {
                long totalTime = benchmarkPlaywrightManagerWithMode(mode);
                double avgTime = totalTime / (double) BENCHMARK_ITERATIONS;
                
                String metricKey = "stealth." + mode.name().toLowerCase() + ".avg";
                System.out.println(formatReport(metricKey, avgTime));
                
                // 使用性能基线验证
                assertThat(isWithinBaseline(metricKey, avgTime))
                    .as("性能指标 %s 应该在基线范围内: %s", metricKey, formatReport(metricKey, avgTime))
                    .isTrue();
            }
        }

        @Test
        @DisplayName("测试连接池大小对性能的影响")
        @Timeout(120)
        void shouldMeasurePoolSizePerformanceImpact() {
            int[] poolSizes = {1, 2, 4, 8};
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.LIGHT);
            
            for (int poolSize : poolSizes) {
                long totalTime = benchmarkPlaywrightManagerWithPoolSize(config, poolSize);
                double avgTime = totalTime / (double) BENCHMARK_ITERATIONS;
                
                System.out.printf("PoolSize=%d - 平均执行时间: %.2f ms%n", 
                                poolSize, avgTime);
                
                // 使用基线验证连接池创建性能
                String metricKey = "pool.creation.avg";
                assertThat(isWithinBaseline(metricKey, avgTime))
                    .as("连接池创建性能应在基线范围内: %s", formatReport(metricKey, avgTime))
                    .isTrue();
            }
        }

        @Test
        @DisplayName("测试并发执行的性能表现")
        @Timeout(180)
        void shouldMeasureConcurrentExecutionPerformance() throws InterruptedException {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.LIGHT);
                    
            int[] threadCounts = {1, 2, 4, 8};
            
            for (int threadCount : threadCounts) {
                long executionTime = benchmarkConcurrentExecution(config, threadCount);
                
                System.out.printf("并发线程数=%d - 总执行时间: %d ms%n", 
                                threadCount, executionTime);
                
                // 使用基线验证并发执行性能  
                String metricKey = "pool.concurrent.avg";
                double avgConcurrentTime = (double) executionTime / threadCount;
                assertThat(isWithinBaseline(metricKey, avgConcurrentTime))
                    .as("并发执行性能应在基线范围内: %s", formatReport(metricKey, avgConcurrentTime))
                    .isTrue();
            }
        }

        private long benchmarkPlaywrightManagerWithMode(StealthMode mode) {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(mode);
                    
            // 预热
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                try (PlaywrightManager manager = new PlaywrightManager(2)) {
                    manager.execute(config, page -> {
                        page.setContent("<html><body>Test</body></html>");
                        page.textContent("body");
                    });
                }
            }
            
            // 基准测试
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
                try (PlaywrightManager manager = new PlaywrightManager(2)) {
                    manager.execute(config, page -> {
                        page.setContent("<html><body>Benchmark Test</body></html>");
                        String content = page.textContent("body");
                        assertThat(content).isEqualTo("Benchmark Test");
                    });
                }
            }
            long endTime = System.currentTimeMillis();
            
            return endTime - startTime;
        }

        private long benchmarkPlaywrightManagerWithPoolSize(PlaywrightConfig config, int poolSize) {
            // 预热
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                try (PlaywrightManager manager = new PlaywrightManager(poolSize)) {
                    manager.execute(config, page -> {
                        page.setContent("<html><body>Test</body></html>");
                    });
                }
            }
            
            // 基准测试
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
                try (PlaywrightManager manager = new PlaywrightManager(poolSize)) {
                    manager.execute(config, page -> {
                        page.setContent("<html><body>Pool Size Test</body></html>");
                        page.textContent("body");
                    });
                }
            }
            long endTime = System.currentTimeMillis();
            
            return endTime - startTime;
        }

        private long benchmarkConcurrentExecution(PlaywrightConfig config, int threadCount) 
                throws InterruptedException {
            try (PlaywrightManager manager = new PlaywrightManager(Math.max(2, threadCount / 2))) {
                
                ExecutorService executor = Executors.newFixedThreadPool(threadCount);
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                
                long startTime = System.currentTimeMillis();
                
                for (int i = 0; i < threadCount; i++) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        manager.execute(config, page -> {
                            page.setContent("<html><body>Concurrent Test</body></html>");
                            String content = page.textContent("body");
                            assertThat(content).isEqualTo("Concurrent Test");
                        });
                    }, executor);
                    
                    futures.add(future);
                }
                
                // 等待所有任务完成
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                
                long endTime = System.currentTimeMillis();
                
                executor.shutdown();
                executor.awaitTermination(10, TimeUnit.SECONDS);
                
                return endTime - startTime;
            }
        }
    }

    @Nested
    @DisplayName("PlaywrightBrowserManager性能测试")
    class PlaywrightBrowserManagerPerformanceTests {

        @Test
        @DisplayName("测试Browser连接池的性能优势")
        @Timeout(120)
        void shouldDemonstrateBrowserPoolPerformanceAdvantage() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.LIGHT);
            
            // 测试无连接池的性能（每次创建新Browser）
            long nonePoolTime = benchmarkWithoutBrowserPool(config);
            
            // 测试有连接池的性能
            long withPoolTime = benchmarkWithBrowserPool(config);
            
            System.out.printf("无连接池执行时间: %d ms%n", nonePoolTime);
            System.out.printf("有连接池执行时间: %d ms%n", withPoolTime);
            
            // 连接池应该提供显著的性能提升
            assertThat(withPoolTime).isLessThan(nonePoolTime);
            
            // 性能提升应该至少20%
            double improvement = (double) (nonePoolTime - withPoolTime) / nonePoolTime * 100;
            System.out.printf("性能提升: %.1f%%n", improvement);
            assertThat(improvement).isGreaterThan(20.0);
        }

        @Test
        @DisplayName("测试连接池预热的性能影响")
        @Timeout(60)
        void shouldMeasureWarmupPerformanceImpact() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.LIGHT);
                    
            try (PlaywrightBrowserManager manager = new PlaywrightBrowserManager(config, 3)) {
                
                // 测试冷启动性能
                long coldStartTime = System.currentTimeMillis();
                manager.execute(page -> {
                    page.setContent("<html><body>Cold Start</body></html>");
                    page.textContent("body");
                });
                long coldEndTime = System.currentTimeMillis();
                
                // 预热连接池
                manager.warmUpPool(2);
                
                // 测试预热后性能
                long warmStartTime = System.currentTimeMillis();
                for (int i = 0; i < 3; i++) {
                    manager.execute(page -> {
                        page.setContent("<html><body>Warm Start</body></html>");
                        page.textContent("body");
                    });
                }
                long warmEndTime = System.currentTimeMillis();
                
                long coldTime = coldEndTime - coldStartTime;
                long warmTime = (warmEndTime - warmStartTime) / 3; // 平均时间
                
                System.out.printf("冷启动时间: %d ms%n", coldTime);
                System.out.printf("预热后平均时间: %d ms%n", warmTime);
                
                // 预热应该提供性能优势
                assertThat(warmTime).isLessThan(coldTime);
            }
        }

        @Test
        @DisplayName("测试连接池在高并发下的性能表现")
        @Timeout(180)
        void shouldMeasureHighConcurrencyPerformance() throws InterruptedException {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.LIGHT);
                    
            try (PlaywrightBrowserManager manager = new PlaywrightBrowserManager(config, 4)) {
                
                // 预热连接池
                manager.warmUpPool(2);
                
                int taskCount = 20;
                int threadCount = 8;
                
                ExecutorService executor = Executors.newFixedThreadPool(threadCount);
                List<CompletableFuture<Long>> futures = new ArrayList<>();
                
                for (int i = 0; i < taskCount; i++) {
                    CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                        long startTime = System.currentTimeMillis();
                        manager.execute(page -> {
                            page.setContent("<html><body>High Concurrency Test</body></html>");
                            String content = page.textContent("body");
                            assertThat(content).isEqualTo("High Concurrency Test");
                        });
                        return System.currentTimeMillis() - startTime;
                    }, executor);
                    
                    futures.add(future);
                }
                
                // 收集所有任务的执行时间
                List<Long> executionTimes = new ArrayList<>();
                for (CompletableFuture<Long> future : futures) {
                    executionTimes.add(future.join());
                }
                
                executor.shutdown();
                executor.awaitTermination(10, TimeUnit.SECONDS);
                
                // 计算统计信息
                double avgTime = executionTimes.stream().mapToLong(Long::longValue).average().orElse(0);
                long maxTime = executionTimes.stream().mapToLong(Long::longValue).max().orElse(0);
                long minTime = executionTimes.stream().mapToLong(Long::longValue).min().orElse(0);
                
                System.out.printf("高并发测试结果 - 任务数: %d, 线程数: %d%n", taskCount, threadCount);
                System.out.printf("平均执行时间: %.1f ms%n", avgTime);
                System.out.printf("最大执行时间: %d ms%n", maxTime);
                System.out.printf("最小执行时间: %d ms%n", minTime);
                
                // 性能断言
                assertThat(avgTime).isLessThan(2000); // 平均执行时间不超过2秒
                assertThat(maxTime).isLessThan(5000); // 最大执行时间不超过5秒
            }
        }

        private long benchmarkWithoutBrowserPool(PlaywrightConfig config) {
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < 5; i++) {
                try (PlaywrightManager manager = new PlaywrightManager(1)) {
                    manager.execute(config, page -> {
                        page.setContent("<html><body>No Pool Test</body></html>");
                        page.textContent("body");
                    });
                }
            }
            
            return System.currentTimeMillis() - startTime;
        }

        private long benchmarkWithBrowserPool(PlaywrightConfig config) {
            long startTime = System.currentTimeMillis();
            
            try (PlaywrightBrowserManager manager = new PlaywrightBrowserManager(config, 2)) {
                manager.warmUpPool(1);
                
                for (int i = 0; i < 5; i++) {
                    manager.execute(page -> {
                        page.setContent("<html><body>With Pool Test</body></html>");
                        page.textContent("body");
                    });
                }
            }
            
            return System.currentTimeMillis() - startTime;
        }
    }

    @Nested
    @DisplayName("内存使用性能测试")
    class MemoryUsagePerformanceTests {

        @Test
        @DisplayName("测试连接池的内存使用情况")
        @Timeout(60)
        void shouldMeasureMemoryUsage() throws InterruptedException {
            Runtime runtime = Runtime.getRuntime();
            
            // 获取初始内存使用情况
            runtime.gc(); // 建议垃圾回收
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();
            
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.LIGHT);
            
            // 创建并使用连接池
            try (PlaywrightBrowserManager manager = new PlaywrightBrowserManager(config, 3)) {
                manager.warmUpPool(2);
                
                // 执行一些操作
                for (int i = 0; i < 5; i++) {
                    manager.execute(page -> {
                        page.setContent("<html><body>Memory Test</body></html>");
                        page.textContent("body");
                    });
                }
                
                // 获取使用后的内存情况
                runtime.gc();
                long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                long memoryIncrease = usedMemory - initialMemory;
                
                System.out.printf("初始内存使用: %.1f MB%n", initialMemory / 1024.0 / 1024.0);
                System.out.printf("使用后内存: %.1f MB%n", usedMemory / 1024.0 / 1024.0);
                System.out.printf("内存增长: %.1f MB%n", memoryIncrease / 1024.0 / 1024.0);
                
                // 内存使用应该在合理范围内（小于500MB增长）
                assertThat(memoryIncrease).isLessThan(500 * 1024 * 1024);
            }
            
            // 连接池关闭后内存应该被释放
            runtime.gc();
            Thread.sleep(1000); // 给垃圾回收一些时间
            runtime.gc();
            
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryDiff = finalMemory - initialMemory;
            
            System.out.printf("最终内存使用: %.1f MB%n", finalMemory / 1024.0 / 1024.0);
            System.out.printf("内存差异: %.1f MB%n", memoryDiff / 1024.0 / 1024.0);
            
            // 内存应该基本释放（允许一些合理的增长）
            assertThat(Math.abs(memoryDiff)).isLessThan(100 * 1024 * 1024); // 100MB误差范围
        }
    }

    @Nested
    @DisplayName("资源清理性能测试")
    class ResourceCleanupPerformanceTests {

        @Test
        @DisplayName("测试大量连接池创建和销毁的性能")
        @Timeout(120)
        void shouldMeasurePoolCreationDestructionPerformance() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.LIGHT);
            
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < 10; i++) {
                try (PlaywrightBrowserManager manager = new PlaywrightBrowserManager(config, 2)) {
                    manager.execute(page -> {
                        page.setContent("<html><body>Cleanup Test " + Thread.currentThread().getId() + "</body></html>");
                        page.textContent("body");
                    });
                    // 自动关闭资源
                }
            }
            
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            
            System.out.printf("10次连接池创建/销毁总时间: %d ms%n", totalTime);
            System.out.printf("平均每次时间: %.1f ms%n", totalTime / 10.0);
            
            // 资源清理应该高效 - 放宽时间限制考虑到Browser实例创建销毁的开销
            assertThat(totalTime).isLessThan(120000); // 2分钟内完成
            assertThat(totalTime / 10.0).isLessThan(12000); // 平均每次不超过12秒
        }

        @Test
        @DisplayName("测试连接池空闲对象清理的性能")
        @Timeout(90)
        void shouldMeasureIdleObjectEvictionPerformance() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.LIGHT);
                    
            try (PlaywrightBrowserManager manager = new PlaywrightBrowserManager(config, 4)) {
                
                // 预热并创建一些空闲对象
                manager.warmUpPool(3);
                
                // 执行一些操作
                for (int i = 0; i < 3; i++) {
                    manager.execute(page -> {
                        page.setContent("<html><body>Eviction Test</body></html>");
                        page.textContent("body");
                    });
                }
                
                System.out.println("清理前状态: " + manager.getPoolStatus());
                
                // 测试空闲对象清理性能
                long startTime = System.currentTimeMillis();
                manager.evictIdleBrowsers();
                long endTime = System.currentTimeMillis();
                
                System.out.println("清理后状态: " + manager.getPoolStatus());
                System.out.printf("空闲对象清理时间: %d ms%n", endTime - startTime);
                
                // 清理操作应该很快
                assertThat(endTime - startTime).isLessThan(5000); // 5秒内完成
            }
        }
    }
}
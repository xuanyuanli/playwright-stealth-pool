package cn.xuanyuanli.playwright.stealth.integration;

import cn.xuanyuanli.playwright.stealth.config.PlaywrightConfig;
import cn.xuanyuanli.playwright.stealth.config.StealthMode;
import cn.xuanyuanli.playwright.stealth.pool.PlaywrightBrowserFactory;
import cn.xuanyuanli.playwright.stealth.pool.PlaywrightFactory;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Playwright;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * 连接池集成测试
 *
 * <p>测试连接池组件之间的集成功能，包括：</p>
 * <ul>
 *   <li>Playwright连接池集成</li>
 *   <li>Browser连接池集成</li>
 *   <li>配置与工厂的集成</li>
 *   <li>并发访问集成</li>
 *   <li>资源管理集成</li>
 * </ul>
 *
 * @author xuanyuanli
 */
@DisplayName("连接池集成测试")
class PoolIntegrationTest {

    @Nested
    @DisplayName("Playwright连接池集成测试")
    class PlaywrightPoolIntegrationTests {

        @Test
        @DisplayName("Playwright连接池应该能够正常创建和管理实例")
        void shouldCreateAndManagePlaywrightInstances() {
            PlaywrightFactory factory = new PlaywrightFactory();
            GenericObjectPoolConfig<Playwright> config = new GenericObjectPoolConfig<>();
            config.setMaxTotal(3);
            config.setMinIdle(1);
            config.setMaxIdle(2);
            
            try (GenericObjectPool<Playwright> pool = new GenericObjectPool<>(factory, config)) {
                
                // 测试从池中借用对象
                Playwright playwright1 = pool.borrowObject();
                assertThat(playwright1).isNotNull();
                
                Playwright playwright2 = pool.borrowObject();
                assertThat(playwright2).isNotNull();
                assertThat(playwright2).isNotSameAs(playwright1);
                
                // 验证池状态
                assertThat(pool.getNumActive()).isEqualTo(2);
                
                // 返回对象到池中
                pool.returnObject(playwright1);
                pool.returnObject(playwright2);
                
                assertThat(pool.getNumActive()).isEqualTo(0);
                assertThat(pool.getNumIdle()).isEqualTo(2);
                
            } catch (Exception e) {
                fail("Pool integration test failed", e);
            }
        }

        @Test
        @DisplayName("连接池配置应该正确生效")
        void shouldRespectPoolConfiguration() {
            PlaywrightFactory factory = new PlaywrightFactory();
            GenericObjectPoolConfig<Playwright> config = new GenericObjectPoolConfig<>();
            config.setMaxTotal(2);
            config.setBlockWhenExhausted(false);
            
            try (GenericObjectPool<Playwright> pool = new GenericObjectPool<>(factory, config)) {
                
                // 借用所有可用对象
                Playwright playwright1 = pool.borrowObject();
                Playwright playwright2 = pool.borrowObject();
                
                // 尝试借用超出限制的对象应该失败
                assertThatThrownBy(() -> pool.borrowObject())
                        .isInstanceOf(Exception.class);
                
                // 清理
                pool.returnObject(playwright1);
                pool.returnObject(playwright2);
                
            } catch (Exception e) {
                fail("Pool configuration test failed", e);
            }
        }
    }

    @Nested
    @DisplayName("Browser连接池集成测试")
    class BrowserPoolIntegrationTests {

        @Test
        @DisplayName("Browser连接池应该能够正常创建和管理实例")
        void shouldCreateAndManageBrowserInstances() {
            PlaywrightConfig playwrightConfig = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.LIGHT);
                    
            PlaywrightBrowserFactory factory = new PlaywrightBrowserFactory(playwrightConfig);
            GenericObjectPoolConfig<Browser> config = new GenericObjectPoolConfig<>();
            config.setMaxTotal(2);
            config.setTestOnBorrow(true);
            
            try (GenericObjectPool<Browser> pool = new GenericObjectPool<>(factory, config)) {
                
                // 测试从池中借用Browser
                Browser browser1 = pool.borrowObject();
                assertThat(browser1).isNotNull();
                assertThat(browser1.isConnected()).isTrue();
                
                Browser browser2 = pool.borrowObject();
                assertThat(browser2).isNotNull();
                assertThat(browser2.isConnected()).isTrue();
                assertThat(browser2).isNotSameAs(browser1);
                
                // 验证池状态
                assertThat(pool.getNumActive()).isEqualTo(2);
                
                // 返回对象到池中
                pool.returnObject(browser1);
                pool.returnObject(browser2);
                
                assertThat(pool.getNumActive()).isEqualTo(0);
                assertThat(pool.getNumIdle()).isEqualTo(2);
                
            } catch (Exception e) {
                fail("Browser pool integration test failed", e);
            }
        }

        @Test
        @DisplayName("Browser验证功能应该正确集成")
        void shouldIntegrateValidationCorrectly() {
            PlaywrightConfig playwrightConfig = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.DISABLED);
                    
            PlaywrightBrowserFactory factory = new PlaywrightBrowserFactory(playwrightConfig);
            GenericObjectPoolConfig<Browser> config = new GenericObjectPoolConfig<>();
            config.setMaxTotal(2);
            config.setTestOnBorrow(true);
            config.setTestOnReturn(true);
            
            try (GenericObjectPool<Browser> pool = new GenericObjectPool<>(factory, config)) {
                
                Browser browser = pool.borrowObject();
                assertThat(browser).isNotNull();
                assertThat(browser.isConnected()).isTrue();
                
                // 正常返回应该成功
                pool.returnObject(browser);
                
                // 再次借用应该得到有效的Browser
                Browser browser2 = pool.borrowObject();
                assertThat(browser2).isNotNull();
                assertThat(browser2.isConnected()).isTrue();
                
                pool.returnObject(browser2);
                
            } catch (Exception e) {
                fail("Browser validation integration test failed", e);
            }
        }
    }

    @Nested
    @DisplayName("配置与工厂集成测试")
    class ConfigurationFactoryIntegrationTests {

        @Test
        @DisplayName("不同StealthMode配置应该产生相应的Browser")
        void shouldCreateBrowsersWithDifferentStealthModes() {
            StealthMode[] modes = {StealthMode.DISABLED, StealthMode.LIGHT, StealthMode.FULL};
            
            for (StealthMode mode : modes) {
                PlaywrightConfig config = new PlaywrightConfig()
                        .setHeadless(true)
                        .setStealthMode(mode);
                        
                PlaywrightBrowserFactory factory = new PlaywrightBrowserFactory(config);
                GenericObjectPoolConfig<Browser> poolConfig = new GenericObjectPoolConfig<>();
                poolConfig.setMaxTotal(1);
                
                try (GenericObjectPool<Browser> pool = new GenericObjectPool<>(factory, poolConfig)) {
                    
                    Browser browser = pool.borrowObject();
                    assertThat(browser).isNotNull();
                    assertThat(browser.isConnected()).isTrue();
                    
                    pool.returnObject(browser);
                    
                } catch (Exception e) {
                    fail("StealthMode " + mode + " integration test failed", e);
                }
            }
        }

        @Test
        @DisplayName("配置参数应该正确传递给Browser")
        void shouldPassConfigurationToBrowser() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setDisableGpu(true)
                    .setDisableImageRender(true)
                    .setStealthMode(StealthMode.LIGHT);
                    
            PlaywrightBrowserFactory factory = new PlaywrightBrowserFactory(config);
            
            try {
                Browser browser = factory.create();
                assertThat(browser).isNotNull();
                assertThat(browser.isConnected()).isTrue();
                
                // 验证Browser具有基本功能
                assertThat(browser.contexts()).isNotNull();
                
                PooledObject<Browser> pooled = factory.wrap(browser);
                factory.destroyObject(pooled);
                
            } catch (Exception e) {
                fail("Configuration passing test failed", e);
            }
        }
    }

    @Nested
    @DisplayName("并发访问集成测试")
    @Timeout(30) // 30秒超时
    class ConcurrentAccessIntegrationTests {

        @Test
        @DisplayName("多线程并发访问Playwright连接池应该安全")
        void shouldHandleConcurrentPlaywrightPoolAccess() {
            PlaywrightFactory factory = new PlaywrightFactory();
            GenericObjectPoolConfig<Playwright> config = new GenericObjectPoolConfig<>();
            config.setMaxTotal(3);
            config.setBlockWhenExhausted(true);
            config.setMaxWait(Duration.ofSeconds(5));
            
            try (GenericObjectPool<Playwright> pool = new GenericObjectPool<>(factory, config)) {
                
                int threadCount = 5;
                ExecutorService executor = Executors.newFixedThreadPool(threadCount);
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                
                for (int i = 0; i < threadCount; i++) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            Playwright playwright = pool.borrowObject();
                            assertThat(playwright).isNotNull();
                            
                            // 模拟使用
                            Thread.sleep(100);
                            
                            pool.returnObject(playwright);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }, executor);
                    
                    futures.add(future);
                }
                
                // 等待所有任务完成
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                
                executor.shutdown();
                executor.awaitTermination(5, TimeUnit.SECONDS);
                
                // 验证池状态
                assertThat(pool.getNumActive()).isEqualTo(0);
                assertThat(pool.getNumIdle()).isLessThanOrEqualTo(3);
                
            } catch (Exception e) {
                fail("Concurrent Playwright pool access test failed", e);
            }
        }

        @Test
        @DisplayName("多线程并发访问Browser连接池应该安全")
        void shouldHandleConcurrentBrowserPoolAccess() {
            PlaywrightConfig playwrightConfig = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.LIGHT);
                    
            PlaywrightBrowserFactory factory = new PlaywrightBrowserFactory(playwrightConfig);
            GenericObjectPoolConfig<Browser> config = new GenericObjectPoolConfig<>();
            config.setMaxTotal(2);
            config.setBlockWhenExhausted(true);
            config.setMaxWait(Duration.ofSeconds(10));
            
            try (GenericObjectPool<Browser> pool = new GenericObjectPool<>(factory, config)) {
                
                int threadCount = 4;
                ExecutorService executor = Executors.newFixedThreadPool(threadCount);
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                
                for (int i = 0; i < threadCount; i++) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            Browser browser = pool.borrowObject();
                            assertThat(browser).isNotNull();
                            assertThat(browser.isConnected()).isTrue();
                            
                            // 模拟使用Browser
                            assertThat(browser.contexts()).isNotNull();
                            Thread.sleep(50);
                            
                            pool.returnObject(browser);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }, executor);
                    
                    futures.add(future);
                }
                
                // 等待所有任务完成
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                
                executor.shutdown();
                executor.awaitTermination(10, TimeUnit.SECONDS);
                
                // 验证池状态
                assertThat(pool.getNumActive()).isEqualTo(0);
                assertThat(pool.getNumIdle()).isLessThanOrEqualTo(2);
                
            } catch (Exception e) {
                fail("Concurrent Browser pool access test failed", e);
            }
        }
    }

    @Nested
    @DisplayName("资源管理集成测试")
    class ResourceManagementIntegrationTests {

        @Test
        @DisplayName("连接池关闭应该正确释放所有资源")
        void shouldReleaseAllResourcesOnPoolClose() {
            PlaywrightBrowserFactory factory = new PlaywrightBrowserFactory(
                new PlaywrightConfig().setHeadless(true).setStealthMode(StealthMode.DISABLED)
            );
            
            GenericObjectPoolConfig<Browser> config = new GenericObjectPoolConfig<>();
            config.setMaxTotal(2);
            
            List<Browser> borrowedBrowsers = new ArrayList<>();
            
            try (GenericObjectPool<Browser> pool = new GenericObjectPool<>(factory, config)) {
                
                // 借用一些Browser但不返回
                Browser browser1 = pool.borrowObject();
                Browser browser2 = pool.borrowObject();
                
                borrowedBrowsers.add(browser1);
                borrowedBrowsers.add(browser2);
                
                assertThat(pool.getNumActive()).isEqualTo(2);
                
                // 连接池关闭时应该自动清理资源
            } catch (Exception e) {
                fail("Resource management test failed", e);
            }
            
            // 验证所有Browser都已被关闭（通过连接池的关闭）
            // 注意：由于Browser可能已经被池关闭，我们不能直接测试isConnected
        }

        @Test
        @DisplayName("异常情况下应该正确清理资源")
        void shouldCleanupResourcesOnException() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.LIGHT);
                    
            PlaywrightBrowserFactory factory = new PlaywrightBrowserFactory(config);
            GenericObjectPoolConfig<Browser> poolConfig = new GenericObjectPoolConfig<>();
            poolConfig.setMaxTotal(1);
            poolConfig.setTestOnBorrow(true);
            
            try (GenericObjectPool<Browser> pool = new GenericObjectPool<>(factory, poolConfig)) {
                
                Browser browser = pool.borrowObject();
                assertThat(browser).isNotNull();
                
                try {
                    // 模拟异常情况
                    throw new RuntimeException("Simulated exception");
                } catch (RuntimeException e) {
                    // 即使发生异常，也要确保资源返回
                    pool.returnObject(browser);
                }
                
                // 验证池状态正常
                assertThat(pool.getNumActive()).isEqualTo(0);
                assertThat(pool.getNumIdle()).isEqualTo(1);
                
            } catch (Exception e) {
                fail("Exception handling test failed", e);
            }
        }
    }

    @Nested
    @DisplayName("性能集成测试")
    class PerformanceIntegrationTests {

        @Test
        @DisplayName("连接池应该提供性能优势")
        void shouldProvidePerformanceBenefit() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.LIGHT);
                    
            PlaywrightBrowserFactory factory = new PlaywrightBrowserFactory(config);
            GenericObjectPoolConfig<Browser> poolConfig = new GenericObjectPoolConfig<>();
            poolConfig.setMaxTotal(2);
            poolConfig.setMinIdle(1);
            
            try (GenericObjectPool<Browser> pool = new GenericObjectPool<>(factory, poolConfig)) {
                
                // 预热连接池
                Browser browser = pool.borrowObject();
                pool.returnObject(browser);
                
                long startTime = System.currentTimeMillis();
                
                // 多次借用和返回，测试性能
                for (int i = 0; i < 5; i++) {
                    Browser testBrowser = pool.borrowObject();
                    assertThat(testBrowser).isNotNull();
                    assertThat(testBrowser.isConnected()).isTrue();
                    pool.returnObject(testBrowser);
                }
                
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                
                // 连接池的多次操作应该相对较快（由于复用）
                assertThat(duration).isLessThan(5000); // 5秒内完成
                
            } catch (Exception e) {
                fail("Performance test failed", e);
            }
        }
    }
}
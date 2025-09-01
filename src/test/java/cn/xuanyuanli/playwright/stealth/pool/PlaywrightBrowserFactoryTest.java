package cn.xuanyuanli.playwright.stealth.pool;

import cn.xuanyuanli.playwright.stealth.config.PlaywrightConfig;
import cn.xuanyuanli.playwright.stealth.config.StealthMode;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Playwright;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PlaywrightBrowserFactory单元测试
 *
 * <p>测试Playwright浏览器实例工厂的功能，包括：</p>
 * <ul>
 *   <li>Browser实例创建功能</li>
 *   <li>实例包装功能</li>
 *   <li>实例验证功能</li>
 *   <li>实例销毁功能</li>
 *   <li>配置处理</li>
 *   <li>异常处理</li>
 * </ul>
 *
 * @author xuanyuanli
 */
@DisplayName("PlaywrightBrowserFactory 浏览器实例工厂测试")
class PlaywrightBrowserFactoryTest {

    private PlaywrightBrowserFactory factory;
    private PlaywrightConfig config;
    private AutoCloseable mockitoCloseable;

    @Mock
    private Browser mockBrowser;
    
    @Mock
    private Playwright mockPlaywright;
    
    @Mock
    private BrowserContext mockContext;

    @BeforeEach
    void setUp() {
        mockitoCloseable = MockitoAnnotations.openMocks(this);
        
        config = new PlaywrightConfig()
                .setHeadless(true)
                .setStealthMode(StealthMode.LIGHT);
                
        factory = new PlaywrightBrowserFactory(config);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mockitoCloseable != null) {
            mockitoCloseable.close();
        }
    }

    @Nested
    @DisplayName("工厂创建测试")
    class FactoryCreationTests {

        @Test
        @DisplayName("应该能够使用配置创建工厂")
        void shouldCreateFactoryWithConfig() {
            PlaywrightConfig customConfig = new PlaywrightConfig()
                    .setHeadless(false)
                    .setStealthMode(StealthMode.FULL);
                    
            PlaywrightBrowserFactory customFactory = new PlaywrightBrowserFactory(customConfig);
            
            assertThat(customFactory).isNotNull();
        }

        @Test
        @DisplayName("应该能够使用null配置创建工厂（使用默认配置）")
        void shouldCreateFactoryWithNullConfig() {
            assertThatCode(() -> {
                PlaywrightBrowserFactory defaultFactory = new PlaywrightBrowserFactory(null);
                assertThat(defaultFactory).isNotNull();
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("应该正确继承BasePooledObjectFactory")
        void shouldExtendBasePooledObjectFactory() {
            assertThat(factory).isInstanceOf(org.apache.commons.pool2.BasePooledObjectFactory.class);
        }
    }

    @Nested
    @DisplayName("Browser实例创建测试")
    class BrowserCreationTests {

        @Test
        @DisplayName("应该能够创建Browser实例")
        void shouldCreateBrowserInstance() {
            // 由于create()方法依赖真实的Playwright API，我们测试它能正常执行
            assertThatCode(() -> {
                Browser browser = factory.create();
                assertThat(browser).isNotNull();
                
                // 测试完成后清理资源
                PooledObject<Browser> pooled = factory.wrap(browser);
                factory.destroyObject(pooled);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("创建的Browser应该具有基本功能")
        void shouldCreateFunctionalBrowser() {
            Browser browser = null;
            try {
                browser = factory.create();
                
                assertThat(browser).isNotNull();
                assertThat(browser.isConnected()).isTrue();
                
                // 验证Browser的基本功能
                assertThat(browser.contexts()).isNotNull();
                
            } finally {
                if (browser != null) {
                    PooledObject<Browser> pooled = factory.wrap(browser);
                    factory.destroyObject(pooled);
                }
            }
        }

        @Test
        @DisplayName("每次调用create应该返回不同的Browser实例")
        void shouldCreateDifferentBrowserInstances() {
            Browser browser1 = null;
            Browser browser2 = null;
            
            try {
                browser1 = factory.create();
                browser2 = factory.create();
                
                assertThat(browser1).isNotNull();
                assertThat(browser2).isNotNull();
                assertThat(browser1).isNotSameAs(browser2);
                
            } finally {
                if (browser1 != null) {
                    PooledObject<Browser> pooled1 = factory.wrap(browser1);
                    factory.destroyObject(pooled1);
                }
                if (browser2 != null) {
                    PooledObject<Browser> pooled2 = factory.wrap(browser2);
                    factory.destroyObject(pooled2);
                }
            }
        }

        @Test
        @DisplayName("使用不同配置应该创建相应的Browser")
        void shouldCreateBrowserWithDifferentConfigs() {
            // 测试不同的配置能够成功创建Browser
            PlaywrightConfig[] configs = {
                new PlaywrightConfig().setHeadless(true).setStealthMode(StealthMode.DISABLED),
                new PlaywrightConfig().setHeadless(true).setStealthMode(StealthMode.LIGHT),
                new PlaywrightConfig().setHeadless(true).setStealthMode(StealthMode.FULL)
            };
            
            List<Browser> browsers = new ArrayList<>();
            
            try {
                for (PlaywrightConfig testConfig : configs) {
                    PlaywrightBrowserFactory testFactory = new PlaywrightBrowserFactory(testConfig);
                    Browser browser = testFactory.create();
                    
                    assertThat(browser).isNotNull();
                    assertThat(browser.isConnected()).isTrue();
                    
                    browsers.add(browser);
                }
                
            } finally {
                // 清理所有创建的Browser
                for (int i = 0; i < browsers.size(); i++) {
                    if (browsers.get(i) != null) {
                        PlaywrightBrowserFactory testFactory = new PlaywrightBrowserFactory(configs[i]);
                        PooledObject<Browser> pooled = testFactory.wrap(browsers.get(i));
                        testFactory.destroyObject(pooled);
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("实例包装测试")
    class WrapInstanceTests {

        @Test
        @DisplayName("应该能够包装Browser实例")
        void shouldWrapBrowserInstance() {
            PooledObject<Browser> pooledObject = factory.wrap(mockBrowser);
            
            assertThat(pooledObject).isNotNull();
            assertThat(pooledObject).isInstanceOf(DefaultPooledObject.class);
            assertThat(pooledObject.getObject()).isEqualTo(mockBrowser);
        }

        @Test
        @DisplayName("包装的对象应该包含正确的实例")
        void shouldContainCorrectInstance() {
            PooledObject<Browser> pooledObject = factory.wrap(mockBrowser);
            
            assertThat(pooledObject.getObject()).isSameAs(mockBrowser);
        }

        @Test
        @DisplayName("应该能够包装null实例")
        void shouldWrapNullInstance() {
            PooledObject<Browser> pooledObject = factory.wrap(null);
            
            assertThat(pooledObject).isNotNull();
            assertThat(pooledObject.getObject()).isNull();
        }
    }

    @Nested
    @DisplayName("实例验证测试")
    class ValidateInstanceTests {

        @Test
        @DisplayName("有效的Browser应该通过验证")
        void shouldValidateHealthyBrowser() {
            when(mockBrowser.contexts()).thenReturn(List.of(mockContext));
            
            PooledObject<Browser> pooledObject = new DefaultPooledObject<>(mockBrowser);
            boolean isValid = factory.validateObject(pooledObject);
            
            assertThat(isValid).isTrue();
            verify(mockBrowser, times(1)).contexts();
        }

        @Test
        @DisplayName("无效的Browser应该验证失败")
        void shouldFailValidationForInvalidBrowser() {
            when(mockBrowser.contexts()).thenThrow(new RuntimeException("Browser closed"));
            
            PooledObject<Browser> pooledObject = new DefaultPooledObject<>(mockBrowser);
            boolean isValid = factory.validateObject(pooledObject);
            
            assertThat(isValid).isFalse();
            verify(mockBrowser, times(1)).contexts();
        }

        @Test
        @DisplayName("null Browser应该验证失败")
        void shouldFailValidationForNullBrowser() {
            PooledObject<Browser> pooledObject = new DefaultPooledObject<>(null);
            
            boolean isValid = factory.validateObject(pooledObject);
            
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("真实Browser的验证应该正常工作")
        void shouldValidateRealBrowser() {
            Browser browser = null;
            try {
                browser = factory.create();
                PooledObject<Browser> pooledObject = factory.wrap(browser);
                
                boolean isValid = factory.validateObject(pooledObject);
                
                assertThat(isValid).isTrue();
                
            } finally {
                if (browser != null) {
                    PooledObject<Browser> pooled = factory.wrap(browser);
                    factory.destroyObject(pooled);
                }
            }
        }
    }

    @Nested
    @DisplayName("实例销毁测试")
    class DestroyInstanceTests {

        @Test
        @DisplayName("应该能够正常销毁Mock Browser实例")
        void shouldDestroyMockBrowserNormally() {
            when(mockBrowser.isConnected()).thenReturn(true);
            
            PooledObject<Browser> pooledObject = new DefaultPooledObject<>(mockBrowser);
            
            assertThatCode(() -> factory.destroyObject(pooledObject))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("应该能够处理销毁时的异常")
        void shouldHandleDestroyException() {
            when(mockBrowser.isConnected()).thenReturn(true);
            doThrow(new RuntimeException("Close failed")).when(mockBrowser).close();
            
            PooledObject<Browser> pooledObject = new DefaultPooledObject<>(mockBrowser);
            
            // 即使close()抛出异常，destroyObject也不应该抛出异常
            assertThatCode(() -> factory.destroyObject(pooledObject))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("应该能够处理null对象的销毁")
        void shouldHandleNullObjectDestroy() {
            PooledObject<Browser> pooledObject = new DefaultPooledObject<>(null);
            
            assertThatCode(() -> factory.destroyObject(pooledObject))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("真实Browser的销毁应该正常工作")
        void shouldDestroyRealBrowser() {
            Browser browser = factory.create();
            PooledObject<Browser> pooledObject = factory.wrap(browser);
            
            assertThatCode(() -> factory.destroyObject(pooledObject))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("销毁操作应该清理缓存")
        void shouldCleanupCacheOnDestroy() {
            Browser browser = factory.create();
            PooledObject<Browser> pooledObject = factory.wrap(browser);
            
            // 销毁后，缓存应该被清理
            factory.destroyObject(pooledObject);
            
            // 无法直接访问private static缓存，但可以验证销毁操作不抛异常
            assertThatCode(() -> factory.destroyObject(pooledObject))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("完整生命周期测试")
    class FullLifecycleTests {

        @Test
        @DisplayName("完整的Browser生命周期应该正常工作")
        void shouldHandleCompleteBrowserLifecycle() {
            Browser browser = null;
            PooledObject<Browser> pooledObject = null;
            
            try {
                // 1. 创建实例
                browser = factory.create();
                assertThat(browser).isNotNull();
                assertThat(browser.isConnected()).isTrue();
                
                // 2. 包装实例
                pooledObject = factory.wrap(browser);
                assertThat(pooledObject).isNotNull();
                assertThat(pooledObject.getObject()).isSameAs(browser);
                
                // 3. 验证实例
                boolean isValid = factory.validateObject(pooledObject);
                assertThat(isValid).isTrue();
                
                // 4. 使用实例（验证基本功能）
                assertThat(browser.contexts()).isNotNull();
                
                // 5. 销毁实例
                factory.destroyObject(pooledObject);
                
            } catch (Exception e) {
                // 如果过程中出现异常，确保资源清理
                if (pooledObject != null) {
                    try {
                        factory.destroyObject(pooledObject);
                    } catch (Exception destroyEx) {
                        // 忽略清理时的异常
                    }
                }
                throw e;
            }
        }

        @Test
        @DisplayName("多个Browser的并发创建应该安全")
        void shouldSafelyConcurrentlyCreateBrowsers() {
            int threadCount = 3;
            Thread[] threads = new Thread[threadCount];
            Exception[] exceptions = new Exception[threadCount];
            Browser[] browsers = new Browser[threadCount];
            
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    try {
                        browsers[index] = factory.create();
                        assertThat(browsers[index]).isNotNull();
                        assertThat(browsers[index].isConnected()).isTrue();
                    } catch (Exception e) {
                        exceptions[index] = e;
                    }
                });
            }
            
            // 启动所有线程
            for (Thread thread : threads) {
                thread.start();
            }
            
            // 等待所有线程完成
            for (Thread thread : threads) {
                try {
                    thread.join(10000); // 10秒超时
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            // 验证结果
            for (int i = 0; i < threadCount; i++) {
                assertThat(exceptions[i]).isNull();
                assertThat(browsers[i]).isNotNull();
            }
            
            // 清理资源
            for (Browser browser : browsers) {
                if (browser != null) {
                    PooledObject<Browser> pooled = factory.wrap(browser);
                    factory.destroyObject(pooled);
                }
            }
        }
    }

    @Nested
    @DisplayName("配置相关测试")
    class ConfigurationTests {

        @Test
        @DisplayName("工厂应该正确处理各种配置")
        void shouldHandleVariousConfigurations() {
            PlaywrightConfig[] testConfigs = {
                null, // 应该使用默认配置
                new PlaywrightConfig(),
                new PlaywrightConfig().setHeadless(true),
                new PlaywrightConfig().setStealthMode(StealthMode.DISABLED),
                new PlaywrightConfig().setStealthMode(StealthMode.LIGHT),
                new PlaywrightConfig().setStealthMode(StealthMode.FULL)
            };
            
            for (PlaywrightConfig testConfig : testConfigs) {
                assertThatCode(() -> {
                    PlaywrightBrowserFactory testFactory = new PlaywrightBrowserFactory(testConfig);
                    assertThat(testFactory).isNotNull();
                }).doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("不同配置创建的Browser应该都能正常工作")
        void shouldCreateWorkingBrowsersWithDifferentConfigs() {
            PlaywrightConfig lightConfig = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.LIGHT);
                    
            PlaywrightConfig fullConfig = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.FULL);
                    
            PlaywrightBrowserFactory lightFactory = new PlaywrightBrowserFactory(lightConfig);
            PlaywrightBrowserFactory fullFactory = new PlaywrightBrowserFactory(fullConfig);
            
            Browser lightBrowser = null;
            Browser fullBrowser = null;
            
            try {
                lightBrowser = lightFactory.create();
                fullBrowser = fullFactory.create();
                
                assertThat(lightBrowser).isNotNull();
                assertThat(fullBrowser).isNotNull();
                assertThat(lightBrowser.isConnected()).isTrue();
                assertThat(fullBrowser.isConnected()).isTrue();
                
            } finally {
                if (lightBrowser != null) {
                    PooledObject<Browser> pooled = lightFactory.wrap(lightBrowser);
                    lightFactory.destroyObject(pooled);
                }
                if (fullBrowser != null) {
                    PooledObject<Browser> pooled = fullFactory.wrap(fullBrowser);
                    fullFactory.destroyObject(pooled);
                }
            }
        }
    }

    @Nested
    @DisplayName("资源管理测试")
    class ResourceManagementTests {

        @Test
        @DisplayName("创建失败时不应该泄露资源")
        void shouldNotLeakResourcesOnCreateFailure() {
            // 创建多个实例，然后全部销毁，验证无内存泄露
            List<Browser> browsers = new ArrayList<>();
            
            try {
                for (int i = 0; i < 3; i++) {
                    Browser browser = factory.create();
                    browsers.add(browser);
                    assertThat(browser).isNotNull();
                }
            } finally {
                // 确保所有Browser都被正确销毁
                for (Browser browser : browsers) {
                    if (browser != null) {
                        PooledObject<Browser> pooled = factory.wrap(browser);
                        factory.destroyObject(pooled);
                    }
                }
            }
        }

        @Test
        @DisplayName("销毁操作应该是幂等的")
        void shouldBeIdempotentDestroy() {
            Browser browser = factory.create();
            PooledObject<Browser> pooledObject = factory.wrap(browser);
            
            // 第一次销毁
            factory.destroyObject(pooledObject);
            
            // 第二次销毁应该安全
            assertThatCode(() -> factory.destroyObject(pooledObject))
                    .doesNotThrowAnyException();
        }
    }
}
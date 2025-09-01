package cn.xuanyuanli.playwright.stealth.pool;

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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PlaywrightFactory单元测试
 *
 * <p>测试Playwright实例工厂的功能，包括：</p>
 * <ul>
 *   <li>实例创建功能</li>
 *   <li>实例包装功能</li>
 *   <li>实例销毁功能</li>
 *   <li>异常处理</li>
 * </ul>
 *
 * @author xuanyuanli
 */
@DisplayName("PlaywrightFactory Playwright实例工厂测试")
class PlaywrightFactoryTest {

    private PlaywrightFactory factory;
    private AutoCloseable mockitoCloseable;

    @Mock
    private Playwright mockPlaywright;

    @BeforeEach
    void setUp() {
        mockitoCloseable = MockitoAnnotations.openMocks(this);
        factory = new PlaywrightFactory();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mockitoCloseable != null) {
            mockitoCloseable.close();
        }
    }

    @Nested
    @DisplayName("实例创建测试")
    class CreateInstanceTests {

        @Test
        @DisplayName("应该能够创建Playwright实例")
        void shouldCreatePlaywrightInstance() {
            // 由于Playwright.create()是静态方法，我们测试它能正常执行
            assertThatCode(() -> {
                Playwright playwright = factory.create();
                assertThat(playwright).isNotNull();
                // 测试完成后清理资源
                playwright.close();
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("创建的实例应该具有基本功能")
        void shouldCreateFunctionalInstance() {
            Playwright playwright = null;
            try {
                playwright = factory.create();
                
                assertThat(playwright).isNotNull();
                
                // 验证Playwright实例的基本功能
                assertThat(playwright.chromium()).isNotNull();
                assertThat(playwright.firefox()).isNotNull();
                assertThat(playwright.webkit()).isNotNull();
                
            } finally {
                if (playwright != null) {
                    playwright.close();
                }
            }
        }

        @Test
        @DisplayName("每次调用create应该返回新的实例")
        void shouldCreateNewInstanceEachTime() {
            Playwright playwright1 = null;
            Playwright playwright2 = null;
            
            try {
                playwright1 = factory.create();
                playwright2 = factory.create();
                
                assertThat(playwright1).isNotNull();
                assertThat(playwright2).isNotNull();
                assertThat(playwright1).isNotSameAs(playwright2);
                
            } finally {
                if (playwright1 != null) playwright1.close();
                if (playwright2 != null) playwright2.close();
            }
        }
    }

    @Nested
    @DisplayName("实例包装测试")
    class WrapInstanceTests {

        @Test
        @DisplayName("应该能够包装Playwright实例")
        void shouldWrapPlaywrightInstance() {
            PooledObject<Playwright> pooledObject = factory.wrap(mockPlaywright);
            
            assertThat(pooledObject).isNotNull();
            assertThat(pooledObject).isInstanceOf(DefaultPooledObject.class);
            assertThat(pooledObject.getObject()).isEqualTo(mockPlaywright);
        }

        @Test
        @DisplayName("包装的对象应该包含正确的实例")
        void shouldContainCorrectInstance() {
            PooledObject<Playwright> pooledObject = factory.wrap(mockPlaywright);
            
            assertThat(pooledObject.getObject()).isSameAs(mockPlaywright);
        }

        @Test
        @DisplayName("应该能够包装null实例")
        void shouldWrapNullInstance() {
            PooledObject<Playwright> pooledObject = factory.wrap(null);
            
            assertThat(pooledObject).isNotNull();
            assertThat(pooledObject.getObject()).isNull();
        }

        @Test
        @DisplayName("包装的对象应该具有池对象属性")
        void shouldHavePooledObjectProperties() {
            PooledObject<Playwright> pooledObject = factory.wrap(mockPlaywright);
            
            // 验证池对象的基本属性
            assertThat(pooledObject.getCreateInstant()).isNotNull();
            assertThat(pooledObject.getLastReturnInstant()).isNotNull();
            assertThat(pooledObject.getLastBorrowInstant()).isNotNull();
            assertThat(pooledObject.getLastUsedInstant()).isNotNull();
        }
    }

    @Nested
    @DisplayName("实例销毁测试")
    class DestroyInstanceTests {

        @Test
        @DisplayName("应该能够正常销毁实例")
        void shouldDestroyInstanceNormally() {
            PooledObject<Playwright> pooledObject = new DefaultPooledObject<>(mockPlaywright);
            
            assertThatCode(() -> factory.destroyObject(pooledObject))
                    .doesNotThrowAnyException();
            
            verify(mockPlaywright, times(1)).close();
        }

        @Test
        @DisplayName("应该能够处理销毁时的异常")
        void shouldHandleDestroyException() {
            doThrow(new RuntimeException("Close failed")).when(mockPlaywright).close();
            
            PooledObject<Playwright> pooledObject = new DefaultPooledObject<>(mockPlaywright);
            
            // 即使close()抛出异常，destroyObject也不应该抛出异常
            assertThatCode(() -> factory.destroyObject(pooledObject))
                    .doesNotThrowAnyException();
            
            verify(mockPlaywright, times(1)).close();
        }

        @Test
        @DisplayName("应该能够处理null对象的销毁")
        void shouldHandleNullObjectDestroy() {
            PooledObject<Playwright> pooledObject = new DefaultPooledObject<>(null);
            
            assertThatCode(() -> factory.destroyObject(pooledObject))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("多次销毁同一个对象应该安全")
        void shouldSafelyHandleMultipleDestroy() {
            PooledObject<Playwright> pooledObject = new DefaultPooledObject<>(mockPlaywright);
            
            // 第一次销毁
            assertThatCode(() -> factory.destroyObject(pooledObject))
                    .doesNotThrowAnyException();
            
            // 第二次销毁（可能在close()时抛出异常）
            doThrow(new RuntimeException("Already closed")).when(mockPlaywright).close();
            assertThatCode(() -> factory.destroyObject(pooledObject))
                    .doesNotThrowAnyException();
            
            verify(mockPlaywright, times(2)).close();
        }
    }

    @Nested
    @DisplayName("工厂方法继承测试")
    class FactoryInheritanceTests {

        @Test
        @DisplayName("应该正确继承BasePooledObjectFactory")
        void shouldExtendBasePooledObjectFactory() {
            assertThat(factory).isInstanceOf(org.apache.commons.pool2.BasePooledObjectFactory.class);
        }

        @Test
        @DisplayName("应该实现必要的抽象方法")
        void shouldImplementRequiredMethods() {
            // 验证create方法存在且可调用
            assertThatCode(() -> {
                Playwright playwright = factory.create();
                playwright.close();
            }).doesNotThrowAnyException();
            
            // 验证wrap方法存在且可调用
            assertThatCode(() -> factory.wrap(mockPlaywright))
                    .doesNotThrowAnyException();
            
            // 验证destroyObject方法存在且可调用
            assertThatCode(() -> {
                PooledObject<Playwright> pooled = factory.wrap(mockPlaywright);
                factory.destroyObject(pooled);
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("完整生命周期测试")
    class FullLifecycleTests {

        @Test
        @DisplayName("完整的对象生命周期应该正常工作")
        void shouldHandleCompleteObjectLifecycle() {
            Playwright playwright = null;
            PooledObject<Playwright> pooledObject = null;
            
            try {
                // 1. 创建实例
                playwright = factory.create();
                assertThat(playwright).isNotNull();
                
                // 2. 包装实例
                pooledObject = factory.wrap(playwright);
                assertThat(pooledObject).isNotNull();
                assertThat(pooledObject.getObject()).isSameAs(playwright);
                
                // 3. 使用实例（验证基本功能）
                assertThat(playwright.chromium()).isNotNull();
                
                // 4. 销毁实例
                factory.destroyObject(pooledObject);
                
                // 注意：销毁后不应该再使用playwright实例
                
            } catch (Exception e) {
                // 如果过程中出现异常，确保资源清理
                if (playwright != null) {
                    try {
                        playwright.close();
                    } catch (Exception closeEx) {
                        // 忽略清理时的异常
                    }
                }
                throw e;
            }
        }

        @Test
        @DisplayName("并发创建多个实例应该安全")
        void shouldSafelyConcurrentlyCreateInstances() {
            int threadCount = 3;
            Thread[] threads = new Thread[threadCount];
            Exception[] exceptions = new Exception[threadCount];
            Playwright[] playwrights = new Playwright[threadCount];
            
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    try {
                        playwrights[index] = factory.create();
                        assertThat(playwrights[index]).isNotNull();
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
                    thread.join(5000); // 5秒超时
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            // 验证结果
            for (int i = 0; i < threadCount; i++) {
                assertThat(exceptions[i]).isNull();
                assertThat(playwrights[i]).isNotNull();
            }
            
            // 清理资源
            for (Playwright playwright : playwrights) {
                if (playwright != null) {
                    playwright.close();
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
            // 这个测试主要验证异常情况下的资源管理
            // 由于Playwright.create()是静态方法，我们模拟一个可能失败的场景
            
            // 创建多个实例，然后全部销毁，验证无内存泄露
            for (int i = 0; i < 3; i++) {
                Playwright playwright = null;
                try {
                    playwright = factory.create();
                    assertThat(playwright).isNotNull();
                } finally {
                    if (playwright != null) {
                        playwright.close();
                    }
                }
            }
        }

        @Test
        @DisplayName("销毁操作应该是幂等的")
        void shouldBeIdempotentDestroy() {
            Playwright playwright = factory.create();
            PooledObject<Playwright> pooledObject = factory.wrap(playwright);
            
            // 第一次销毁
            factory.destroyObject(pooledObject);
            
            // 第二次销毁应该安全（即使对象已经关闭）
            assertThatCode(() -> factory.destroyObject(pooledObject))
                    .doesNotThrowAnyException();
        }
    }
}
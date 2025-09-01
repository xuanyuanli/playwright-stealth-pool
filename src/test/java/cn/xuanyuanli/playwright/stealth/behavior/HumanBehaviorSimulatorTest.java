package cn.xuanyuanli.playwright.stealth.behavior;

import cn.xuanyuanli.playwright.stealth.config.PlaywrightConfig;
import cn.xuanyuanli.playwright.stealth.manager.PlaywrightManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.*;

/**
 * HumanBehaviorSimulator工具类测试
 *
 * @author xuanyuanli
 */
@DisplayName("人类行为模拟器测试")
@Tag("slow")
class HumanBehaviorSimulatorTest {

    @Nested
    @DisplayName("参数验证和容错测试")
    class ParameterValidationAndRobustnessTests {

        @Test
        @DisplayName("simulate方法应该优雅处理null页面参数")
        void simulateShouldGracefullyHandleNullPage() {
            assertThatCode(() -> {
                HumanBehaviorSimulator.simulate(null);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("simulate方法应该优雅处理null强度参数")
        void simulateShouldGracefullyHandleNullIntensity() {
            // 创建一个简单页面进行测试
            PlaywrightConfig config = new PlaywrightConfig().setHeadless(true);
            
            try (PlaywrightManager manager = new PlaywrightManager(1)) {
                assertThatCode(() -> {
                    manager.execute(config, page -> {
                        page.navigate("about:blank");
                        page.waitForLoadState();
                        
                        // 传入null强度，应该使用默认的NORMAL强度
                        HumanBehaviorSimulator.simulate(page, null);
                    });
                }).doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("quickSimulate方法应该优雅处理null页面")
        void quickSimulateShouldGracefullyHandleNullPage() {
            assertThatCode(() -> {
                HumanBehaviorSimulator.quickSimulate(null);
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("行为模拟功能测试")
    class BehaviorSimulationFunctionalityTests {

        @Test
        @DisplayName("默认模拟应该使用NORMAL强度")
        void defaultSimulateShouldUseNormalIntensity() {
            PlaywrightConfig config = new PlaywrightConfig().setHeadless(true);
            
            try (PlaywrightManager manager = new PlaywrightManager(1)) {
                long startTime = System.currentTimeMillis();
                
                manager.execute(config, page -> {
                    page.navigate("about:blank");
                    page.waitForLoadState();
                    
                    HumanBehaviorSimulator.simulate(page);
                });
                
                long duration = System.currentTimeMillis() - startTime;
                
                // 验证执行时间在合理范围内（考虑页面加载时间）
                // NORMAL强度是1.5-3秒，加上页面加载时间应该在5秒内完成
                assertThat(duration).isLessThan(5000L);
            }
        }

        @Test
        @DisplayName("quickSimulate应该比默认模拟更快完成")
        void quickSimulateShouldBeFasterThanDefault() {
            PlaywrightConfig config = new PlaywrightConfig().setHeadless(true);
            
            try (PlaywrightManager manager = new PlaywrightManager(1)) {
                long quickStartTime = System.currentTimeMillis();
                
                manager.execute(config, page -> {
                    page.navigate("about:blank");
                    page.waitForLoadState();
                    
                    HumanBehaviorSimulator.quickSimulate(page);
                });
                
                long quickDuration = System.currentTimeMillis() - quickStartTime;
                
                // QUICK强度应该在较短时间内完成（0.5-1秒 + 页面加载时间）
                assertThat(quickDuration).isLessThan(3000L);
            }
        }

        @ParameterizedTest
        @EnumSource(BehaviorIntensity.class)
        @DisplayName("所有强度级别的模拟应该正常工作")
        void allIntensityLevelsShouldWork(BehaviorIntensity intensity) {
            PlaywrightConfig config = new PlaywrightConfig().setHeadless(true);
            
            try (PlaywrightManager manager = new PlaywrightManager(1)) {
                assertThatCode(() -> {
                    manager.execute(config, page -> {
                        page.navigate("about:blank");
                        page.waitForLoadState();
                        
                        HumanBehaviorSimulator.simulate(page, intensity);
                        
                        // 验证页面仍然可用
                        assertThat(page.url()).contains("about:blank");
                    });
                }).doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("在包含丰富内容的页面上执行模拟")
        void shouldWorkOnContentRichPage() {
            PlaywrightConfig config = new PlaywrightConfig().setHeadless(true);
            
            try (PlaywrightManager manager = new PlaywrightManager(1)) {
                assertThatCode(() -> {
                    manager.execute(config, page -> {
                        // 创建包含多种元素的页面
                        String htmlContent = """
                            <!DOCTYPE html>
                            <html>
                            <head><title>Test Page</title></head>
                            <body style="height: 3000px; padding: 20px;">
                                <h1>主标题</h1>
                                <h2>副标题</h2>
                                <p>这是一个测试段落，包含了很多文字内容。</p>
                                <div>测试div元素</div>
                                <span>测试span元素</span>
                                <img src="data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgZmlsbD0iIzAwZiIvPjwvc3ZnPg==" alt="测试图片">
                                <p>页面底部的段落</p>
                            </body>
                            </html>
                            """;
                        
                        page.setContent(htmlContent);
                        page.waitForLoadState();
                        
                        // 执行快速模拟以节省测试时间
                        HumanBehaviorSimulator.simulate(page, BehaviorIntensity.QUICK);
                        
                        // 验证页面内容没有被改变
                        assertThat(page.title()).isEqualTo("Test Page");
                        assertThat(page.locator("h1").textContent()).isEqualTo("主标题");
                        assertThat(page.locator("p").first().isVisible()).isTrue();
                    });
                }).doesNotThrowAnyException();
            }
        }
    }

    @Nested
    @DisplayName("时间控制和性能测试")
    class TimeControlAndPerformanceTests {

        @Test
        @DisplayName("QUICK强度应该在预期时间范围内完成")
        void quickIntensityShouldCompleteWithinExpectedTime() {
            PlaywrightConfig config = new PlaywrightConfig().setHeadless(true);
            
            try (PlaywrightManager manager = new PlaywrightManager(1)) {
                manager.execute(config, page -> {
                    page.navigate("about:blank");
                    page.waitForLoadState();
                    
                    long startTime = System.currentTimeMillis();
                    HumanBehaviorSimulator.simulate(page, BehaviorIntensity.QUICK);
                    long actualDuration = System.currentTimeMillis() - startTime;
                    
                    // QUICK强度：500-1000ms，允许一定的误差
                    assertThat(actualDuration).isBetween(400L, 1500L);
                });
            }
        }

        @Test
        @DisplayName("NORMAL强度应该比QUICK强度耗时更长")
        void normalIntensityShouldTakeLongerThanQuick() {
            PlaywrightConfig config = new PlaywrightConfig().setHeadless(true);
            
            try (PlaywrightManager manager = new PlaywrightManager(1)) {
                manager.execute(config, page -> {
                    page.navigate("about:blank");
                    page.waitForLoadState();
                    
                    // 测试QUICK强度
                    long quickStart = System.currentTimeMillis();
                    HumanBehaviorSimulator.simulate(page, BehaviorIntensity.QUICK);
                    long quickDuration = System.currentTimeMillis() - quickStart;
                    
                    // 短暂等待，避免连续操作
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    // 测试NORMAL强度
                    long normalStart = System.currentTimeMillis();
                    HumanBehaviorSimulator.simulate(page, BehaviorIntensity.NORMAL);
                    long normalDuration = System.currentTimeMillis() - normalStart;
                    
                    // NORMAL应该比QUICK耗时更长
                    assertThat(normalDuration).isGreaterThan(quickDuration);
                });
            }
        }

        @Test
        @DisplayName("THOROUGH强度应该在预期时间范围内完成")
        void thoroughIntensityShouldCompleteWithinExpectedTime() {
            PlaywrightConfig config = new PlaywrightConfig().setHeadless(true);
            
            try (PlaywrightManager manager = new PlaywrightManager(1)) {
                manager.execute(config, page -> {
                    page.navigate("about:blank");
                    page.waitForLoadState();
                    
                    long startTime = System.currentTimeMillis();
                    HumanBehaviorSimulator.simulate(page, BehaviorIntensity.THOROUGH);
                    long actualDuration = System.currentTimeMillis() - startTime;
                    
                    // THOROUGH强度：3000-6000ms，允许一定的误差
                    assertThat(actualDuration).isBetween(2800L, 7000L);
                });
            }
        }
    }

    @Nested
    @DisplayName("并发和稳定性测试")
    class ConcurrencyAndStabilityTests {

        @Test
        @DisplayName("并发执行模拟应该稳定工作")
        void concurrentSimulationShouldWorkStably() throws InterruptedException {
            PlaywrightConfig config = new PlaywrightConfig().setHeadless(true);
            
            try (PlaywrightManager manager = new PlaywrightManager(3)) {
                int threadCount = 3;
                Thread[] threads = new Thread[threadCount];
                Exception[] exceptions = new Exception[threadCount];
                
                for (int i = 0; i < threadCount; i++) {
                    final int index = i;
                    threads[i] = new Thread(() -> {
                        try {
                            manager.execute(config, page -> {
                                page.navigate("about:blank");
                                page.waitForLoadState();
                                
                                // 使用QUICK强度减少测试时间
                                HumanBehaviorSimulator.simulate(page, BehaviorIntensity.QUICK);
                                
                                // 验证页面状态
                                assertThat(page.url()).contains("about:blank");
                            });
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
                    thread.join(8000); // 8秒超时
                }
                
                // 验证没有异常
                for (int i = 0; i < threadCount; i++) {
                    if (exceptions[i] != null) {
                        throw new AssertionError("Thread " + i + " failed", exceptions[i]);
                    }
                }
            }
        }

        @Test
        @DisplayName("重复执行模拟应该保持稳定")
        void repeatedSimulationShouldRemainStable() {
            PlaywrightConfig config = new PlaywrightConfig().setHeadless(true);
            
            try (PlaywrightManager manager = new PlaywrightManager(1)) {
                assertThatCode(() -> {
                    for (int i = 0; i < 5; i++) {
                        manager.execute(config, page -> {
                            page.navigate("about:blank");
                            page.waitForLoadState();
                            
                            HumanBehaviorSimulator.simulate(page, BehaviorIntensity.QUICK);
                            
                            // 验证每次执行后页面状态正常
                            assertThat(page.url()).contains("about:blank");
                        });
                    }
                }).doesNotThrowAnyException();
            }
        }
    }

    @Nested
    @DisplayName("安全性测试")
    class SafetyTests {

        @Test
        @DisplayName("模拟执行不应该改变页面URL")
        void simulationShouldNotChangePageUrl() {
            PlaywrightConfig config = new PlaywrightConfig().setHeadless(true);
            
            try (PlaywrightManager manager = new PlaywrightManager(1)) {
                manager.execute(config, page -> {
                    page.navigate("about:blank");
                    page.waitForLoadState();
                    
                    String originalUrl = page.url();
                    
                    HumanBehaviorSimulator.simulate(page, BehaviorIntensity.NORMAL);
                    
                    // URL不应该改变
                    assertThat(page.url()).isEqualTo(originalUrl);
                });
            }
        }

        @Test
        @DisplayName("模拟执行不应该改变页面内容")
        void simulationShouldNotChangePageContent() {
            PlaywrightConfig config = new PlaywrightConfig().setHeadless(true);
            
            try (PlaywrightManager manager = new PlaywrightManager(1)) {
                manager.execute(config, page -> {
                    String testContent = "<html><head><title>Test</title></head><body><h1 id=\"test\">原始内容</h1></body></html>";
                    page.setContent(testContent);
                    page.waitForLoadState();
                    
                    String originalTitle = page.title();
                    String originalHeading = page.locator("#test").textContent();
                    
                    HumanBehaviorSimulator.simulate(page, BehaviorIntensity.THOROUGH);
                    
                    // 页面内容不应该改变
                    assertThat(page.title()).isEqualTo(originalTitle);
                    assertThat(page.locator("#test").textContent()).isEqualTo(originalHeading);
                });
            }
        }

        @Test
        @DisplayName("模拟过程中的异常不应该中断主流程")
        void exceptionsDuringSimulationShouldNotInterruptMainFlow() {
            PlaywrightConfig config = new PlaywrightConfig().setHeadless(true);
            
            try (PlaywrightManager manager = new PlaywrightManager(1)) {
                assertThatCode(() -> {
                    manager.execute(config, page -> {
                        page.navigate("about:blank");
                        page.waitForLoadState();
                        
                        // 即使在页面加载后立即关闭（模拟异常情况），也不应该抛出异常
                        HumanBehaviorSimulator.simulate(page, BehaviorIntensity.QUICK);
                        
                        // 验证仍然可以继续操作
                        assertThat(page.url()).isNotNull();
                    });
                }).doesNotThrowAnyException();
            }
        }
    }

    @Nested
    @DisplayName("集成测试")
    class IntegrationTests {

        @Test
        @DisplayName("与反检测脚本集成测试")
        void shouldIntegrateWithStealthScripts() {
            PlaywrightConfig config = new PlaywrightConfig()
                .setHeadless(true)
                .setStealthMode(cn.xuanyuanli.playwright.stealth.config.StealthMode.LIGHT);
            
            try (PlaywrightManager manager = new PlaywrightManager(1)) {
                assertThatCode(() -> {
                    manager.execute(config, page -> {
                        page.navigate("about:blank");
                        page.waitForLoadState();
                        
                        // 验证反检测脚本生效
                        Object webdriver = page.evaluate("navigator.webdriver");
                        assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue();
                        
                        // 执行行为模拟
                        HumanBehaviorSimulator.simulate(page, BehaviorIntensity.NORMAL);
                        
                        // 验证反检测脚本仍然生效
                        Object webdriverAfter = page.evaluate("navigator.webdriver");
                        assertThat(webdriverAfter == null || webdriverAfter.equals(false) || "undefined".equals(webdriverAfter.toString())).isTrue();
                    });
                }).doesNotThrowAnyException();
            }
        }
    }
}
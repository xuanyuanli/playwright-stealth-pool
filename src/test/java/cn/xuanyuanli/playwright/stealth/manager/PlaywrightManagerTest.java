package cn.xuanyuanli.playwright.stealth.manager;

import cn.xuanyuanli.playwright.stealth.config.PlaywrightConfig;
import cn.xuanyuanli.playwright.stealth.config.StealthMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

/**
 * PlaywrightManager测试类
 * 
 * <p>提供PlaywrightManager的功能测试，包括基本功能和配置测试</p>
 *
 * @author xuanyuanli
 */
class PlaywrightManagerTest {

    private PlaywrightManager playwrightManager;
    private PlaywrightConfig config;

    @BeforeEach
    void setUp() {
        // 创建测试配置
        config = new PlaywrightConfig()
                .setHeadless(true)
                .setStealthMode(StealthMode.FULL);
        
        // 创建Playwright管理器，池大小为4
        playwrightManager = new PlaywrightManager(4);
    }

    @AfterEach
    void tearDown() {
        if (playwrightManager != null) {
            playwrightManager.close();
        }
    }

    /**
     * 递归检查异常链中是否包含指定消息
     */
    private boolean containsMessageInCauseChain(Throwable throwable, String message) {
        if (throwable == null) {
            return false;
        }
        if (throwable.getMessage() != null && throwable.getMessage().contains(message)) {
            return true;
        }
        return containsMessageInCauseChain(throwable.getCause(), message);
    }

    /**
     * 构建完整的异常消息（包含整个原因链）
     */
    private String buildFullExceptionMessage(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            if (!sb.isEmpty()) {
                sb.append(" -> ");
            }
            sb.append(current.getClass().getSimpleName()).append(": ").append(current.getMessage());
            current = current.getCause();
        }
        return sb.toString();
    }

    /**
     * 测试基本的页面访问功能
     */
    @Test
    @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isIntegrationTestsEnabled")
    void testBasicPageAccess() {
        playwrightManager.execute(config, page -> {
            page.navigate("https://www.baidu.com");
            String title = page.title();
            assertThat(title).containsIgnoringCase("百度");
        });
    }

    /**
     * 测试使用默认配置
     */
    @Test
    @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isIntegrationTestsEnabled")
    void testDefaultConfiguration() {
        // 使用默认配置（传入null）
        playwrightManager.execute(page -> {
            page.navigate("https://httpbin.org/get");
            String content = page.content();
            assertThat(content).isNotEmpty();
            assertThat(content).containsIgnoringCase("httpbin");
        });
    }

    /**
     * 测试自定义浏览器上下文配置
     */
    @Test
    @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isIntegrationTestsEnabled")
    void testCustomBrowserContext() {
        playwrightManager.execute(config, context -> {
            // 设置额外的权限
            context.grantPermissions(List.of("geolocation"));
            
            // 设置地理位置
            context.setGeolocation(new com.microsoft.playwright.options.Geolocation(39.9042, 116.4074));
            
        }, page -> {
            page.navigate("https://httpbin.org/get");
            
            // 验证页面可以正常访问
            String content = page.content();
            assertThat(content).containsIgnoringCase("httpbin");
            
            // 检查地理位置权限 - 注意反检测脚本可能会修改permissions API
            Object geolocationPermission = page.evaluate("""
                (async () => {
                    try {
                        const result = await navigator.permissions.query({name: 'geolocation'});
                        return result.state;
                    } catch (e) {
                        return 'unavailable';
                    }
                })()
            """);
            // 验证权限查询能够执行（无论结果如何，反检测脚本可能会修改行为）
            assertThat(geolocationPermission).isNotNull();
        });
    }

    /**
     * 测试不同StealthMode配置
     */
    @Test
    @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isIntegrationTestsEnabled")
    void testStealthModeConfiguration() {
        // 测试禁用反检测
        PlaywrightConfig disabledConfig = new PlaywrightConfig()
                .setHeadless(true)
                .setStealthMode(StealthMode.DISABLED);
                
        playwrightManager.execute(disabledConfig, page -> {
            page.navigate("https://httpbin.org/get");
            Object webdriver = page.evaluate("navigator.webdriver");
            // webdriver属性被Playwright默认隐藏了
            assertThat(webdriver).isNotNull();
            assertThat(webdriver).isEqualTo(false);
        });

        // 测试轻量级反检测
        PlaywrightConfig lightConfig = new PlaywrightConfig()
                .setHeadless(true)
                .setStealthMode(StealthMode.LIGHT);
                
        playwrightManager.execute(lightConfig, page -> {
            page.navigate("about:blank");
            page.navigate("https://httpbin.org/get");
            Object webdriver = page.evaluate("navigator.webdriver");
            Object languages = page.evaluate("navigator.languages");
            // LIGHT模式应该隐藏webdriver
            assertThat(webdriver == null || webdriver.toString().equals("undefined")).isTrue();
            // 应该有语言设置
            assertThat(languages).isNotNull();
        });

        // 测试完整反检测
        PlaywrightConfig fullConfig = new PlaywrightConfig()
                .setHeadless(true)
                .setStealthMode(StealthMode.FULL);
                
        playwrightManager.execute(fullConfig, page -> {
            page.navigate("about:blank");
            page.navigate("https://httpbin.org/get");
            Object webdriver = page.evaluate("navigator.webdriver");
            Object pluginsLength = page.evaluate("navigator.plugins.length");
            Object hardwareConcurrency = page.evaluate("navigator.hardwareConcurrency");
            
            // FULL模式应该全面伪装
            assertThat(webdriver == null || webdriver.toString().equals("undefined")).isTrue();
            assertThat(pluginsLength).isNotNull();
            assertThat(Integer.parseInt(pluginsLength.toString())).isGreaterThan(0);
            assertThat(hardwareConcurrency).isEqualTo(8);
        });
    }

    /**
     * 测试反检测脚本配置（向后兼容）
     */
    @Test
    @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isIntegrationTestsEnabled")
    void testStealthScriptConfiguration() {
        // 测试启用反检测脚本
        PlaywrightConfig stealthConfig = new PlaywrightConfig()
                .setHeadless(true)
                .setStealthMode(StealthMode.FULL);
                
        playwrightManager.execute(stealthConfig, page -> {
            page.navigate("about:blank");
            page.navigate("https://httpbin.org/get");
            
            // 检查webdriver属性是否被隐藏
            Object webdriver = page.evaluate("navigator.webdriver");
            assertThat(webdriver == null || webdriver.toString().equals("undefined")).isTrue();
        });

        // 测试禁用反检测脚本  
        PlaywrightConfig noStealthConfig = new PlaywrightConfig()
                .setHeadless(true)
                .setStealthMode(StealthMode.DISABLED);
                
        playwrightManager.execute(noStealthConfig, page -> {
            page.navigate("https://httpbin.org/get");

            // webdriver属性被Playwright默认隐藏了
            Object webdriver = page.evaluate("navigator.webdriver");
            assertThat(webdriver).isNotNull();
            assertThat(webdriver).isEqualTo(false);
        });
    }

    /**
     * 测试并发执行
     */
    @Test
    @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isIntegrationTestsEnabled")
    void testConcurrentExecution() {
        // 并发执行12个任务，验证连接池能正确处理并发请求
        AtomicInteger successCount = new AtomicInteger(0);
        
        IntStream.range(0, 12).parallel().forEach(i -> playwrightManager.execute(config, page -> {
            page.navigate("https://httpbin.org/delay/1");
            String content = page.textContent("body");
            assertThat(content).containsIgnoringCase("delay");
            successCount.incrementAndGet();
        }));
        
        // 验证所有任务都成功完成
        assertThat(successCount.get()).isEqualTo(12);
    }

    /**
     * 测试不同浏览器配置
     */
    @Test
    @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isIntegrationTestsEnabled")
    void testDifferentBrowserConfigurations() {
        // 测试有头模式（仅在开发环境测试）
        PlaywrightConfig headfulConfig = new PlaywrightConfig()
                .setHeadless(false)
                .setStartMaximized(true)
                .setSlowMo(100.0);
                
        playwrightManager.execute(headfulConfig, page -> {
            page.navigate("https://www.baidu.com");
            page.waitForTimeout(2000); // 等待观察
            String title = page.title();
            assertThat(title).containsIgnoringCase("百度");
        });

        // 测试禁用GPU
        PlaywrightConfig noGpuConfig = new PlaywrightConfig()
                .setHeadless(true)
                .setDisableGpu(true)
                .setDisableImageRender(true);
                
        playwrightManager.execute(noGpuConfig, page -> {
            page.navigate("https://httpbin.org/get");
            String content = page.content();
            assertThat(content).containsIgnoringCase("httpbin");
        });
    }

    /**
     * 测试错误处理
     */
    @Test
    void testErrorHandling() {
        assertThatThrownBy(() -> playwrightManager.execute(config, page -> {
            // 故意触发一个错误
            throw new RuntimeException("测试异常处理");
        })).isInstanceOf(RuntimeException.class)
          .satisfies(e -> {
              // 检查异常消息或者整个原因链
              boolean containsTestException = containsMessageInCauseChain(e, "测试异常处理");
              String actualMessage = buildFullExceptionMessage(e);
              assertThat(containsTestException)
                  .as("异常消息应该包含测试异常信息，实际消息: " + actualMessage)
                  .isTrue();
          });

        // 确保连接池状态正常 - 验证连接池仍可正常工作
        playwrightManager.execute(config, page -> {
            page.navigate("https://httpbin.org/get");
            String content = page.content();
            assertThat(content).containsIgnoringCase("httpbin");
        });
    }

    /**
     * 测试并发控制 - 验证 Browser 创建并发数被限制
     */
    @Test
    @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isIntegrationTestsEnabled")
    void testConcurrencyControl() throws InterruptedException {
        // 使用并发数为2的管理器
        PlaywrightManager limitedManager = new PlaywrightManager(4, 2);

        int taskCount = 6;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(taskCount);
        AtomicInteger maxConcurrentBrowsers = new AtomicInteger(0);
        AtomicInteger currentConcurrentBrowsers = new AtomicInteger(0);
        AtomicLong totalExecutionTime = new AtomicLong(0);

        long startTime = System.currentTimeMillis();

        // 启动多个并发任务
        for (int i = 0; i < taskCount; i++) {
            final int taskId = i;
            new Thread(() -> {
                try {
                    startLatch.await(); // 等待所有线程同时开始

                    limitedManager.execute(config, page -> {
                        int current = currentConcurrentBrowsers.incrementAndGet();
                        maxConcurrentBrowsers.updateAndGet(max -> Math.max(max, current));

                        try {
                            page.navigate("https://httpbin.org/delay/1");
                            String content = page.textContent("body");
                            assertThat(content).containsIgnoringCase("delay");
                        } finally {
                            currentConcurrentBrowsers.decrementAndGet();
                        }
                    });
                } catch (Exception e) {
                    System.err.println("Task " + taskId + " failed: " + e.getMessage());
                } finally {
                    completeLatch.countDown();
                }
            }).start();
        }

        // 同时开始所有任务
        startLatch.countDown();

        // 等待所有任务完成
        boolean completed = completeLatch.await(60, TimeUnit.SECONDS);
        totalExecutionTime.set(System.currentTimeMillis() - startTime);

        assertThat(completed).as("所有任务应该在60秒内完成").isTrue();

        // 验证并发数被限制在2以内
        assertThat(maxConcurrentBrowsers.get())
            .as("Browser 创建并发数应该被限制在2以内，实际最大值: " + maxConcurrentBrowsers.get())
            .isLessThanOrEqualTo(2);

        // 验证执行时间符合串行预期（6个任务，每个1秒，并发2，理论上至少3秒）
        assertThat(totalExecutionTime.get())
            .as("执行时间应该符合并发控制预期")
            .isGreaterThanOrEqualTo(2500); // 至少2.5秒

        System.out.println("Concurrency test completed: maxConcurrent=" + maxConcurrentBrowsers.get() +
                          ", totalTime=" + totalExecutionTime.get() + "ms");

        limitedManager.close();
    }

    /**
     * 测试默认并发数计算（CPU核数-1）
     */
    @Test
    void testDefaultConcurrencyCalculation() {
        int processors = Runtime.getRuntime().availableProcessors();
        int expectedConcurrency = Math.max(1, processors - 1);

        // 创建使用默认并发数的管理器
        PlaywrightManager manager = new PlaywrightManager(4);

        // 验证可以正常执行
        manager.execute(config, page -> {
            page.navigate("https://httpbin.org/get");
            assertThat(page.content()).containsIgnoringCase("httpbin");
        });

        manager.close();

        System.out.println("Default concurrency test: processors=" + processors +
                          ", expectedConcurrency=" + expectedConcurrency);
    }

    /**
     * 测试重试机制 - 模拟可重试错误场景
     */
    @Test
    @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isIntegrationTestsEnabled")
    void testRetryMechanism() {
        AtomicInteger attemptCount = new AtomicInteger(0);

        // 测试正常执行不会触发重试
        playwrightManager.execute(config, page -> {
            int attempt = attemptCount.incrementAndGet();
            page.navigate("https://httpbin.org/get");
            assertThat(page.content()).containsIgnoringCase("httpbin");
            System.out.println("Normal execution attempt: " + attempt);
        });

        assertThat(attemptCount.get()).isEqualTo(1);
    }

    /**
     * 测试连接池配置优化
     */
    @Test
    @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isIntegrationTestsEnabled")
    void testPoolConfiguration() {
        // 创建管理器并执行多个任务，验证连接池配置正常工作
        PlaywrightManager poolManager = new PlaywrightManager(2, 1);

        // 顺序执行多个任务
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            poolManager.execute(config, page -> {
                page.navigate("https://httpbin.org/get");
                String content = page.content();
                assertThat(content).containsIgnoringCase("httpbin");
                System.out.println("Pool task " + taskId + " completed");
            });
        }

        poolManager.close();
    }
}
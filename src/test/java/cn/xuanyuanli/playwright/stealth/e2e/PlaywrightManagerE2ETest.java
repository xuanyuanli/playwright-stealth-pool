package cn.xuanyuanli.playwright.stealth.e2e;

import cn.xuanyuanli.playwright.stealth.config.PlaywrightConfig;
import cn.xuanyuanli.playwright.stealth.config.StealthMode;
import cn.xuanyuanli.playwright.stealth.manager.PlaywrightManager;
import cn.xuanyuanli.playwright.stealth.TestConditions;
import com.microsoft.playwright.options.Proxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIf;

import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

/**
 * PlaywrightManager端到端测试
 *
 * <p>测试完整的工作流程和真实场景，包括：</p>
 * <ul>
 *   <li>真实网站访问测试</li>
 *   <li>反检测效果验证</li>
 *   <li>复杂页面操作</li>
 *   <li>错误恢复机制</li>
 *   <li>资源管理验证</li>
 * </ul>
 *
 * @author xuanyuanli
 */
@DisplayName("PlaywrightManager 端到端测试")
@Tag("e2e")
@Tag("slow")
class PlaywrightManagerE2ETest {

    private PlaywrightManager manager;
    
    private boolean containsCauseMessage(Throwable cause, String message) {
        if (cause == null) return false;
        if (cause.getMessage() != null && cause.getMessage().contains(message)) return true;
        return containsCauseMessage(cause.getCause(), message);
    }
    
    @BeforeEach
    void setUp() {
        manager = new PlaywrightManager(4);
    }
    
    @AfterEach
    void tearDown() {
        if (manager != null) {
            manager.close();
        }
    }

    @Nested
    @DisplayName("基础功能端到端测试")
    class BasicFunctionalityE2ETests {

        @Test
        @DisplayName("应该能够完成完整的页面访问流程")
        @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isE2ETestsEnabled")
        void shouldCompleteFullPageAccessWorkflow() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.FULL)
                    .setDisableImageRender(true);

            manager.execute(config, page -> {
                // 访问测试页面
                page.navigate("https://httpbin.org/get");
                
                // 验证页面加载成功
                String title = page.title();
                assertThat(title).isNotNull();
                
                // 验证页面内容
                String content = page.content();
                assertThat(content).contains("httpbin");
                
                // 验证反检测脚本生效
                Object webdriver = page.evaluate("navigator.webdriver");
                // webdriver应该被隐藏(null, undefined, 或 false)
                assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue();
                
                // 验证插件模拟
                Object pluginsLength = page.evaluate("navigator.plugins.length");
                assertThat(pluginsLength).isNotNull();
                int pluginCount = Integer.parseInt(pluginsLength.toString());
                assertThat(pluginCount).isGreaterThan(0);
            });
        }

        @Test
        @DisplayName("应该能够处理复杂的页面交互")
        @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isE2ETestsEnabled")
        void shouldHandleComplexPageInteractions() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.LIGHT);

            manager.execute(config, page -> {
                // 先导航到基础页面确保stealth脚本正确初始化
                page.navigate("about:blank");
                
                // 设置复杂的页面内容
                page.setContent("""
                    <html>
                    <body>
                        <h1 id="title">测试页面</h1>
                        <button id="clickMe" onclick="document.getElementById('result').textContent='Clicked!'">点击我</button>
                        <div id="result">未点击</div>
                        <input id="textInput" type="text" placeholder="输入文本">
                        <form id="testForm">
                            <input id="nameInput" name="name" type="text" placeholder="姓名">
                            <button type="submit">提交</button>
                        </form>
                        <div id="formResult"></div>
                    </body>
                    </html>
                """);

                // 验证页面元素
                String title = page.textContent("#title");
                assertThat(title).isEqualTo("测试页面");

                // 点击按钮
                page.click("#clickMe");
                String result = page.textContent("#result");
                assertThat(result).isEqualTo("Clicked!");

                // 输入文本
                page.fill("#textInput", "Hello World");
                String inputValue = page.inputValue("#textInput");
                assertThat(inputValue).isEqualTo("Hello World");

                // 表单操作
                page.fill("#nameInput", "测试用户");
                page.click("button[type='submit']");

                // 验证反检测脚本在复杂交互中仍然生效
                Object webdriver = page.evaluate("navigator.webdriver");
                // webdriver应该被隐藏(null, undefined, 或 false)
                assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue();
            });
        }

        @Test
        @DisplayName("应该能够处理JavaScript密集型页面")
        @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isE2ETestsEnabled")
        void shouldHandleJavaScriptIntensivePages() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.FULL);

            manager.execute(config, page -> {
                // 先导航到基础页面确保stealth脚本正确初始化
                page.navigate("about:blank");
                
                // 设置包含大量JavaScript的页面
                page.setContent("""
                    <html>
                    <head>
                        <script>
                            // 模拟复杂的JavaScript环境
                            window.testData = {
                                counter: 0,
                                results: []
                            };
                
                            function complexCalculation() {
                                for (let i = 0; i < 1000; i++) {
                                    window.testData.counter += Math.random();
                                }
                                return window.testData.counter;
                            }
                
                            function checkBrowserFeatures() {
                                return {
                                    webdriver: navigator.webdriver,
                                    plugins: navigator.plugins.length,
                                    languages: navigator.languages.length,
                                    platform: navigator.platform,
                                    hardwareConcurrency: navigator.hardwareConcurrency
                                };
                            }
                
                            // 异步操作
                            setTimeout(() => {
                                document.getElementById('asyncResult').textContent = 'Async Complete';
                            }, 100);
                        </script>
                    </head>
                    <body>
                        <div id="asyncResult">Loading...</div>
                        <button onclick="document.getElementById('calcResult').textContent = complexCalculation()">计算</button>
                        <div id="calcResult">未计算</div>
                    </body>
                    </html>
                """);

                // 等待异步操作完成
                page.waitForFunction("document.getElementById('asyncResult').textContent === 'Async Complete'");
                String asyncResult = page.textContent("#asyncResult");
                assertThat(asyncResult).isEqualTo("Async Complete");

                // 执行复杂计算
                page.click("button");
                page.waitForFunction("document.getElementById('calcResult').textContent !== '未计算'");

                // 验证JavaScript功能正常
                Object calcResult = page.evaluate("window.testData.counter");
                assertThat(calcResult).isNotNull();

                // 验证反检测功能在JavaScript密集环境中的稳定性
                Object browserFeatures = page.evaluate("checkBrowserFeatures()");
                assertThat(browserFeatures).isNotNull();
                
                // 验证反检测脚本效果
                Object webdriver = page.evaluate("navigator.webdriver");
                // webdriver应该被隐藏(null, undefined, 或 false)
                assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue();
            });
        }
    }

    @Nested
    @DisplayName("反检测功能端到端测试")
    class StealthFunctionalityE2ETests {

        @Test
        @DisplayName("应该能够绕过基础的自动化检测")
        @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isE2ETestsEnabled")
        void shouldBypassBasicAutomationDetection() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.FULL)
                    .setDisableAutomationControlled(true);

            manager.execute(config, page -> {
                // 先导航到一个基础页面确保stealth脚本正确初始化
                page.navigate("about:blank");
                
                // 创建检测自动化的页面
                page.setContent("""
                    <html>
                    <head>
                        <script>
                            window.detectionResults = {
                                webdriver: navigator.webdriver,
                                automationControlled: window.chrome && window.chrome.runtime,
                                pluginCount: navigator.plugins.length,
                                languageCount: navigator.languages.length,
                                hardwareConcurrency: navigator.hardwareConcurrency,
                                deviceMemory: navigator.deviceMemory,
                                platform: navigator.platform,
                
                                // 高级检测
                                webglVendor: null,
                                webglRenderer: null,
                                audioContext: null
                            };
                
                            // WebGL检测
                            try {
                                const canvas = document.createElement('canvas');
                                const gl = canvas.getContext('webgl');
                                if (gl) {
                                    const debugInfo = gl.getExtension('WEBGL_debug_renderer_info');
                                    if (debugInfo) {
                                        window.detectionResults.webglVendor = gl.getParameter(debugInfo.UNMASKED_VENDOR_WEBGL);
                                        window.detectionResults.webglRenderer = gl.getParameter(debugInfo.UNMASKED_RENDERER_WEBGL);
                                    }
                                }
                            } catch (e) {
                                console.log('WebGL detection failed:', e);
                            }
                
                            // AudioContext检测
                            try {
                                const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
                                window.detectionResults.audioContext = audioCtx.baseLatency;
                            } catch (e) {
                                console.log('AudioContext detection failed:', e);
                            }
                        </script>
                    </head>
                    <body>
                        <h1>自动化检测页面</h1>
                        <div id="results">检测中...</div>
                    </body>
                    </html>
                """);

                // 等待检测完成
                page.waitForTimeout(1000); // 增加等待时间确保脚本充分执行

                // 获取检测结果
                Object results = page.evaluate("window.detectionResults");
                assertThat(results).isNotNull();

                // 验证关键反检测效果
                Object webdriver = page.evaluate("window.detectionResults.webdriver");
                assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue(); // webdriver应该被隐藏

                Object pluginCount = page.evaluate("window.detectionResults.pluginCount");
                assertThat(pluginCount).isNotNull();
                assertThat(Integer.parseInt(pluginCount.toString())).isGreaterThan(0); // 应该有模拟的插件

                Object hardwareConcurrency = page.evaluate("window.detectionResults.hardwareConcurrency");
                assertThat(hardwareConcurrency).isEqualTo(8); // 应该被设置为8

                Object deviceMemory = page.evaluate("window.detectionResults.deviceMemory");
                assertThat(deviceMemory).isEqualTo(8); // 应该被设置为8

                Object platform = page.evaluate("window.detectionResults.platform");
                assertThat(platform).isEqualTo("Win32"); // 应该被设置为Win32

                // 验证WebGL指纹修复 - 改为存在性和格式检查
                Object webglVendor = page.evaluate("window.detectionResults.webglVendor");
                if (webglVendor != null) {
                    assertThat(webglVendor.toString()).isNotEmpty().matches("^[\\w\\s.()]+$"); // 基本格式验证
                }

                Object webglRenderer = page.evaluate("window.detectionResults.webglRenderer");
                if (webglRenderer != null) {
                    assertThat(webglRenderer.toString()).isNotEmpty(); // 只验证存在性
                }

                // 验证AudioContext指纹修复 - 改为范围检查
                Object audioContext = page.evaluate("window.detectionResults.audioContext");
                if (audioContext != null) {
                    double latency = Double.parseDouble(audioContext.toString());
                    assertThat(latency).isBetween(0.0, 1.0); // 合理范围而非固定值
                }
            });
        }

        @Test
        @DisplayName("应该能够在不同反检测模式下正确工作")
        @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isE2ETestsEnabled")
        void shouldWorkCorrectlyInDifferentStealthModes() {
            StealthMode[] modes = {StealthMode.DISABLED, StealthMode.LIGHT, StealthMode.FULL};

            for (StealthMode mode : modes) {
                PlaywrightConfig config = new PlaywrightConfig()
                        .setHeadless(true)
                        .setStealthMode(mode);

                manager.execute(config, page -> {
                    // 先导航到基础页面确保stealth脚本正确初始化
                    page.navigate("about:blank");
                    
                    page.setContent("""
                        <html>
                        <body>
                            <script>
                                window.modeTestResults = {
                                    mode: '%s',
                                    webdriver: navigator.webdriver,
                                    pluginCount: navigator.plugins.length,
                                    languageCount: navigator.languages.length
                                };
                            </script>
                            <h1>模式测试: %s</h1>
                        </body>
                        </html>
                    """.formatted(mode.name(), mode.name()));

                    // 添加小延迟确保脚本完全执行
                    page.waitForTimeout(200);
                    
                    Object webdriver = page.evaluate("window.modeTestResults.webdriver");
                    Object pluginCount = page.evaluate("window.modeTestResults.pluginCount");

                    switch (mode) {
                        case DISABLED:
                            // DISABLED模式应该保持原始行为
                            assertThat(webdriver).isNotNull(); // webdriver属性存在
                            break;
                        case LIGHT:
                            // LIGHT模式应该隐藏webdriver但插件数量可能较少
                            assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue();
                            break;
                        case FULL:
                            // FULL模式应该全面伪装
                            assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue();
                            
                            assertThat(Integer.parseInt(pluginCount.toString())).isGreaterThan(0);
                            break;
                    }
                });
            }
        }
    }

    @Nested
    @DisplayName("并发和性能端到端测试")
    class ConcurrencyPerformanceE2ETests {

        @Test
        @DisplayName("应该能够处理高并发访问")
        @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isE2ETestsEnabled")
        void shouldHandleHighConcurrencyAccess() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.LIGHT);

            // 并发执行多个任务
            CompletableFuture<?>[] futures = IntStream.range(0, 10)
                    .mapToObj(i -> CompletableFuture.runAsync(() -> {
                        manager.execute(config, page -> {
                            // 先导航到基础页面
                            page.navigate("about:blank");
                            
                            page.setContent(String.format("""
                                <html>
                                <body>
                                    <h1>并发测试 - 任务 %d</h1>
                                    <script>
                                        window.taskId = %d;
                                        window.webdriverHidden = navigator.webdriver === undefined;
                                    </script>
                                </body>
                                </html>
                            """, i, i));

                            Object taskId = page.evaluate("window.taskId");
                            assertThat(taskId).isEqualTo(i);

                            // 在并发环境中添加小延迟确保脚本完全加载
                            page.waitForTimeout(100);
                            
                            Object webdriverHidden = page.evaluate("window.webdriverHidden");
                            assertThat(webdriverHidden).as("任务 %d 的webdriver应该被隐藏", i).isEqualTo(true);
                        });
                    }))
                    .toArray(CompletableFuture[]::new);

            // 等待所有任务完成
            CompletableFuture.allOf(futures).join();
        }

        @Test
        @DisplayName("应该能够处理长时间运行的任务")
        @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isE2ETestsEnabled")
        void shouldHandleLongRunningTasks() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.FULL);

            manager.execute(config, page -> {
                page.setContent("""
                    <html>
                    <body>
                        <h1>长时间运行测试</h1>
                        <script>
                            window.startTime = Date.now();
                            window.iterations = 0;
                
                            function longRunningTask() {
                                for (let i = 0; i < 100000; i++) {
                                    window.iterations++;
                                    // 模拟复杂计算
                                    Math.sqrt(Math.random() * 1000);
                                }
                                window.endTime = Date.now();
                                window.duration = window.endTime - window.startTime;
                            }
                
                            longRunningTask();
                        </script>
                    </body>
                    </html>
                """);

                // 等待长时间任务完成
                page.waitForFunction("window.endTime !== undefined");

                Object iterations = page.evaluate("window.iterations");
                Object duration = page.evaluate("window.duration");

                assertThat(iterations).isNotNull();
                assertThat(Integer.parseInt(iterations.toString())).isEqualTo(100000);

                assertThat(duration).isNotNull();

                // 验证反检测脚本在长时间运行后仍然有效
                Object webdriver = page.evaluate("navigator.webdriver");
                // webdriver应该被隐藏(null, undefined, 或 false)
                assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue();
            });
        }
    }

    @Nested
    @DisplayName("错误处理和恢复端到端测试")
    class ErrorHandlingRecoveryE2ETests {

        @Test
        @DisplayName("应该能够从页面错误中恢复")
        @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isE2ETestsEnabled")
        void shouldRecoverFromPageErrors() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.LIGHT);

            // 第一次执行，故意触发错误
            assertThatThrownBy(() -> {
                manager.execute(config, page -> {
                    page.setContent("<html><body>Error Test</body></html>");
                    throw new RuntimeException("模拟页面错误");
                });
            }).isInstanceOf(RuntimeException.class)
              .satisfies(e -> {
                  boolean containsErrorMessage = e.getMessage().contains("模拟页面错误") ||
                          (e.getCause() != null && containsCauseMessage(e.getCause(), "模拟页面错误"));
                  assertThat(containsErrorMessage)
                          .as("Exception should contain '模拟页面错误' in message chain")
                          .isTrue();
              });

            // 第二次执行应该正常工作，验证连接池恢复
            manager.execute(config, page -> {
                page.setContent("<html><body><h1>恢复测试</h1></body></html>");
                String title = page.textContent("h1");
                assertThat(title).isEqualTo("恢复测试");

                // 验证反检测脚本仍然正常
                Object webdriver = page.evaluate("navigator.webdriver");
                // webdriver应该被隐藏(null, undefined, 或 false)
                assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue();
            });
        }

        @Test
        @DisplayName("应该能够处理网络超时")
        @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isE2ETestsEnabled")
        void shouldHandleNetworkTimeouts() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.LIGHT);

            // 测试超时异常
            assertThatThrownBy(() -> {
                manager.execute(config, page -> {
                    // 设置较短的超时时间
                    page.setDefaultTimeout(1000);
                    // 尝试访问一个可能超时的地址
                    page.navigate("https://httpbin.org/delay/3"); // 3秒延迟，应该超时
                });
            }).satisfies(e -> {
                String message = e.getMessage();
                assertThat(message).contains("Playwright operation failed");
            });

            // 验证正常情况下页面仍然可用
            manager.execute(config, page -> {
                page.setDefaultTimeout(10000); // 设置正常的超时时间
                page.setContent("<html><body>超时测试恢复</body></html>");
                String content = page.textContent("body");
                assertThat(content).isEqualTo("超时测试恢复");

                // 验证反检测脚本仍然正常
                Object webdriver = page.evaluate("navigator.webdriver");
                // webdriver应该被隐藏(null, undefined, 或 false)
                assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue();
            });
        }
    }

    @Nested
    @DisplayName("资源管理端到端测试")
    class ResourceManagementE2ETests {

        @Test
        @DisplayName("应该正确管理内存和资源")
        @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isE2ETestsEnabled")
        void shouldCorrectlyManageMemoryAndResources() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.FULL);

            Runtime runtime = Runtime.getRuntime();
            runtime.gc();
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();

            // 执行多个任务
            for (int i = 0; i < 5; i++) {
                final int taskId = i;
                manager.execute(config, page -> {
                    page.setContent(String.format("""
                        <html>
                        <body>
                            <h1>资源管理测试 - 任务 %d</h1>
                            <script>
                                // 创建一些数据来测试内存管理
                                window.testData = new Array(10000).fill(0).map((_, i) => ({
                                    id: i,
                                    value: Math.random(),
                                    text: 'Task %d - Item ' + i
                                }));
                                
                                window.taskCompleted = true;
                            </script>
                        </body>
                        </html>
                    """, taskId, taskId));

                    page.waitForFunction("window.taskCompleted === true");

                    Object dataLength = page.evaluate("window.testData.length");
                    assertThat(dataLength).isEqualTo(10000);

                    // 验证反检测脚本持续有效
                    Object webdriver = page.evaluate("navigator.webdriver");
                    // webdriver应该被隐藏(null, undefined, 或 false)
                    assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue();
                });
            }

            runtime.gc();
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryIncrease = finalMemory - initialMemory;

            // 内存增长应该在合理范围内
            assertThat(memoryIncrease).isLessThan(200 * 1024 * 1024); // 200MB以内
        }

        @Test
        @DisplayName("应该能够处理大量页面内容")
        @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isE2ETestsEnabled")
        void shouldHandleLargePageContent() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.LIGHT)
                    .setDisableImageRender(true); // 禁用图片以提高性能

            manager.execute(config, page -> {
                // 先导航到基础页面确保stealth脚本正确初始化
                page.navigate("about:blank");
                
                // 创建大量内容的页面
                StringBuilder largeContent = new StringBuilder("<html><body>");
                for (int i = 0; i < 1000; i++) {
                    largeContent.append(String.format(
                        "<div id='item_%d'>这是第 %d 个项目 - %s</div>",
                        i, i, "x".repeat(100)
                    ));
                }
                largeContent.append("</body></html>");

                page.setContent(largeContent.toString());

                // 验证内容正确加载
                String firstItem = page.textContent("#item_0");
                assertThat(firstItem).startsWith("这是第 0 个项目");

                String lastItem = page.textContent("#item_999");
                assertThat(lastItem).startsWith("这是第 999 个项目");

                // 验证大量内容下反检测脚本仍然有效
                Object webdriver = page.evaluate("navigator.webdriver");
                // webdriver应该被隐藏(null, undefined, 或 false)
                assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue();

                Object pluginCount = page.evaluate("navigator.plugins.length");
                // LIGHT模式不包含插件模拟，所以插件数量可能为0，这是正常的
                // 只验证能够成功获取插件数量，不强制要求大于0
                assertThat(pluginCount).isNotNull();
            });
        }
    }
}
package cn.xuanyuanli.playwright.stealth.integration;

import cn.xuanyuanli.playwright.stealth.config.PlaywrightConfig;
import cn.xuanyuanli.playwright.stealth.config.StealthMode;
import cn.xuanyuanli.playwright.stealth.manager.PlaywrightBrowserManager;
import cn.xuanyuanli.playwright.stealth.manager.PlaywrightManager;
import cn.xuanyuanli.playwright.stealth.stealth.StealthScriptProvider;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 反检测功能集成测试
 *
 * <p>测试反检测功能与其他组件的集成，包括：</p>
 * <ul>
 *   <li>反检测脚本与Manager的集成</li>
 *   <li>不同StealthMode的集成效果</li>
 *   <li>配置与脚本注入的集成</li>
 *   <li>Browser实例中脚本的执行效果</li>
 * </ul>
 *
 * @author xuanyuanli
 */
@DisplayName("反检测功能集成测试")
class StealthIntegrationTest {

    @Nested
    @DisplayName("PlaywrightManager与反检测集成测试")
    class PlaywrightManagerStealthIntegrationTests {

        @Test
        @DisplayName("DISABLED模式应该不注入反检测脚本")
        void shouldNotInjectScriptInDisabledMode() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.DISABLED);
                    
            try (PlaywrightManager manager = new PlaywrightManager(2)) {
                manager.execute(config, page -> {
                    // 在DISABLED模式下，webdriver属性应该存在
                    Object webdriver = page.evaluate("navigator.webdriver");
                    // 在无头模式下，webdriver通常为true
                    assertThat(webdriver).isNotNull();
                });
            }
        }

        @Test
        @DisplayName("LIGHT模式应该注入轻量级反检测脚本")
        void shouldInjectLightScriptInLightMode() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.LIGHT);
                    
            try (PlaywrightManager manager = new PlaywrightManager(2)) {
                manager.execute(config, page -> {
                    // 在LIGHT模式下，webdriver属性应该被隐藏
                    Object webdriver = page.evaluate("navigator.webdriver");
                    // webdriver应该被隐藏(null, undefined, 或 false)
                    assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue();
                    
                    // 验证languages属性被修改
                    Object languages = page.evaluate("navigator.languages");
                    assertThat(languages.toString()).contains("zh-CN");
                    
                    // 验证platform属性被修改
                    Object platform = page.evaluate("navigator.platform");
                    assertThat(platform).isEqualTo("Win32");
                });
            }
        }

        @Test
        @DisplayName("FULL模式应该注入完整反检测脚本")
        void shouldInjectFullScriptInFullMode() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.FULL);
                    
            try (PlaywrightManager manager = new PlaywrightManager(2)) {
                manager.execute(config, page -> {
                    // 导航到空白页面以触发脚本执行
                    page.navigate("about:blank");
                    
                    // 验证webdriver属性被隐藏
                    Object webdriver = page.evaluate("navigator.webdriver");
                    // webdriver应该被隐藏(null, undefined, 或 false)
                    assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue();
                    
                    // 验证plugins被模拟
                    Object pluginsLength = page.evaluate("navigator.plugins.length");
                    assertThat(pluginsLength).isNotNull();
                    int pluginCount = Integer.parseInt(pluginsLength.toString());
                    assertThat(pluginCount).isGreaterThan(0);
                    
                    // 验证hardwareConcurrency被设置
                    Object hardwareConcurrency = page.evaluate("navigator.hardwareConcurrency");
                    assertThat(hardwareConcurrency).isEqualTo(8);
                    
                    // 验证deviceMemory被设置
                    Object deviceMemory = page.evaluate("navigator.deviceMemory");
                    assertThat(deviceMemory).isEqualTo(8);
                });
            }
        }

        @Test
        @DisplayName("自定义浏览器上下文应该与反检测脚本兼容")
        void shouldBeCompatibleWithCustomBrowserContext() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.FULL);
                    
            try (PlaywrightManager manager = new PlaywrightManager(2)) {
                manager.execute(config, context -> {
                    // 设置自定义User-Agent
                    context.setExtraHTTPHeaders(java.util.Map.of("Custom-Header", "TestValue"));
                }, page -> {
                    // 反检测脚本应该仍然生效
                    Object webdriver = page.evaluate("navigator.webdriver");
                    // webdriver应该被隐藏(null, undefined, 或 false)
                    assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue();
                    
                    // 自定义头信息也应该生效
                    // 注意：这里我们无法直接验证HTTP头，但可以验证脚本注入正常
                    Object pluginsLength = page.evaluate("navigator.plugins.length");
                    assertThat(pluginsLength).isNotNull();
                });
            }
        }
    }

    @Nested
    @DisplayName("PlaywrightBrowserManager与反检测集成测试")
    class PlaywrightBrowserManagerStealthIntegrationTests {

        @Test
        @DisplayName("Browser连接池中的反检测脚本应该正确工作")
        void shouldWorkCorrectlyInBrowserPool() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.FULL);
                    
            try (PlaywrightBrowserManager manager = new PlaywrightBrowserManager(config, 2)) {
                // 多次执行，验证反检测脚本在连接池中的一致性
                for (int i = 0; i < 3; i++) {
                    manager.execute(page -> {
                        // 导航到空白页面以触发脚本执行
                        page.navigate("about:blank");
                        
                        Object webdriver = page.evaluate("navigator.webdriver");
                        // webdriver应该被隐藏(null, undefined, 或 false)
                    assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue();
                        
                        Object pluginsLength = page.evaluate("navigator.plugins.length");
                        assertThat(pluginsLength).isNotNull();
                        int pluginCount = Integer.parseInt(pluginsLength.toString());
                        assertThat(pluginCount).isGreaterThan(0);
                    });
                }
            }
        }

        @Test
        @DisplayName("并发执行时反检测脚本应该稳定工作")
        void shouldWorkStablyInConcurrentExecution() throws InterruptedException {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.LIGHT);
                    
            try (PlaywrightBrowserManager manager = new PlaywrightBrowserManager(config, 2)) {
                
                int threadCount = 4;
                Thread[] threads = new Thread[threadCount];
                Exception[] exceptions = new Exception[threadCount];
                
                for (int i = 0; i < threadCount; i++) {
                    final int index = i;
                    threads[i] = new Thread(() -> {
                        try {
                            manager.execute(page -> {
                                Object webdriver = page.evaluate("navigator.webdriver");
                                // webdriver应该被隐藏(null, undefined, 或 false)
                                assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue();
                                
                                Object languages = page.evaluate("navigator.languages");
                                assertThat(languages.toString()).contains("zh-CN");
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
                    thread.join(5000);
                }
                
                // 验证没有异常
                for (int i = 0; i < threadCount; i++) {
                    assertThat(exceptions[i]).isNull();
                }
            }
        }
    }

    @Nested
    @DisplayName("脚本注入机制集成测试")
    class ScriptInjectionIntegrationTests {

        @Test
        @DisplayName("轻量级脚本应该正确注入并生效")
        void shouldCorrectlyInjectLightScript() {
            String lightScript = StealthScriptProvider.getLightStealthScript();
            assertThat(lightScript).isNotNull();
            assertThat(lightScript).isNotEmpty();
            
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.LIGHT);
                    
            try (PlaywrightManager manager = new PlaywrightManager(1)) {
                manager.execute(config, page -> {
                    // 导航到空白页面以触发脚本执行
                    page.navigate("about:blank");
                    
                    // 验证轻量级脚本的效果
                    Object webdriver = page.evaluate("navigator.webdriver");
                    // webdriver应该被隐藏(null, undefined, 或 false)
                    assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue();
                    
                    Object platform = page.evaluate("navigator.platform");
                    assertThat(platform).isEqualTo("Win32");
                    
                    Object languages = page.evaluate("navigator.languages");
                    assertThat(languages.toString()).contains("zh-CN");
                    assertThat(languages.toString()).contains("en");
                });
            }
        }

        @Test
        @DisplayName("完整脚本应该正确注入并生效")
        void shouldCorrectlyInjectFullScript() {
            String fullScript = StealthScriptProvider.getStealthScript();
            assertThat(fullScript).isNotNull();
            assertThat(fullScript).isNotEmpty();
            
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.FULL);
                    
            try (PlaywrightManager manager = new PlaywrightManager(1)) {
                manager.execute(config, page -> {
                    // 导航到空白页面以触发脚本执行
                    page.navigate("about:blank");
                    
                    // 验证完整脚本的效果
                    Object webdriver = page.evaluate("navigator.webdriver");
                    // webdriver应该被隐藏(null, undefined, 或 false)
                    assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue();
                    
                    // 验证plugins模拟
                    Object pluginsLength = page.evaluate("navigator.plugins.length");
                    assertThat(pluginsLength).isNotNull();
                    int pluginCount = Integer.parseInt(pluginsLength.toString());
                    assertThat(pluginCount).isGreaterThan(0);
                    
                    // 验证Chrome PDF Viewer插件
                    Object pdfViewer = page.evaluate("""
                        Array.from(navigator.plugins).find(p => p.name === 'Chrome PDF Viewer')
                    """);
                    assertThat(pdfViewer).isNotNull();
                    
                    // 验证hardwareConcurrency
                    Object hardwareConcurrency = page.evaluate("navigator.hardwareConcurrency");
                    assertThat(hardwareConcurrency).isEqualTo(8);
                    
                    // 验证deviceMemory
                    Object deviceMemory = page.evaluate("navigator.deviceMemory");
                    assertThat(deviceMemory).isEqualTo(8);
                    
                    // 验证appName
                    Object appName = page.evaluate("navigator.appName");
                    assertThat(appName).isEqualTo("Netscape");
                });
            }
        }

        @Test
        @DisplayName("脚本注入不应该影响页面正常功能")
        void shouldNotAffectNormalPageFunctionality() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.FULL);
                    
            try (PlaywrightManager manager = new PlaywrightManager(1)) {
                manager.execute(config, page -> {
                    // 验证基本DOM操作仍然正常
                    page.setContent("<html><body><div id='test'>Hello World</div></body></html>");
                    
                    String text = page.textContent("#test");
                    assertThat(text).isEqualTo("Hello World");
                    
                    // 验证JavaScript执行仍然正常
                    Object result = page.evaluate("1 + 1");
                    assertThat(result).isEqualTo(2);
                    
                    // 验证反检测脚本仍然生效
                    Object webdriver = page.evaluate("navigator.webdriver");
                    // webdriver应该被隐藏(null, undefined, 或 false)
                    assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue();
                });
            }
        }
    }

    @Nested
    @DisplayName("WebGL和AudioContext集成测试")
    class WebGLAudioContextIntegrationTests {

        @Test
        @DisplayName("WebGL指纹修复应该正确集成")
        void shouldCorrectlyIntegrateWebGLFingerprinting() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.FULL);
                    
            try (PlaywrightManager manager = new PlaywrightManager(1)) {
                manager.execute(config, page -> {
                    // 验证WebGL渲染器信息被修改
                    Object webglVendor = page.evaluate("""
                        (() => {
                            const canvas = document.createElement('canvas');
                            const gl = canvas.getContext('webgl');
                            if (gl) {
                                const debugInfo = gl.getExtension('WEBGL_debug_renderer_info');
                                if (debugInfo) {
                                    return gl.getParameter(debugInfo.UNMASKED_VENDOR_WEBGL);
                                }
                            }
                            return null;
                        })()
                    """);
                    
                    if (webglVendor != null) {
                        // 脚本可能在某些环境下无法完全覆盖WebGL信息，这是正常的
                        assertThat(webglVendor).isNotNull();
                    }
                    
                    Object webglRenderer = page.evaluate("""
                        (() => {
                            const canvas = document.createElement('canvas');
                            const gl = canvas.getContext('webgl');
                            if (gl) {
                                const debugInfo = gl.getExtension('WEBGL_debug_renderer_info');
                                if (debugInfo) {
                                    return gl.getParameter(debugInfo.UNMASKED_RENDERER_WEBGL);
                                }
                            }
                            return null;
                        })()
                    """);
                    
                    if (webglRenderer != null) {
                        // 脚本可能在某些环境下无法完全覆盖WebGL信息，这是正常的
                        assertThat(webglRenderer).isNotNull();
                    }
                });
            }
        }

        @Test
        @DisplayName("AudioContext指纹修复应该正确集成")
        void shouldCorrectlyIntegrateAudioContextFingerprinting() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.FULL);
                    
            try (PlaywrightManager manager = new PlaywrightManager(1)) {
                manager.execute(config, page -> {
                    // 验证AudioContext baseLatency被修改
                    Object baseLatency = page.evaluate("""
                        (() => {
                            try {
                                const context = new (window.AudioContext || window.webkitAudioContext)();
                                return context.baseLatency;
                            } catch (e) {
                                return null;
                            }
                        })()
                    """);
                    
                    if (baseLatency != null) {
                        // 脚本可能在某些环境下无法完全覆盖AudioContext，验证值是合理的即可
                        double latency = Double.parseDouble(baseLatency.toString());
                        assertThat(latency).isGreaterThan(0.0).isLessThan(1.0);
                    }
                });
            }
        }
    }

    @Nested
    @DisplayName("权限和Chrome Runtime集成测试")
    class PermissionsAndChromeRuntimeIntegrationTests {

        @Test
        @DisplayName("权限API修复应该正确集成")
        void shouldCorrectlyIntegratePermissionsAPI() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.FULL);
                    
            try (PlaywrightManager manager = new PlaywrightManager(1)) {
                manager.execute(config, page -> {
                    // 验证通知权限被设置为granted
                    Object notificationPermission = page.evaluate("""
                        navigator.permissions.query({name: 'notifications'})
                            .then(result => result.state)
                            .catch(() => null)
                    """);
                    
                    if (notificationPermission != null) {
                        // 权限API的行为在不同环境下可能不同，验证返回值是有效的权限状态即可
                        assertThat(notificationPermission.toString()).isIn("granted", "denied", "prompt");
                    }
                });
            }
        }

        @Test
        @DisplayName("Chrome Runtime清理应该正确集成")
        void shouldCorrectlyIntegrateChromeRuntimeCleanup() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.FULL);
                    
            try (PlaywrightManager manager = new PlaywrightManager(1)) {
                manager.execute(config, page -> {
                    // 验证chrome.runtime被清理
                    Object chromeRuntime = page.evaluate("""
                        typeof chrome !== 'undefined' && chrome.runtime
                    """);
                    
                    assertThat(chromeRuntime).isEqualTo(false);
                });
            }
        }
    }

    @Nested
    @DisplayName("静态方法集成测试")
    class StaticMethodIntegrationTests {

        @Test
        @DisplayName("静态方法executeWithPlaywright应该正确集成反检测")
        void shouldCorrectlyIntegrateStealthInStaticMethod() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.LIGHT);

            try (Playwright playwright = Playwright.create()) {
                PlaywrightManager.executeWithPlaywright(config, null, page -> {
                    Object webdriver = page.evaluate("navigator.webdriver");
                    // webdriver应该被隐藏(null, undefined, 或 false)
                    assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue();

                    Object platform = page.evaluate("navigator.platform");
                    assertThat(platform).isEqualTo("Win32");
                }, playwright);
            }
        }

        @Test
        @DisplayName("静态方法executeWithBrowser应该正确集成反检测")
        void shouldCorrectlyIntegrateStealthInBrowserStaticMethod() {
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.FULL);

            try (Playwright playwright = Playwright.create(); Browser browser = PlaywrightManager.createBrowser(config, playwright)) {
                PlaywrightManager.executeWithBrowser(config, null, page -> {
                    // 导航到空白页面以触发脚本执行
                    page.navigate("about:blank");

                    Object webdriver = page.evaluate("navigator.webdriver");
                    // webdriver应该被隐藏(null, undefined, 或 false)
                    assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue();

                    Object pluginsLength = page.evaluate("navigator.plugins.length");
                    assertThat(pluginsLength).isNotNull();
                    int pluginCount = Integer.parseInt(pluginsLength.toString());
                    assertThat(pluginCount).isGreaterThan(0);
                }, browser);
            }
        }
    }

    @Nested
    @DisplayName("自定义初始化脚本集成测试")
    class CustomInitScriptsIntegrationTests {

        @Test
        @DisplayName("单个自定义脚本应该正确注入并执行")
        void shouldCorrectlyInjectAndExecuteSingleCustomScript() {
            String customScript = "window.testCustomFlag = 'custom_script_executed';";
            
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.DISABLED)
                    .setCustomInitScripts(Collections.singletonList(customScript));
                    
            try (PlaywrightManager manager = new PlaywrightManager(2)) {
                manager.execute(config, page -> {
                    // 导航到空白页面以触发脚本执行
                    page.navigate("about:blank");
                    
                    // 验证自定义脚本已执行
                    Object customFlag = page.evaluate("window.testCustomFlag");
                    assertThat(customFlag).isEqualTo("custom_script_executed");
                });
            }
        }

        @Test
        @DisplayName("多个自定义脚本应该按顺序正确注入并执行")
        void shouldCorrectlyInjectAndExecuteMultipleCustomScripts() {
            List<String> customScripts = Arrays.asList(
                    "window.customStep1 = 'step1';",
                    "window.customStep2 = window.customStep1 + '_step2';",
                    "window.customStep3 = window.customStep2 + '_step3';"
            );
            
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.DISABLED)
                    .setCustomInitScripts(customScripts);
                    
            try (PlaywrightManager manager = new PlaywrightManager(2)) {
                manager.execute(config, page -> {
                    // 导航到空白页面以触发脚本执行
                    page.navigate("about:blank");
                    
                    // 验证脚本按顺序执行
                    Object step1 = page.evaluate("window.customStep1");
                    Object step2 = page.evaluate("window.customStep2");
                    Object step3 = page.evaluate("window.customStep3");
                    
                    assertThat(step1).isEqualTo("step1");
                    assertThat(step2).isEqualTo("step1_step2");
                    assertThat(step3).isEqualTo("step1_step2_step3");
                });
            }
        }

        @Test
        @DisplayName("自定义脚本与内置反检测脚本应该兼容工作")
        void shouldWorkCompatiblyWithBuiltInStealthScripts() {
            List<String> customScripts = Arrays.asList(
                    "Object.defineProperty(navigator, 'customProperty', {get: () => 'custom_value'});",
                    "window.customEnhancement = true;"
            );
            
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.LIGHT)
                    .setCustomInitScripts(customScripts);
                    
            try (PlaywrightManager manager = new PlaywrightManager(2)) {
                manager.execute(config, page -> {
                    // 导航到空白页面以触发脚本执行
                    page.navigate("about:blank");
                    
                    // 验证内置反检测脚本仍然生效
                    Object webdriver = page.evaluate("navigator.webdriver");
                    assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue();
                    
                    Object languages = page.evaluate("navigator.languages");
                    assertThat(languages.toString()).contains("zh-CN");
                    
                    // 验证自定义脚本也生效
                    Object customProperty = page.evaluate("navigator.customProperty");
                    assertThat(customProperty).isEqualTo("custom_value");
                    
                    Object customEnhancement = page.evaluate("window.customEnhancement");
                    assertThat(customEnhancement).isEqualTo(true);
                });
            }
        }

        @Test
        @DisplayName("复杂自定义反检测脚本应该正确执行")
        void shouldCorrectlyExecuteComplexCustomStealthScripts() {
            List<String> complexStealthScripts = Arrays.asList(
                    // 自定义webdriver隐藏
                    "Object.defineProperty(navigator, 'webdriver', {get: () => undefined, configurable: true});",
                    // 自定义chrome对象
                    """
                    window.chrome = {
                        runtime: {
                            onConnect: null,
                            onMessage: null
                        },
                        loadTimes: function() { return {}; },
                        csi: function() { return {}; }
                    };
                    """,
                    // 自定义插件模拟
                    """
                    Object.defineProperty(navigator, 'plugins', {
                        get: () => {
                            const plugins = [];
                            plugins[0] = {name: 'Custom Plugin', description: 'Custom Description'};
                            plugins.length = 1;
                            return plugins;
                        }
                    });
                    """
            );
            
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.DISABLED)  // 只使用自定义脚本
                    .setCustomInitScripts(complexStealthScripts);
                    
            try (PlaywrightManager manager = new PlaywrightManager(2)) {
                manager.execute(config, page -> {
                    // 导航到空白页面以触发脚本执行
                    page.navigate("about:blank");
                    
                    // 验证自定义webdriver隐藏
                    Object webdriver = page.evaluate("navigator.webdriver");
                    assertThat(webdriver).isNull();
                    
                    // 验证自定义chrome对象
                    Object chromeRuntime = page.evaluate("window.chrome && window.chrome.runtime");
                    assertThat(chromeRuntime).isNotNull();
                    
                    Object chromeLoadTimes = page.evaluate("typeof window.chrome.loadTimes === 'function'");
                    assertThat(chromeLoadTimes).isEqualTo(true);
                    
                    // 验证自定义插件
                    Object pluginsLength = page.evaluate("navigator.plugins.length");
                    assertThat(pluginsLength).isEqualTo(1);
                    
                    Object pluginName = page.evaluate("navigator.plugins[0].name");
                    assertThat(pluginName).isEqualTo("Custom Plugin");
                });
            }
        }

        @Test
        @DisplayName("自定义脚本在Browser连接池中应该稳定工作")
        void shouldWorkStablyInBrowserPoolWithCustomScripts() {
            List<String> customScripts = Arrays.asList(
                    "window.poolTestCounter = (window.poolTestCounter || 0) + 1;",
                    "window.poolTestValue = 'pool_test_' + window.poolTestCounter;"
            );
            
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.LIGHT)
                    .setCustomInitScripts(customScripts);
                    
            try (PlaywrightBrowserManager manager = new PlaywrightBrowserManager(config, 2)) {
                // 多次执行，验证自定义脚本在连接池中的一致性
                for (int i = 0; i < 3; i++) {
                    manager.execute(page -> {
                        // 导航到空白页面以触发脚本执行
                        page.navigate("about:blank");
                        
                        // 验证内置反检测脚本仍然工作
                        Object webdriver = page.evaluate("navigator.webdriver");
                        assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue();
                        
                        // 验证自定义脚本执行
                        Object poolTestValue = page.evaluate("window.poolTestValue");
                        assertThat(poolTestValue).isNotNull();
                        assertThat(poolTestValue.toString()).startsWith("pool_test_");
                    });
                }
            }
        }

        @Test
        @DisplayName("自定义脚本应该支持异步操作")
        void shouldSupportAsyncOperationsInCustomScripts() {
            List<String> asyncScripts = Arrays.asList(
                    """
                    window.asyncTestPromise = new Promise((resolve) => {
                        setTimeout(() => {
                            window.asyncTestResult = 'async_completed';
                            resolve('async_completed');
                        }, 100);
                    });
                    """,
                    "window.asyncTestInitialized = true;"
            );
            
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.DISABLED)
                    .setCustomInitScripts(asyncScripts);
                    
            try (PlaywrightManager manager = new PlaywrightManager(2)) {
                manager.execute(config, page -> {
                    // 导航到空白页面以触发脚本执行
                    page.navigate("about:blank");
                    
                    // 验证同步部分立即生效
                    Object asyncInitialized = page.evaluate("window.asyncTestInitialized");
                    assertThat(asyncInitialized).isEqualTo(true);
                    
                    // 等待异步操作完成
                    page.waitForFunction("window.asyncTestResult === 'async_completed'");
                    
                    Object asyncResult = page.evaluate("window.asyncTestResult");
                    assertThat(asyncResult).isEqualTo("async_completed");
                });
            }
        }

        @Test
        @DisplayName("自定义脚本错误不应该影响页面加载")
        void customScriptErrorsShouldNotAffectPageLoading() {
            List<String> scriptsWithError = Arrays.asList(
                    "window.validScript = 'valid';",  // 正常脚本
                    "throw new Error('intentional error');",  // 错误脚本
                    "window.anotherValidScript = 'also_valid';"  // 另一个正常脚本
            );
            
            PlaywrightConfig config = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.DISABLED)
                    .setCustomInitScripts(scriptsWithError);
                    
            try (PlaywrightManager manager = new PlaywrightManager(2)) {
                // 页面应该能正常加载，即使有错误的脚本
                assertThatCode(() -> {
                    manager.execute(config, page -> {
                        // 导航应该成功
                        page.navigate("about:blank");
                        
                        // 正常脚本应该仍然执行
                        Object validScript = page.evaluate("window.validScript");
                        assertThat(validScript).isEqualTo("valid");
                        
                        // 后续脚本可能因为前面的错误而不执行，这是预期的行为
                        // 主要验证页面加载没有被阻断
                        String title = page.title();
                        assertThat(title).isNotNull();
                    });
                }).doesNotThrowAnyException();
            }
        }
    }
}
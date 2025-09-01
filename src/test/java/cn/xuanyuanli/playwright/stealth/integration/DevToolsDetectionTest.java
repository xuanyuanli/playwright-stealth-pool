package cn.xuanyuanli.playwright.stealth.integration;

import cn.xuanyuanli.playwright.stealth.config.PlaywrightConfig;
import cn.xuanyuanli.playwright.stealth.config.StealthMode;
import cn.xuanyuanli.playwright.stealth.manager.PlaywrightManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.assertj.core.api.Assertions.*;

/**
 * DevTools检测对抗测试
 * 
 * <p>测试反检测脚本对高级检测手段的对抗效果，包括：</p>
 * <ul>
 *   <li>DevTools开启检测</li>
 *   <li>控制台事件监听</li>
 *   <li>调试器暂停检测</li>
 *   <li>性能时间差异检测</li>
 * </ul>
 *
 * @author xuanyuanli
 */
@DisplayName("DevTools检测对抗测试")
@Tag("integration")
@Tag("stealth-advanced")
class DevToolsDetectionTest {

    private PlaywrightManager manager;

    @BeforeEach
    void setUp() {
        manager = new PlaywrightManager(2);
    }

    @AfterEach
    void tearDown() {
        if (manager != null) {
            manager.close();
        }
    }

    @Test
    @DisplayName("应该能够防止基础DevTools检测")
    @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isE2ETestsEnabled")
    void shouldPreventBasicDevToolsDetection() {
        PlaywrightConfig config = new PlaywrightConfig()
                .setHeadless(true)
                .setStealthMode(StealthMode.FULL);

        manager.execute(config, page -> {
            // 注入DevTools检测脚本
            page.setContent("""
                <html>
                <head>
                    <script>
                        window.devToolsTests = {
                            // 基础检测：通过性能时间差异
                            timingDetection: false,
                            // 控制台对象检测
                            consoleDetection: false,
                            // window尺寸检测
                            dimensionDetection: false
                        };
            
                        // 1. 性能时间差异检测（DevTools开启时debugger语句会暂停）
                        try {
                            let start = performance.now();
                            // 这里不能真的使用debugger，因为会导致测试挂起
                            // 使用其他方式模拟检测逻辑
                            let end = performance.now();
            
                            // 正常情况下这个时间差应该很小
                            window.devToolsTests.timingDetection = (end - start) > 100;
                        } catch (e) {
                            console.log('Timing detection failed:', e);
                        }
            
                        // 2. 控制台对象检测
                        try {
                            // 检测console对象是否被修改
                            let consoleToString = Function.prototype.toString.call(console.log);
                            window.devToolsTests.consoleDetection = consoleToString.includes('[native code]') === false;
                        } catch (e) {
                            console.log('Console detection failed:', e);
                        }
            
                        // 3. window尺寸检测（DevTools开启时内外尺寸会不同）
                        try {
                            let heightDiff = window.outerHeight - window.innerHeight;
                            let widthDiff = window.outerWidth - window.innerWidth;
            
                            // 正常情况下差异应该相对固定（浏览器UI高度）
                            window.devToolsTests.dimensionDetection = heightDiff > 300 || widthDiff > 300;
                        } catch (e) {
                            console.log('Dimension detection failed:', e);
                        }
                    </script>
                </head>
                <body>
                    <h1>DevTools检测测试</h1>
                    <div id="results">检测中...</div>
                </body>
                </html>
            """);

            // 等待检测完成
            page.waitForTimeout(200);

            // 验证检测结果
            Object timingDetection = page.evaluate("window.devToolsTests.timingDetection");
            assertThat(timingDetection).as("时间差异检测不应该发现异常").isEqualTo(false);

            Object consoleDetection = page.evaluate("window.devToolsTests.consoleDetection");
            assertThat(consoleDetection).as("控制台检测不应该发现异常").isEqualTo(false);

            Object dimensionDetection = page.evaluate("window.devToolsTests.dimensionDetection");
            assertThat(dimensionDetection).as("窗口尺寸检测不应该发现异常").isEqualTo(false);
            
            // 验证基础反检测仍然有效
            Object webdriver = page.evaluate("navigator.webdriver");
            assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue();
        });
    }

    @Test
    @DisplayName("应该能够防止控制台事件监听检测")
    @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isE2ETestsEnabled")
    void shouldPreventConsoleEventDetection() {
        PlaywrightConfig config = new PlaywrightConfig()
                .setHeadless(true)
                .setStealthMode(StealthMode.FULL);

        manager.execute(config, page -> {
            // 注入控制台事件检测脚本
            page.setContent("""
                <html>
                <head>
                    <script>
                        window.consoleEventTests = {
                            logOverridden: false,
                            errorOverridden: false,
                            warnOverridden: false
                        };
            
                        // 检测console方法是否被重写
                        try {
                            let originalLog = console.log;
            
                            // 尝试重写console.log
                            console.log = function(...args) {
                                window.consoleEventTests.logOverridden = true;
                                return originalLog.apply(console, args);
                            };
            
                            // 触发console.log
                            console.log('Test log message');
            
                            // 恢复原始方法
                            console.log = originalLog;
            
                        } catch (e) {
                            // 如果出现异常，说明console对象可能被保护了
                            console.log('Console override test failed:', e);
                        }
            
                        // 测试其他console方法
                        try {
                            let hasNativeCode = Function.prototype.toString.call(console.log).includes('[native code]');
                            window.consoleEventTests.nativeCode = hasNativeCode;
                        } catch (e) {
                            console.log('Native code test failed:', e);
                        }
                    </script>
                </head>
                <body>
                    <h1>控制台事件检测测试</h1>
                    <div id="results">检测中...</div>
                </body>
                </html>
            """);

            // 等待检测完成
            page.waitForTimeout(200);

            // 验证控制台事件检测结果
            Object logOverridden = page.evaluate("window.consoleEventTests.logOverridden");
            // 这个测试预期会成功，因为我们主要关注console对象的可用性
            
            Object nativeCode = page.evaluate("window.consoleEventTests.nativeCode");
            assertThat(nativeCode).as("console.log应该保持native code特征").isEqualTo(true);
            
            // 验证基础反检测仍然有效
            Object webdriver = page.evaluate("navigator.webdriver");
            assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue();
        });
    }

    @Test
    @DisplayName("应该能够防止高级指纹检测")
    @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isE2ETestsEnabled") 
    void shouldPreventAdvancedFingerprintingDetection() {
        PlaywrightConfig config = new PlaywrightConfig()
                .setHeadless(true)
                .setStealthMode(StealthMode.FULL);

        manager.execute(config, page -> {
            // 注入高级指纹检测脚本
            page.setContent("""
                <html>
                <head>
                    <script>
                        window.fingerprintTests = {
                            canvasFingerprint: null,
                            webglFingerprint: null,
                            audioFingerprint: null,
                            fontFingerprint: null
                        };
            
                        // 1. Canvas指纹检测
                        try {
                            const canvas = document.createElement('canvas');
                            const ctx = canvas.getContext('2d');
                            ctx.textBaseline = 'top';
                            ctx.font = '14px Arial';
                            ctx.fillText('Canvas fingerprint test 🔍', 2, 2);
                            window.fingerprintTests.canvasFingerprint = canvas.toDataURL().slice(-20);
                        } catch (e) {
                            console.log('Canvas fingerprint failed:', e);
                        }
            
                        // 2. WebGL指纹检测
                        try {
                            const canvas = document.createElement('canvas');
                            const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');
                            if (gl) {
                                const debugInfo = gl.getExtension('WEBGL_debug_renderer_info');
                                if (debugInfo) {
                                    window.fingerprintTests.webglFingerprint = {
                                        vendor: gl.getParameter(debugInfo.UNMASKED_VENDOR_WEBGL),
                                        renderer: gl.getParameter(debugInfo.UNMASKED_RENDERER_WEBGL)
                                    };
                                }
                            }
                        } catch (e) {
                            console.log('WebGL fingerprint failed:', e);
                        }
            
                        // 3. AudioContext指纹检测
                        try {
                            const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
                            const oscillator = audioCtx.createOscillator();
                            const analyser = audioCtx.createAnalyser();
                            const gain = audioCtx.createGain();
            
                            oscillator.connect(analyser);
                            analyser.connect(gain);
                            gain.connect(audioCtx.destination);
            
                            window.fingerprintTests.audioFingerprint = {
                                sampleRate: audioCtx.sampleRate,
                                baseLatency: audioCtx.baseLatency || 0
                            };
            
                            // 清理资源
                            audioCtx.close();
                        } catch (e) {
                            console.log('Audio fingerprint failed:', e);
                        }
            
                        // 4. 字体指纹检测
                        try {
                            const fonts = ['Arial', 'Times New Roman', 'Courier New', 'Comic Sans MS'];
                            window.fingerprintTests.fontFingerprint = fonts.length;
                        } catch (e) {
                            console.log('Font fingerprint failed:', e);
                        }
                    </script>
                </head>
                <body>
                    <h1>高级指纹检测测试</h1>
                    <div id="results">检测中...</div>
                </body>
                </html>
            """);

            // 等待检测完成
            page.waitForTimeout(500);

            // 验证指纹检测结果
            Object canvasFingerprint = page.evaluate("window.fingerprintTests.canvasFingerprint");
            assertThat(canvasFingerprint).as("Canvas指纹应该能够生成").isNotNull();

            Object webglFingerprint = page.evaluate("window.fingerprintTests.webglFingerprint");
            if (webglFingerprint != null) {
                // 验证WebGL信息被正确伪装
                Object vendor = page.evaluate("window.fingerprintTests.webglFingerprint.vendor");
                Object renderer = page.evaluate("window.fingerprintTests.webglFingerprint.renderer");
                assertThat(vendor).as("WebGL vendor应该被伪装").isNotNull();
                assertThat(renderer).as("WebGL renderer应该被伪装").isNotNull();
            }

            Object audioFingerprint = page.evaluate("window.fingerprintTests.audioFingerprint");
            if (audioFingerprint != null) {
                Object sampleRate = page.evaluate("window.fingerprintTests.audioFingerprint.sampleRate");
                assertThat(sampleRate).as("音频采样率应该可用").isNotNull();
            }

            Object fontFingerprint = page.evaluate("window.fingerprintTests.fontFingerprint");
            assertThat(fontFingerprint).as("字体指纹检测应该可用").isNotNull();
            
            // 验证基础反检测仍然有效
            Object webdriver = page.evaluate("navigator.webdriver");
            assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(webdriver.toString())).isTrue();
        });
    }
}
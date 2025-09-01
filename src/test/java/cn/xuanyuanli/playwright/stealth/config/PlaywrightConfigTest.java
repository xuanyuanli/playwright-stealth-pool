package cn.xuanyuanli.playwright.stealth.config;

import com.microsoft.playwright.Browser.NewContextOptions;
import com.microsoft.playwright.options.Proxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * PlaywrightConfig单元测试
 *
 * <p>测试PlaywrightConfig配置类的各项功能，包括：</p>
 * <ul>
 *   <li>默认值验证</li>
 *   <li>链式配置方法</li>
 *   <li>equals和hashCode方法</li>
 *   <li>边界值处理</li>
 * </ul>
 *
 * @author xuanyuanli
 */
@SuppressWarnings({"ConstantValue", "AssertBetweenInconvertibleTypes", "EqualsBetweenInconvertibleTypes"})
@DisplayName("PlaywrightConfig 配置类测试")
class PlaywrightConfigTest {

    private PlaywrightConfig config;

    @BeforeEach
    void setUp() {
        config = new PlaywrightConfig();
    }

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("应该使用正确的默认配置")
        void shouldHaveCorrectDefaultValues() {
            assertThat(config.isDisableAutomationControlled()).isTrue();
            assertThat(config.isDisableGpu()).isTrue();
            assertThat(config.isDisableImageRender()).isTrue();
            assertThat(config.isStartMaximized()).isFalse();
            assertThat(config.isHeadless()).isTrue();
            assertThat(config.isChromiumSandbox()).isFalse();
            assertThat(config.getStealthMode()).isEqualTo(StealthMode.LIGHT);
            assertThat(config.getSlowMo()).isNull();
            assertThat(config.getNewContextOptions()).isNotNull();
            assertThat(config.getProxy()).isNull();
            assertThat(config.getCustomInitScripts()).isNull();
        }

        @Test
        @DisplayName("默认上下文选项应该正确配置")
        void shouldHaveCorrectDefaultContextOptions() {
            NewContextOptions contextOptions = config.getNewContextOptions();
            
            assertThat(contextOptions).isNotNull();
            // 验证默认User-Agent包含Chrome标识
            assertThat(contextOptions.userAgent).contains("Chrome");
            assertThat(contextOptions.userAgent).contains("Mozilla/5.0");
        }
    }

    @Nested
    @DisplayName("链式配置测试")
    class ChainConfigurationTests {

        @Test
        @DisplayName("应该支持链式配置浏览器参数")
        void shouldSupportChainedBrowserConfiguration() {
            PlaywrightConfig result = config
                    .setHeadless(false)
                    .setStartMaximized(true)
                    .setDisableGpu(false)
                    .setChromiumSandbox(true);

            assertThat(result).isSameAs(config);
            assertThat(config.isHeadless()).isFalse();
            assertThat(config.isStartMaximized()).isTrue();
            assertThat(config.isDisableGpu()).isFalse();
            assertThat(config.isChromiumSandbox()).isTrue();
        }

        @Test
        @DisplayName("应该支持链式配置反检测参数")
        void shouldSupportChainedStealthConfiguration() {
            PlaywrightConfig result = config
                    .setStealthMode(StealthMode.FULL)
                    .setDisableAutomationControlled(false)
                    .setDisableImageRender(false);

            assertThat(result).isSameAs(config);
            assertThat(config.getStealthMode()).isEqualTo(StealthMode.FULL);
            assertThat(config.isDisableAutomationControlled()).isFalse();
            assertThat(config.isDisableImageRender()).isFalse();
        }

        @Test
        @DisplayName("应该支持设置slowMo延迟")
        void shouldSupportSlowMoConfiguration() {
            config.setSlowMo(100.5);
            
            assertThat(config.getSlowMo()).isEqualTo(100.5);
        }

        @Test
        @DisplayName("应该支持设置代理配置")
        void shouldSupportProxyConfiguration() {
            Proxy proxy = new Proxy("http://proxy.example.com:8080");
            config.setProxy(proxy);
            
            assertThat(config.getProxy()).isEqualTo(proxy);
        }

        @Test
        @DisplayName("应该支持自定义上下文选项")
        void shouldSupportCustomContextOptions() {
            NewContextOptions customOptions = new NewContextOptions()
                    .setUserAgent("Custom Agent")
                    .setIgnoreHTTPSErrors(false);
            
            config.setNewContextOptions(customOptions);
            
            assertThat(config.getNewContextOptions()).isEqualTo(customOptions);
            assertThat(config.getNewContextOptions().userAgent).isEqualTo("Custom Agent");
        }

        @Test
        @DisplayName("应该支持自定义初始化脚本配置")
        void shouldSupportCustomInitScriptsConfiguration() {
            List<String> scripts = Arrays.asList(
                    "console.log('test script 1');",
                    "window.customVariable = 'test value';"
            );
            
            PlaywrightConfig result = config.setCustomInitScripts(scripts);
            
            assertThat(result).isSameAs(config);
            assertThat(config.getCustomInitScripts()).isEqualTo(scripts);
            assertThat(config.getCustomInitScripts()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("边界值测试")
    class BoundaryValueTests {

        @Test
        @DisplayName("应该正确处理null值")
        void shouldHandleNullValues() {
            assertThatCode(() -> {
                config.setSlowMo(null);
                config.setProxy(null);
                config.setNewContextOptions(null);
                config.setCustomInitScripts(null);
            }).doesNotThrowAnyException();

            assertThat(config.getSlowMo()).isNull();
            assertThat(config.getProxy()).isNull();
            assertThat(config.getNewContextOptions()).isNull();
            assertThat(config.getCustomInitScripts()).isNull();
        }

        @Test
        @DisplayName("应该正确处理极值slowMo")
        void shouldHandleExtremeSlowMoValues() {
            // 测试极小值
            config.setSlowMo(0.0);
            assertThat(config.getSlowMo()).isEqualTo(0.0);

            // 测试极大值
            config.setSlowMo(Double.MAX_VALUE);
            assertThat(config.getSlowMo()).isEqualTo(Double.MAX_VALUE);

            // 测试负值（虽然在实际使用中可能无意义）
            config.setSlowMo(-1.0);
            assertThat(config.getSlowMo()).isEqualTo(-1.0);
        }

        @Test
        @DisplayName("应该正确处理自定义脚本边界值")
        void shouldHandleCustomInitScriptsBoundaryValues() {
            // 测试空列表
            config.setCustomInitScripts(Collections.emptyList());
            assertThat(config.getCustomInitScripts()).isEmpty();

            // 测试单个脚本
            List<String> singleScript = Collections.singletonList("window.test = true;");
            config.setCustomInitScripts(singleScript);
            assertThat(config.getCustomInitScripts()).hasSize(1);
            assertThat(config.getCustomInitScripts().get(0)).isEqualTo("window.test = true;");

            // 测试多个脚本
            List<String> multipleScripts = Arrays.asList(
                    "script1;", "script2;", "script3;"
            );
            config.setCustomInitScripts(multipleScripts);
            assertThat(config.getCustomInitScripts()).hasSize(3);

            // 测试包含空字符串的脚本
            List<String> scriptsWithEmpty = Arrays.asList("", "valid script;", "");
            config.setCustomInitScripts(scriptsWithEmpty);
            assertThat(config.getCustomInitScripts()).hasSize(3);
            assertThat(config.getCustomInitScripts().get(0)).isEmpty();
            assertThat(config.getCustomInitScripts().get(1)).isEqualTo("valid script;");
        }
    }

    @SuppressWarnings("EqualsWithItself")
    @Nested
    @DisplayName("equals和hashCode测试")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("相同配置的对象应该相等")
        void shouldBeEqualForSameConfiguration() {
            PlaywrightConfig config1 = new PlaywrightConfig()
                    .setHeadless(false)
                    .setStealthMode(StealthMode.FULL)
                    .setSlowMo(100.0)
                    .setNewContextOptions(null); // 设置相同的上下文选项

            PlaywrightConfig config2 = new PlaywrightConfig()
                    .setHeadless(false)
                    .setStealthMode(StealthMode.FULL)
                    .setSlowMo(100.0)
                    .setNewContextOptions(null); // 设置相同的上下文选项

            assertThat(config1).isEqualTo(config2);
            assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
        }

        @Test
        @DisplayName("不同配置的对象应该不相等")
        void shouldNotBeEqualForDifferentConfiguration() {
            PlaywrightConfig config1 = new PlaywrightConfig().setHeadless(true);
            PlaywrightConfig config2 = new PlaywrightConfig().setHeadless(false);

            assertThat(config1).isNotEqualTo(config2);
            assertThat(config1.hashCode()).isNotEqualTo(config2.hashCode());
        }

        @Test
        @DisplayName("应该正确处理null比较")
        void shouldHandleNullComparison() {
            assertThat(config).isNotEqualTo(null);
            assertThat(config.equals(null)).isFalse();
        }

        @Test
        @DisplayName("应该正确处理不同类型比较")
        void shouldHandleDifferentTypeComparison() {
            assertThat(config).isNotEqualTo("string");
            assertThat(config.equals("string")).isFalse();
        }

        @Test
        @DisplayName("应该正确处理自引用比较")
        void shouldHandleSelfReference() {
            assertThat(config).isEqualTo(config);
            assertThat(config.equals(config)).isTrue();
        }

        @Test
        @DisplayName("包含代理配置的对象应该正确比较")
        void shouldCompareProxyConfiguration() {
            Proxy proxy1 = new Proxy("http://proxy1.com:8080");
            Proxy proxy2 = new Proxy("http://proxy2.com:8080");

            PlaywrightConfig config1 = new PlaywrightConfig().setProxy(proxy1);
            PlaywrightConfig config2 = new PlaywrightConfig().setProxy(proxy2);

            assertThat(config1).isNotEqualTo(config2);
        }

        @Test
        @DisplayName("包含上下文选项的对象应该正确比较")
        void shouldCompareContextOptions() {
            NewContextOptions options1 = new NewContextOptions().setUserAgent("Agent1");
            NewContextOptions options2 = new NewContextOptions().setUserAgent("Agent2");

            PlaywrightConfig config1 = new PlaywrightConfig().setNewContextOptions(options1);
            PlaywrightConfig config2 = new PlaywrightConfig().setNewContextOptions(options2);

            assertThat(config1).isNotEqualTo(config2);
        }

        @Test
        @DisplayName("包含自定义脚本的对象应该正确比较")
        void shouldCompareCustomInitScriptsConfiguration() {
            List<String> scripts1 = Arrays.asList("script1;", "script2;");
            List<String> scripts2 = Arrays.asList("script3;", "script4;");

            PlaywrightConfig config1 = new PlaywrightConfig().setCustomInitScripts(scripts1);
            PlaywrightConfig config2 = new PlaywrightConfig().setCustomInitScripts(scripts2);
            PlaywrightConfig config3 = new PlaywrightConfig().setCustomInitScripts(scripts1);

            assertThat(config1).isNotEqualTo(config2);
            assertThat(config1).isEqualTo(config3);
            assertThat(config1.hashCode()).isEqualTo(config3.hashCode());
        }
    }

    @Nested
    @DisplayName("StealthMode集成测试")
    class StealthModeIntegrationTests {

        @Test
        @DisplayName("应该正确设置所有StealthMode枚举值")
        void shouldSetAllStealthModeValues() {
            for (StealthMode mode : StealthMode.values()) {
                config.setStealthMode(mode);
                assertThat(config.getStealthMode()).isEqualTo(mode);
            }
        }

        @Test
        @DisplayName("应该正确处理StealthMode为null的情况")
        void shouldHandleNullStealthMode() {
            assertThatCode(() -> config.setStealthMode(null))
                    .doesNotThrowAnyException();
            
            assertThat(config.getStealthMode()).isNull();
        }
    }

    @Nested
    @DisplayName("自定义脚本功能测试")
    class CustomInitScriptsTests {

        @Test
        @DisplayName("应该支持单个自定义脚本")
        void shouldSupportSingleCustomScript() {
            String script = "window.customFlag = true;";
            config.setCustomInitScripts(Collections.singletonList(script));

            assertThat(config.getCustomInitScripts()).hasSize(1);
            assertThat(config.getCustomInitScripts().get(0)).isEqualTo(script);
        }

        @Test
        @DisplayName("应该支持多个自定义脚本")
        void shouldSupportMultipleCustomScripts() {
            List<String> scripts = Arrays.asList(
                    "Object.defineProperty(navigator, 'webdriver', {get: () => undefined});",
                    "window.chrome = {runtime: {}};",
                    "Object.defineProperty(navigator, 'languages', {get: () => ['zh-CN', 'zh', 'en']});"
            );
            
            config.setCustomInitScripts(scripts);

            assertThat(config.getCustomInitScripts()).hasSize(3);
            assertThat(config.getCustomInitScripts()).containsExactlyElementsOf(scripts);
        }

        @Test
        @DisplayName("应该正确处理复杂JavaScript脚本")
        void shouldHandleComplexJavaScriptScripts() {
            String complexScript = """
                    (function() {
                        const originalFunction = window.navigator.webdriver;
                        delete window.navigator.webdriver;
                        Object.defineProperty(navigator, 'webdriver', {
                            get: () => undefined,
                            configurable: true
                        });
                    })();
                    """;
            
            config.setCustomInitScripts(Collections.singletonList(complexScript));

            assertThat(config.getCustomInitScripts()).hasSize(1);
            assertThat(config.getCustomInitScripts().get(0)).isEqualTo(complexScript);
        }

        @Test
        @DisplayName("自定义脚本不应影响其他配置")
        void customScriptsShouldNotAffectOtherConfigurations() {
            List<String> scripts = Arrays.asList("script1;", "script2;");
            
            config.setCustomInitScripts(scripts)
                  .setHeadless(false)
                  .setStealthMode(StealthMode.FULL)
                  .setSlowMo(100.0);

            assertThat(config.getCustomInitScripts()).isEqualTo(scripts);
            assertThat(config.isHeadless()).isFalse();
            assertThat(config.getStealthMode()).isEqualTo(StealthMode.FULL);
            assertThat(config.getSlowMo()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("应该支持清空自定义脚本")
        void shouldSupportClearingCustomScripts() {
            // 先设置脚本
            config.setCustomInitScripts(Arrays.asList("script1;", "script2;"));
            assertThat(config.getCustomInitScripts()).hasSize(2);

            // 清空脚本
            config.setCustomInitScripts(Collections.emptyList());
            assertThat(config.getCustomInitScripts()).isEmpty();

            // 设置为null
            config.setCustomInitScripts(null);
            assertThat(config.getCustomInitScripts()).isNull();
        }
    }

    @Nested
    @DisplayName("实际使用场景测试")
    class RealWorldScenarioTests {

        @Test
        @DisplayName("应该正确配置性能优化场景")
        void shouldConfigurePerformanceOptimizedScenario() {
            PlaywrightConfig performanceConfig = new PlaywrightConfig()
                    .setHeadless(true)
                    .setDisableGpu(true)
                    .setDisableImageRender(true)
                    .setStealthMode(StealthMode.LIGHT)
                    .setChromiumSandbox(false);

            assertThat(performanceConfig.isHeadless()).isTrue();
            assertThat(performanceConfig.isDisableGpu()).isTrue();
            assertThat(performanceConfig.isDisableImageRender()).isTrue();
            assertThat(performanceConfig.getStealthMode()).isEqualTo(StealthMode.LIGHT);
            assertThat(performanceConfig.isChromiumSandbox()).isFalse();
        }

        @Test
        @DisplayName("应该正确配置调试场景")
        void shouldConfigureDebugScenario() {
            PlaywrightConfig debugConfig = new PlaywrightConfig()
                    .setHeadless(false)
                    .setStartMaximized(true)
                    .setSlowMo(500.0)
                    .setStealthMode(StealthMode.DISABLED)
                    .setDisableImageRender(false);

            assertThat(debugConfig.isHeadless()).isFalse();
            assertThat(debugConfig.isStartMaximized()).isTrue();
            assertThat(debugConfig.getSlowMo()).isEqualTo(500.0);
            assertThat(debugConfig.getStealthMode()).isEqualTo(StealthMode.DISABLED);
            assertThat(debugConfig.isDisableImageRender()).isFalse();
        }

        @Test
        @DisplayName("应该正确配置高隐蔽性场景")
        void shouldConfigureHighStealthScenario() {
            Proxy proxy = new Proxy("http://stealth-proxy.com:8080");
            
            PlaywrightConfig stealthConfig = new PlaywrightConfig()
                    .setHeadless(true)
                    .setStealthMode(StealthMode.FULL)
                    .setDisableAutomationControlled(true)
                    .setProxy(proxy)
                    .setSlowMo(100.0);

            assertThat(stealthConfig.isHeadless()).isTrue();
            assertThat(stealthConfig.getStealthMode()).isEqualTo(StealthMode.FULL);
            assertThat(stealthConfig.isDisableAutomationControlled()).isTrue();
            assertThat(stealthConfig.getProxy()).isEqualTo(proxy);
            assertThat(stealthConfig.getSlowMo()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("应该正确配置自定义脚本增强反检测场景")
        void shouldConfigureCustomScriptEnhancedStealthScenario() {
            List<String> customStealthScripts = Arrays.asList(
                    "Object.defineProperty(navigator, 'webdriver', {get: () => undefined});",
                    "window.chrome = {runtime: {}, loadTimes: function() {}, csi: function() {}};",
                    "Object.defineProperty(navigator, 'plugins', {get: () => [1, 2, 3, 4, 5]});",
                    "Object.defineProperty(navigator, 'languages', {get: () => ['zh-CN', 'zh', 'en']});"
            );

            PlaywrightConfig enhancedConfig = new PlaywrightConfig()
                    .setStealthMode(StealthMode.LIGHT)  // 轻量级内置反检测
                    .setCustomInitScripts(customStealthScripts)  // 额外的自定义反检测
                    .setHeadless(true)
                    .setDisableAutomationControlled(true);

            assertThat(enhancedConfig.getStealthMode()).isEqualTo(StealthMode.LIGHT);
            assertThat(enhancedConfig.getCustomInitScripts()).hasSize(4);
            assertThat(enhancedConfig.isHeadless()).isTrue();
            assertThat(enhancedConfig.isDisableAutomationControlled()).isTrue();
        }
    }
}
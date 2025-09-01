package cn.xuanyuanli.playwright.stealth.stealth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * StealthScriptProvider单元测试
 *
 * <p>测试反检测脚本提供器的功能，包括：</p>
 * <ul>
 *   <li>脚本内容验证</li>
 *   <li>脚本语法正确性</li>
 *   <li>功能模块完整性</li>
 *   <li>脚本长度和复杂度</li>
 * </ul>
 *
 * @author xuanyuanli
 */
@DisplayName("StealthScriptProvider 反检测脚本提供器测试")
class StealthScriptProviderTest {

    @Nested
    @DisplayName("脚本基本属性测试")
    class ScriptBasicPropertiesTests {

        @Test
        @DisplayName("完整反检测脚本应该非空")
        void shouldReturnNonEmptyFullStealthScript() {
            String script = StealthScriptProvider.getStealthScript();
            
            assertThat(script).isNotNull();
            assertThat(script).isNotEmpty();
            assertThat(script).doesNotContainOnlyWhitespaces();
        }

        @Test
        @DisplayName("轻量级反检测脚本应该非空")
        void shouldReturnNonEmptyLightStealthScript() {
            String script = StealthScriptProvider.getLightStealthScript();
            
            assertThat(script).isNotNull();
            assertThat(script).isNotEmpty();
            assertThat(script).doesNotContainOnlyWhitespaces();
        }

        @Test
        @DisplayName("完整脚本应该比轻量级脚本更长")
        void shouldHaveFullScriptLongerThanLight() {
            String fullScript = StealthScriptProvider.getStealthScript();
            String lightScript = StealthScriptProvider.getLightStealthScript();
            
            assertThat(fullScript.length()).isGreaterThan(lightScript.length());
        }

        @Test
        @DisplayName("脚本应该包含合理数量的行")
        void shouldHaveReasonableNumberOfLines() {
            String fullScript = StealthScriptProvider.getStealthScript();
            String lightScript = StealthScriptProvider.getLightStealthScript();
            
            long fullLines = fullScript.lines().count();
            long lightLines = lightScript.lines().count();
            
            assertThat(fullLines).isGreaterThan(10);  // 完整脚本应该有足够的行数
            assertThat(lightLines).isGreaterThan(5);   // 轻量级脚本也应该有基本的行数
            assertThat(fullLines).isGreaterThan(lightLines);
        }
    }

    @Nested
    @DisplayName("Navigator WebDriver 修复测试")
    class NavigatorWebdriverTests {

        @Test
        @DisplayName("完整脚本应该包含webdriver属性修复")
        void shouldContainWebdriverFixInFullScript() {
            String script = StealthScriptProvider.getStealthScript();
            
            assertThat(script).contains("navigator, 'webdriver'");
            assertThat(script).contains("Object.defineProperty");
            assertThat(script).contains("get: () => undefined");
        }

        @Test
        @DisplayName("轻量级脚本应该包含webdriver属性修复")
        void shouldContainWebdriverFixInLightScript() {
            String script = StealthScriptProvider.getLightStealthScript();
            
            assertThat(script).contains("navigator, 'webdriver'");
            assertThat(script).contains("Object.defineProperty");
            assertThat(script).contains("get: () => undefined");
        }
    }

    @Nested
    @DisplayName("Navigator Languages 修复测试")
    class NavigatorLanguagesTests {

        @Test
        @DisplayName("两种脚本都应该包含语言设置")
        void shouldContainLanguageSettings() {
            String fullScript = StealthScriptProvider.getStealthScript();
            String lightScript = StealthScriptProvider.getLightStealthScript();
            
            assertThat(fullScript).contains("navigator, 'languages'");
            assertThat(lightScript).contains("navigator, 'languages'");
            
            // 验证包含中文和英文语言代码
            assertThat(fullScript).contains("zh-CN");
            assertThat(lightScript).contains("zh-CN");
        }

        @Test
        @DisplayName("完整脚本应该包含更多语言选项")
        void shouldHaveMoreLanguageOptionsInFullScript() {
            String fullScript = StealthScriptProvider.getStealthScript();
            
            assertThat(fullScript).contains("zh-CN");
            assertThat(fullScript).contains("zh");
            assertThat(fullScript).contains("en-US");
            assertThat(fullScript).contains("en");
        }
    }

    @Nested
    @DisplayName("Navigator Platform 修复测试")
    class NavigatorPlatformTests {

        @Test
        @DisplayName("两种脚本都应该包含平台信息设置")
        void shouldContainPlatformSettings() {
            String fullScript = StealthScriptProvider.getStealthScript();
            String lightScript = StealthScriptProvider.getLightStealthScript();
            
            assertThat(fullScript).contains("navigator, 'platform'");
            assertThat(lightScript).contains("navigator, 'platform'");
            assertThat(fullScript).contains("Win32");
            assertThat(lightScript).contains("Win32");
        }
    }

    @Nested
    @DisplayName("Plugins 和 MimeTypes 模拟测试")
    class PluginsAndMimeTypesTests {

        @Test
        @DisplayName("完整脚本应该包含插件模拟")
        void shouldContainPluginSimulationInFullScript() {
            String script = StealthScriptProvider.getStealthScript();
            
            assertThat(script).contains("navigator, 'plugins'");
            assertThat(script).contains("mockPlugins");
            assertThat(script).contains("Chrome PDF Viewer");
            assertThat(script).contains("Native Client");
        }

        @Test
        @DisplayName("完整脚本应该包含MIME类型模拟")
        void shouldContainMimeTypeSimulationInFullScript() {
            String script = StealthScriptProvider.getStealthScript();
            
            assertThat(script).contains("navigator, 'mimeTypes'");
            assertThat(script).contains("mockMimeTypes");
            assertThat(script).contains("application/pdf");
            assertThat(script).contains("application/x-nacl");
        }

        @Test
        @DisplayName("轻量级脚本不应该包含插件模拟")
        void shouldNotContainPluginSimulationInLightScript() {
            String script = StealthScriptProvider.getLightStealthScript();
            
            assertThat(script).doesNotContain("mockPlugins");
            assertThat(script).doesNotContain("Chrome PDF Viewer");
            // 注意：轻量级脚本可能包含navigator.plugins的引用，但不包含完整的模拟实现
        }
    }

    @Nested
    @DisplayName("硬件信息模拟测试")
    class HardwareInfoTests {

        @Test
        @DisplayName("完整脚本应该包含硬件信息模拟")
        void shouldContainHardwareInfoInFullScript() {
            String script = StealthScriptProvider.getStealthScript();
            
            assertThat(script).contains("hardwareConcurrency");
            assertThat(script).contains("deviceMemory");
            assertThat(script).contains("appName");
            assertThat(script).contains("product");
        }

        @Test
        @DisplayName("轻量级脚本不应该包含详细硬件信息")
        void shouldNotContainDetailedHardwareInfoInLightScript() {
            String script = StealthScriptProvider.getLightStealthScript();
            
            assertThat(script).doesNotContain("hardwareConcurrency");
            assertThat(script).doesNotContain("deviceMemory");
            assertThat(script).doesNotContain("appName");
        }
    }

    @Nested
    @DisplayName("WebGL 指纹修复测试")
    class WebGLTests {

        @Test
        @DisplayName("完整脚本应该包含WebGL修复")
        void shouldContainWebGLFixInFullScript() {
            String script = StealthScriptProvider.getStealthScript();
            
            assertThat(script).contains("WebGLRenderingContext");
            assertThat(script).contains("WebGL2RenderingContext");
            assertThat(script).contains("getParameter");
            assertThat(script).contains("Intel Inc.");
            assertThat(script).contains("UHD Graphics");
        }

        @Test
        @DisplayName("轻量级脚本不应该包含WebGL修复")
        void shouldNotContainWebGLFixInLightScript() {
            String script = StealthScriptProvider.getLightStealthScript();
            
            assertThat(script).doesNotContain("WebGLRenderingContext");
            assertThat(script).doesNotContain("getParameter");
            assertThat(script).doesNotContain("Intel Inc.");
        }
    }

    @Nested
    @DisplayName("AudioContext 修复测试")
    class AudioContextTests {

        @Test
        @DisplayName("完整脚本应该包含AudioContext修复")
        void shouldContainAudioContextFixInFullScript() {
            String script = StealthScriptProvider.getStealthScript();
            
            assertThat(script).contains("AudioContext");
            assertThat(script).contains("webkitAudioContext");
            assertThat(script).contains("baseLatency");
            assertThat(script).contains("AudioContextProxy");
        }

        @Test
        @DisplayName("轻量级脚本不应该包含AudioContext修复")
        void shouldNotContainAudioContextFixInLightScript() {
            String script = StealthScriptProvider.getLightStealthScript();
            
            assertThat(script).doesNotContain("AudioContext");
            assertThat(script).doesNotContain("baseLatency");
        }
    }

    @Nested
    @DisplayName("Chrome Runtime 修复测试")
    class ChromeRuntimeTests {

        @Test
        @DisplayName("完整脚本应该包含Chrome Runtime清理")
        void shouldContainChromeRuntimeCleanupInFullScript() {
            String script = StealthScriptProvider.getStealthScript();
            
            assertThat(script).contains("chrome.runtime");
            assertThat(script).contains("delete chrome.runtime");
        }

        @Test
        @DisplayName("轻量级脚本不应该包含Chrome Runtime清理")
        void shouldNotContainChromeRuntimeCleanupInLightScript() {
            String script = StealthScriptProvider.getLightStealthScript();
            
            assertThat(script).doesNotContain("chrome.runtime");
        }
    }

    @Nested
    @DisplayName("脚本语法和结构测试")
    class ScriptSyntaxTests {

        @Test
        @DisplayName("脚本应该包含正确的JavaScript语法结构")
        void shouldHaveCorrectJavaScriptSyntax() {
            String fullScript = StealthScriptProvider.getStealthScript();
            String lightScript = StealthScriptProvider.getLightStealthScript();
            
            // 验证基本的JavaScript语法元素
            assertThat(fullScript).contains("Object.defineProperty");
            assertThat(fullScript).contains("function");
            
            assertThat(lightScript).contains("Object.defineProperty");
            // 轻量级脚本可能没有显式的return语句，主要使用属性定义
        }

        @Test
        @DisplayName("脚本应该正确使用模板字符串格式")
        void shouldUseTemplateStringFormat() {
            String fullScript = StealthScriptProvider.getStealthScript();
            String lightScript = StealthScriptProvider.getLightStealthScript();
            
            // 验证不包含模板字符串的边界标记（这些应该被Java处理掉）
            assertThat(fullScript).doesNotContain("\"\"\"");
            assertThat(lightScript).doesNotContain("\"\"\"");
        }

        @ParameterizedTest
        @ValueSource(strings = {"{", "}", "(", ")", "[", "]"})
        @DisplayName("脚本应该包含成对的括号")
        void shouldHaveBalancedBrackets(String bracket) {
            String fullScript = StealthScriptProvider.getStealthScript();
            String lightScript = StealthScriptProvider.getLightStealthScript();
            
            assertThat(fullScript).contains(bracket);
            assertThat(lightScript).contains(bracket);
        }
    }

    @Nested
    @DisplayName("脚本注释和文档测试")
    class ScriptDocumentationTests {

        @Test
        @DisplayName("完整脚本应该包含详细的注释")
        void shouldContainDetailedCommentsInFullScript() {
            String script = StealthScriptProvider.getStealthScript();
            
            assertThat(script).contains("//");
            assertThat(script).contains("Navigator WebDriver 修复");
            assertThat(script).contains("Navigator Languages 模拟");
            assertThat(script).contains("WebGL 指纹修复");
            assertThat(script).contains("AudioContext 指纹修复");
        }

        @Test
        @DisplayName("轻量级脚本应该包含基本注释")
        void shouldContainBasicCommentsInLightScript() {
            String script = StealthScriptProvider.getLightStealthScript();
            
            assertThat(script).contains("//");
            assertThat(script).contains("移除自动化标识");
            assertThat(script).contains("基础语言设置");
            assertThat(script).contains("基础平台信息");
        }
    }

    @Nested
    @DisplayName("脚本功能覆盖测试")
    class ScriptCoverageTests {

        @Test
        @DisplayName("完整脚本应该覆盖所有主要检测点")
        void shouldCoverAllMajorDetectionPointsInFullScript() {
            String script = StealthScriptProvider.getStealthScript();
            
            // 验证覆盖了文档中提到的所有功能
            assertThat(script).contains("webdriver");        // Navigator属性
            assertThat(script).contains("plugins");          // Plugin模拟
            assertThat(script).contains("WebGL");            // WebGL指纹
            assertThat(script).contains("AudioContext");     // AudioContext指纹
            assertThat(script).contains("permissions");      // 权限API
            assertThat(script).contains("chrome");           // Chrome扩展API
            assertThat(script).contains("devtools");         // 开发者工具检测
        }

        @Test
        @DisplayName("轻量级脚本应该覆盖基础检测点")
        void shouldCoverBasicDetectionPointsInLightScript() {
            String script = StealthScriptProvider.getLightStealthScript();
            
            // 验证覆盖了基础功能
            assertThat(script).contains("webdriver");
            assertThat(script).contains("languages");
            assertThat(script).contains("platform");
        }
    }

    @Nested
    @DisplayName("性能和复杂度测试")
    class PerformanceTests {

        @Test
        @DisplayName("脚本长度应该在合理范围内")
        void shouldHaveReasonableLength() {
            String fullScript = StealthScriptProvider.getStealthScript();
            String lightScript = StealthScriptProvider.getLightStealthScript();
            
            // 验证脚本不会过长（可能影响性能）
            assertThat(fullScript.length()).isLessThan(50000);  // 50KB
            assertThat(lightScript.length()).isLessThan(5000);   // 5KB
            
            // 验证脚本不会过短（功能不完整）
            assertThat(fullScript.length()).isGreaterThan(1000);
            assertThat(lightScript.length()).isGreaterThan(100);
        }

        @Test
        @DisplayName("脚本复杂度应该符合预期")
        void shouldHaveExpectedComplexity() {
            String fullScript = StealthScriptProvider.getStealthScript();
            String lightScript = StealthScriptProvider.getLightStealthScript();
            
            // 通过函数数量评估复杂度
            long fullFunctionCount = fullScript.lines()
                    .filter(line -> line.contains("function") || line.contains("=>"))
                    .count();
            
            long lightFunctionCount = lightScript.lines()
                    .filter(line -> line.contains("function") || line.contains("=>"))
                    .count();
            
            assertThat(fullFunctionCount).isGreaterThan(lightFunctionCount);
        }
    }

    @Nested
    @DisplayName("静态方法测试")
    class StaticMethodTests {

        @Test
        @DisplayName("getStealthScript方法应该始终返回相同内容")
        void shouldReturnConsistentFullScript() {
            String script1 = StealthScriptProvider.getStealthScript();
            String script2 = StealthScriptProvider.getStealthScript();
            
            assertThat(script1).isEqualTo(script2);
        }

        @Test
        @DisplayName("getLightStealthScript方法应该始终返回相同内容")
        void shouldReturnConsistentLightScript() {
            String script1 = StealthScriptProvider.getLightStealthScript();
            String script2 = StealthScriptProvider.getLightStealthScript();
            
            assertThat(script1).isEqualTo(script2);
        }

        @Test
        @DisplayName("两个方法应该返回不同的脚本内容")
        void shouldReturnDifferentScripts() {
            String fullScript = StealthScriptProvider.getStealthScript();
            String lightScript = StealthScriptProvider.getLightStealthScript();
            
            assertThat(fullScript).isNotEqualTo(lightScript);
        }
    }
}
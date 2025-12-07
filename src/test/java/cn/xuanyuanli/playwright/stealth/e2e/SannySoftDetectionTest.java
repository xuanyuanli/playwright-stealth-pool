package cn.xuanyuanli.playwright.stealth.e2e;

import cn.xuanyuanli.playwright.stealth.config.PlaywrightConfig;
import cn.xuanyuanli.playwright.stealth.config.StealthMode;
import cn.xuanyuanli.playwright.stealth.manager.PlaywrightManager;
import cn.xuanyuanli.playwright.stealth.TestConditions;
import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.assertj.core.api.Assertions.*;

/**
 * SannySoft Bot检测测试
 *
 * <p>使用 https://bot.sannysoft.com/ 进行真实的反检测效果验证</p>
 * <p>该网站是一个常用的浏览器自动化检测工具，可以检测多种自动化特征</p>
 *
 * @author xuanyuanli
 */
@DisplayName("SannySoft Bot检测测试")
@Tag("e2e")
@Tag("slow")
@Tag("external")
class SannySoftDetectionTest {

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
    @DisplayName("应该通过SannySoft的WebDriver检测")
    @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isE2ETestsEnabled")
    void shouldPassWebDriverDetection() {
        PlaywrightConfig config = new PlaywrightConfig()
                .setHeadless(false)
                .setStealthMode(StealthMode.FULL)
                .setDisableAutomationControlled(true);

        manager.execute(config, page -> {
            // 访问SannySoft检测页面
            page.navigate("https://bot.sannysoft.com/");
            
            // 等待页面加载完成
            page.waitForLoadState();
            page.waitForTimeout(2000); // 等待检测脚本执行完成
            
            // 检查 WebDriver 检测结果
            // 查找包含 "WebDriver" 文本的行
            Locator webdriverRow = page.locator("table tr:has(td:text-is('WebDriver'))");
            if (webdriverRow.count() > 0) {
                String rowText = webdriverRow.textContent();
                System.out.println("WebDriver检测结果: " + rowText);
                // 验证不包含 "failed"
                assertThat(rowText.toLowerCase()).doesNotContain("failed");
            }
            
            // 检查 WebDriver(New) 检测结果
            Locator webdriverNewRow = page.locator("table tr:has(td:text-is('WebDriver (New)'))");
            if (webdriverNewRow.count() > 0) {
                String rowText = webdriverNewRow.textContent();
                System.out.println("WebDriver(New)检测结果: " + rowText);
                // 验证不包含 "failed"
                assertThat(rowText.toLowerCase()).doesNotContain("failed");
            }
        });
    }

    @Test
    @DisplayName("应该通过SannySoft的Plugins检测")
    @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isE2ETestsEnabled")
    void shouldPassPluginsDetection() {
        PlaywrightConfig config = new PlaywrightConfig()
                .setHeadless(true)
                .setStealthMode(StealthMode.FULL)
                .setDisableAutomationControlled(true);

        manager.execute(config, page -> {
            // 访问SannySoft检测页面
            page.navigate("https://bot.sannysoft.com/");
            
            // 等待页面加载完成
            page.waitForLoadState();
            page.waitForTimeout(2000);
            
            // 检查 Plugins 类型检测结果
            Locator pluginsRow = page.locator("table tr:has(td:text('Plugins is of type'))");
            if (pluginsRow.count() > 0) {
                String rowText = pluginsRow.textContent();
                System.out.println("Plugins类型检测结果: " + rowText);
                // 验证不包含 "failed"
                assertThat(rowText.toLowerCase()).doesNotContain("failed");
            }
            
            // 检查 Plugins 长度检测结果
            Locator pluginsLengthRow = page.locator("table tr:has(td:text('Plugins Length'))");
            if (pluginsLengthRow.count() > 0) {
                String rowText = pluginsLengthRow.textContent();
                System.out.println("Plugins长度检测结果: " + rowText);
                // 验证不包含 "failed"
                assertThat(rowText.toLowerCase()).doesNotContain("failed");
            }
        });
    }

    @Test
    @DisplayName("应该通过SannySoft的主要检测项")
    @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isE2ETestsEnabled")
    void shouldPassMajorDetections() {
        PlaywrightConfig config = new PlaywrightConfig()
                .setHeadless(true)
                .setStealthMode(StealthMode.FULL)
                .setDisableAutomationControlled(true);

        manager.execute(config, page -> {
            // 访问SannySoft检测页面
            page.navigate("https://bot.sannysoft.com/");
            
            // 等待页面加载完成
            page.waitForLoadState();
            page.waitForTimeout(3000); // 等待所有检测完成
            
            // 获取所有检测结果行
            Locator allRows = page.locator("table tr");
            int rowCount = allRows.count();
            
            System.out.println("=== SannySoft 检测结果 ===");
            int passedCount = 0;
            int failedCount = 0;
            
            for (int i = 0; i < rowCount; i++) {
                String rowText = allRows.nth(i).textContent();
                if (rowText != null && !rowText.trim().isEmpty()) {
                    System.out.println(rowText);
                    if (rowText.toLowerCase().contains("failed")) {
                        failedCount++;
                    } else if (rowText.toLowerCase().contains("passed") || 
                               rowText.contains("missing") ||
                               rowText.matches(".*\\d+.*")) {
                        passedCount++;
                    }
                }
            }
            
            System.out.println("=== 检测统计 ===");
            System.out.println("通过: " + passedCount);
            System.out.println("失败: " + failedCount);
            
            // 验证关键检测项通过
            // WebDriver 检测
            Object webdriver = page.evaluate("navigator.webdriver");
            System.out.println("navigator.webdriver = " + webdriver);
            assertThat(webdriver == null || webdriver.equals(false) || "undefined".equals(String.valueOf(webdriver)))
                    .as("WebDriver应该被隐藏").isTrue();
            
            // Plugins instanceof PluginArray 检测
            Object pluginsInstanceOf = page.evaluate("navigator.plugins instanceof PluginArray");
            System.out.println("navigator.plugins instanceof PluginArray = " + pluginsInstanceOf);
            assertThat(pluginsInstanceOf).as("Plugins应该是PluginArray类型").isEqualTo(true);
            
            // WebDriver属性描述符检测
            Object webdriverDescriptor = page.evaluate(
                "Object.getOwnPropertyDescriptor(Navigator.prototype, 'webdriver')");
            System.out.println("WebDriver属性描述符 = " + webdriverDescriptor);
            assertThat(webdriverDescriptor).as("WebDriver属性描述符应该不存在").isNull();
        });
    }
}


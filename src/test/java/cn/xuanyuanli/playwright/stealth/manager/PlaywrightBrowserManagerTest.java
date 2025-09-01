package cn.xuanyuanli.playwright.stealth.manager;

import cn.xuanyuanli.core.util.Images;
import cn.xuanyuanli.playwright.stealth.config.PlaywrightConfig;
import cn.xuanyuanli.playwright.stealth.config.StealthMode;
import cn.xuanyuanli.playwright.stealth.TestConditions;
import com.microsoft.playwright.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.util.stream.IntStream;

/**
 * PlaywrightBrowserManager测试类
 * 
 * <p>提供PlaywrightBrowserManager的功能测试和性能测试</p>
 *
 * @author xuanyuanli
 */
class PlaywrightBrowserManagerTest {

    private PlaywrightBrowserManager browserManager;
    private PlaywrightConfig config;

    @BeforeEach
    void setUp() {
        // 创建测试配置
        config = new PlaywrightConfig()
                .setHeadless(true)
                .setStealthMode(StealthMode.FULL)
                .setDisableImageRender(false); // 为了测试图片下载，启用图片渲染
        
        // 创建Browser管理器，池大小为4
        browserManager = new PlaywrightBrowserManager(config, 4);
    }

    @AfterEach
    void tearDown() {
        if (browserManager != null) {
            browserManager.close();
        }
    }

    /**
     * 测试基本的页面访问功能
     */
    @Test
    @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isIntegrationTestsEnabled")
    void testBasicPageNavigation() {
        browserManager.execute(page -> {
            page.navigate("https://www.baidu.com");
            String title = page.title();
            System.out.println("页面标题: " + title);
            assert title.contains("百度");
        });
    }

    /**
     * 测试并发访问和连接池功能
     */
    @Test
    @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isIntegrationTestsEnabled")
    void testConcurrentAccess() {
        System.out.println("开始并发测试，初始状态: " + browserManager.getPoolStatus());
        
        // 并发执行16个任务，但连接池只有4个Browser实例
        IntStream.range(0, 16).parallel().forEach(i -> {
            browserManager.execute(page -> {
                page.navigate("https://httpbin.org/delay/1");
                String content = page.content();
                System.out.printf("任务 %d 完成，内容长度: %d%n", i, content.length());
            });
        });
        
        System.out.println("并发测试完成，最终状态: " + browserManager.getPoolStatus());
    }

    /**
     * 测试图片下载和处理功能
     * 
     * <p>注意：该测试访问外部URL，可能因为网络问题而失败</p>
     */
    @Test
    @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isIntegrationTestsEnabled")
    void testImageDownload() {
        // 并发下载图片测试
        IntStream.range(0, 8).parallel().forEach(i -> {
            browserManager.execute(page -> {
                String imageUrl = "https://httpbin.org/image/png";
                try {
                    Response response = page.navigate(imageUrl);
                    byte[] body = response.body();
                    boolean isImage = Images.isImageByTika(body);
                    System.out.printf("任务 %d: 图片大小 %d bytes, 是否为图片: %s%n", 
                                     i, body.length, isImage);
                } catch (Exception e) {
                    System.err.printf("任务 %d 失败: %s%n", i, e.getMessage());
                }
            });
        });
    }

    /**
     * 测试反检测脚本功能
     */
    @Test
    @EnabledIf("cn.xuanyuanli.playwright.stealth.TestConditions#isIntegrationTestsEnabled")
    void testStealthFeatures() {
        browserManager.execute(page -> {
            // 访问一个可以检测自动化的网站
            page.navigate("https://bot.sannysoft.com/");
            
            // 等待页面加载完成
            page.waitForTimeout(2000);
            
            // 检查webdriver属性是否被成功隐藏
            Object webdriverValue = page.evaluate("navigator.webdriver");
            System.out.println("navigator.webdriver: " + webdriverValue);
            
            // 检查languages属性
            Object languages = page.evaluate("navigator.languages");
            System.out.println("navigator.languages: " + languages);
            
            // 检查plugins数量
            Object pluginsLength = page.evaluate("navigator.plugins.length");
            System.out.println("navigator.plugins.length: " + pluginsLength);
        });
    }

    /**
     * 测试连接池管理功能
     */
    @Test
    void testPoolManagement() {
        System.out.println("初始状态: " + browserManager.getPoolStatus());
        
        // 预热连接池
        browserManager.warmUpPool(2);
        System.out.println("预热后状态: " + browserManager.getPoolStatus());
        
        // 清理空闲连接
        browserManager.evictIdleBrowsers();
        System.out.println("清理后状态: " + browserManager.getPoolStatus());
        
        // 输出统计信息
        System.out.println("详细统计: " + browserManager.getPoolStatistics());
    }
}
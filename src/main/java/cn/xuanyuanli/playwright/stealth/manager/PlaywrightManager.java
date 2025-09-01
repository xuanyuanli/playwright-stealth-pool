package cn.xuanyuanli.playwright.stealth.manager;

import cn.xuanyuanli.playwright.stealth.config.PlaywrightConfig;
import cn.xuanyuanli.playwright.stealth.pool.PlaywrightFactory;
import cn.xuanyuanli.playwright.stealth.stealth.StealthScriptProvider;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType.LaunchOptions;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Playwright管理器
 * 
 * <p>提供Playwright实例的连接池管理和统一的执行接口。该管理器负责：</p>
 * <ul>
 *   <li>维护Playwright实例连接池，提高资源复用率</li>
 *   <li>提供统一的页面执行接口</li>
 *   <li>集成反检测脚本注入功能</li>
 *   <li>管理浏览器启动参数和配置</li>
 * </ul>
 * 
 * <p><strong>使用示例：</strong></p>
 * <pre>{@code
 * // 创建管理器实例，设置连接池大小为8
 * PlaywrightManager manager = new PlaywrightManager(8);
 * 
 * // 使用默认配置执行页面操作
 * manager.execute(page -> {
 *     page.navigate("https://example.com");
 *     System.out.println(page.title());
 * });
 * 
 * // 使用自定义配置
 * PlaywrightConfig config = new PlaywrightConfig()
 *     .setHeadless(false)
 *     .setStealthMode(StealthMode.LIGHT);
 * manager.execute(config, page -> {
 *     // 页面操作
 * });
 *
 * // 记得关闭管理器释放资源
 * manager.close();
 * }</pre>
 *
 * @author xuanyuanli
 */
@Slf4j
public class PlaywrightManager implements AutoCloseable {

    /**
     * Playwright实例连接池
     */
    private final GenericObjectPool<Playwright> pool;

    /**
     * 创建Playwright管理器
     *
     * @param capacity 连接池最大容量
     */
    public PlaywrightManager(int capacity) {
        this(null, capacity);
    }

    /**
     * 创建Playwright管理器
     *
     * @param poolConfig 连接池配置，为null时使用默认配置
     * @param capacity   连接池最大容量
     */
    public PlaywrightManager(GenericObjectPoolConfig<Playwright> poolConfig, int capacity) {
        if (poolConfig == null) {
            poolConfig = new GenericObjectPoolConfig<>();
            poolConfig.setMaxTotal(capacity);
            poolConfig.setMinIdle(1);
            poolConfig.setMaxIdle(capacity);
            poolConfig.setTestOnBorrow(false);
            poolConfig.setTestOnReturn(false);
        }

        this.pool = new GenericObjectPool<>(new PlaywrightFactory(), poolConfig);
        log.info("PlaywrightManager initialized with pool capacity: {}", capacity);
    }

    /**
     * 使用默认配置执行页面操作
     *
     * @param pageConsumer 页面操作函数
     * @throws RuntimeException 当操作执行失败时
     */
    public void execute(Consumer<Page> pageConsumer) {
        execute(null, null, pageConsumer);
    }

    /**
     * 使用指定配置执行页面操作
     *
     * @param config       Playwright配置，为null时使用默认配置
     * @param pageConsumer 页面操作函数
     * @throws RuntimeException 当操作执行失败时
     */
    public void execute(PlaywrightConfig config, Consumer<Page> pageConsumer) {
        execute(config, null, pageConsumer);
    }

    /**
     * 使用指定配置执行页面操作，支持浏览器上下文自定义
     *
     * @param config                   Playwright配置，为null时使用默认配置
     * @param browserContextConsumer  浏览器上下文配置函数，可为null
     * @param pageConsumer            页面操作函数
     * @throws RuntimeException       当操作执行失败时
     */
    public void execute(PlaywrightConfig config, Consumer<BrowserContext> browserContextConsumer, 
                       Consumer<Page> pageConsumer) {
        if (config == null) {
            config = new PlaywrightConfig();
        }

        Playwright playwright = null;
        try {
            playwright = pool.borrowObject();
            executeInternal(config, browserContextConsumer, pageConsumer, playwright);
        } catch (Exception e) {
            log.error("Failed to execute playwright operation", e);
            throw new RuntimeException("Playwright operation failed", e);
        } finally {
            if (playwright != null) {
                try {
                    pool.returnObject(playwright);
                } catch (Exception e) {
                    log.warn("Failed to return playwright object to pool", e);
                }
            }
        }
    }

    /**
     * 静态方法：使用现有Playwright实例执行操作
     *
     * @param config                   配置
     * @param browserContextConsumer  浏览器上下文配置函数
     * @param pageConsumer            页面操作函数
     * @param playwright              Playwright实例
     */
    public static void executeWithPlaywright(PlaywrightConfig config, 
                                           Consumer<BrowserContext> browserContextConsumer,
                                           Consumer<Page> pageConsumer, 
                                           Playwright playwright) {
        if (config == null) {
            config = new PlaywrightConfig();
        }

        try (Browser browser = createBrowser(config, playwright)) {
            executeWithBrowser(config, browserContextConsumer, pageConsumer, browser);
        }
    }

    /**
     * 静态方法：使用现有Browser实例执行操作
     *
     * @param config                   配置
     * @param browserContextConsumer  浏览器上下文配置函数
     * @param pageConsumer            页面操作函数
     * @param browser                 Browser实例
     */
    public static void executeWithBrowser(PlaywrightConfig config, 
                                        Consumer<BrowserContext> browserContextConsumer,
                                        Consumer<Page> pageConsumer, 
                                        Browser browser) {
        if (config == null) {
            config = new PlaywrightConfig();
        }

        BrowserContext browserContext = null;
        Page page = null;
        try {
            // 创建浏览器上下文
            browserContext = browser.newContext(config.getNewContextOptions());
            
            // 应用上下文配置
            if (browserContextConsumer != null) {
                browserContextConsumer.accept(browserContext);
            }

            // 创建页面
            page = browserContext.newPage();

            // 根据配置注入内置反检测脚本
            if (config.getStealthMode().isEnabled()) {
                if (config.getStealthMode().isLight()) {
                    page.addInitScript(StealthScriptProvider.getLightStealthScript());
                    log.debug("Light stealth script injected");
                } else if (config.getStealthMode().isFull()) {
                    page.addInitScript(StealthScriptProvider.getStealthScript());
                    log.debug("Full stealth script injected");
                }
            }

            // 注入自定义初始化脚本（在内置脚本之后执行）
            if (config.getCustomInitScripts() != null && !config.getCustomInitScripts().isEmpty()) {
                for (int i = 0; i < config.getCustomInitScripts().size(); i++) {
                    String script = config.getCustomInitScripts().get(i);
                    page.addInitScript(script);
                    log.debug("Custom init script [{}] injected: {} chars", i + 1, script.length());
                }
                log.debug("Total {} custom init scripts injected", config.getCustomInitScripts().size());
            }

            // 执行页面操作
            pageConsumer.accept(page);
            
        } catch (Exception e) {
            log.error("Page operation failed", e);
            throw new RuntimeException("Page operation failed", e);
        } finally {
            // 确保资源正确释放
            if (page != null) {
                try {
                    page.close();
                } catch (Exception e) {
                    log.warn("Failed to close page", e);
                }
            }
            if (browserContext != null) {
                try {
                    browserContext.close();
                } catch (Exception e) {
                    log.warn("Failed to close browser context", e);
                }
            }
        }
    }

    /**
     * 创建配置好的Browser实例
     *
     * @param config     配置
     * @param playwright Playwright实例
     * @return 配置好的Browser实例
     */
    public static Browser createBrowser(PlaywrightConfig config, Playwright playwright) {
        if (config == null) {
            config = new PlaywrightConfig();
        }

        // 构建启动参数
        List<String> args = new ArrayList<>();
        
        if (config.isDisableImageRender()) {
            args.add("--blink-settings=imagesEnabled=false");
        }
        
        if (config.isDisableAutomationControlled()) {
            args.add("--disable-blink-features=AutomationControlled");
        }
        
        if (config.isDisableGpu()) {
            args.add("--disable-gpu");
        }
        
        if (config.isStartMaximized()) {
            args.add("--start-maximized");
        }

        // 创建启动选项
        LaunchOptions options = new LaunchOptions()
                .setArgs(args)
                .setHeadless(config.isHeadless())
                .setChromiumSandbox(config.isChromiumSandbox());

        // 设置可选参数
        if (config.getSlowMo() != null) {
            options.setSlowMo(config.getSlowMo());
        }
        
        if (config.getProxy() != null) {
            options.setProxy(config.getProxy());
        }

        log.debug("Launching browser with config: headless={}, args={}", 
                 config.isHeadless(), args);

        return playwright.chromium().launch(options);
    }

    /**
     * 获取连接池状态信息
     *
     * @return 连接池状态描述
     */
    public String getPoolStatus() {
        return String.format("Pool Status - Active: %d, Idle: %d, Total: %d/%d",
                pool.getNumActive(), pool.getNumIdle(), 
                pool.getNumActive() + pool.getNumIdle(), pool.getMaxTotal());
    }

    /**
     * 内部执行方法
     */
    private void executeInternal(PlaywrightConfig config, 
                               Consumer<BrowserContext> browserContextConsumer,
                               Consumer<Page> pageConsumer, 
                               Playwright playwright) {
        executeWithPlaywright(config, browserContextConsumer, pageConsumer, playwright);
    }

    @Override
    public void close() {
        try {
            pool.close();
            log.info("PlaywrightManager closed successfully");
        } catch (Exception e) {
            log.error("Error closing PlaywrightManager", e);
        }
    }
}
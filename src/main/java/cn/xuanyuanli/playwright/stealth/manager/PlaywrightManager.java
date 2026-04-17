package cn.xuanyuanli.playwright.stealth.manager;

import cn.xuanyuanli.playwright.stealth.config.PlaywrightConfig;
import cn.xuanyuanli.playwright.stealth.pool.PlaywrightFactory;
import cn.xuanyuanli.playwright.stealth.stealth.StealthScriptProvider;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType.LaunchOptions;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
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
     * Browser创建并发控制信号量
     */
    private final Semaphore browserCreationSemaphore;

    /**
     * 默认最大重试次数
     */
    private static final int DEFAULT_MAX_RETRIES = 3;

    /**
     * 默认重试间隔（毫秒）
     */
    private static final long DEFAULT_RETRY_DELAY_MS = 500;

    /**
     * 连接池最大等待时间（毫秒）
     */
    private static final long DEFAULT_MAX_WAIT_MILLIS = 30000;

    /**
     * 创建Playwright管理器
     *
     * @param capacity 连接池最大容量
     */
    public PlaywrightManager(int capacity) {
        this(null, capacity, getDefaultConcurrency());
    }

    /**
     * 创建Playwright管理器
     *
     * @param capacity       连接池最大容量
     * @param maxConcurrency 最大Browser创建并发数（建议不超过系统资源限制）
     */
    public PlaywrightManager(int capacity, int maxConcurrency) {
        this(null, capacity, maxConcurrency);
    }

    /**
     * 创建Playwright管理器
     *
     * @param poolConfig     连接池配置，为null时使用默认配置
     * @param capacity       连接池最大容量
     * @param maxConcurrency 最大Browser创建并发数
     */
    public PlaywrightManager(GenericObjectPoolConfig<Playwright> poolConfig, int capacity, int maxConcurrency) {
        if (poolConfig == null) {
            poolConfig = new GenericObjectPoolConfig<>();
            poolConfig.setMaxTotal(capacity);
            poolConfig.setMinIdle(1);
            poolConfig.setMaxIdle(capacity);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(false);
            poolConfig.setBlockWhenExhausted(true);
            poolConfig.setMaxWait(Duration.ofMillis(DEFAULT_MAX_WAIT_MILLIS));
        }

        this.pool = new GenericObjectPool<>(new PlaywrightFactory(), poolConfig);
        this.browserCreationSemaphore = new Semaphore(maxConcurrency);
        log.info("PlaywrightManager initialized with pool capacity: {}, max browser concurrency: {}", capacity, maxConcurrency);
    }

    /**
     * 获取默认并发数（CPU核数-1，最小为1）
     *
     * @return 默认并发数
     */
    private static int getDefaultConcurrency() {
        int processors = Runtime.getRuntime().availableProcessors();
        int concurrency = Math.max(1, processors - 1);
        log.debug("Default browser concurrency calculated: {} (based on {} processors)", concurrency, processors);
        return concurrency;
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
        executeWithRetry(config, browserContextConsumer, pageConsumer);
    }

    /**
     * 带重试机制的执行方法
     *
     * @param config                 Playwright配置
     * @param browserContextConsumer 浏览器上下文配置函数
     * @param pageConsumer           页面操作函数
     */
    private void executeWithRetry(PlaywrightConfig config, Consumer<BrowserContext> browserContextConsumer,
                                  Consumer<Page> pageConsumer) {
        if (config == null) {
            config = new PlaywrightConfig();
        }

        int attempt = 0;
        long retryDelay = DEFAULT_RETRY_DELAY_MS;

        while (true) {
            attempt++;
            Playwright playwright = null;

            try {
                playwright = pool.borrowObject();
                executeInternal(config, browserContextConsumer, pageConsumer, playwright);

                // 如果之前有过重试，记录成功恢复
                if (attempt > 1) {
                    log.info("Playwright operation succeeded after {} attempt(s)", attempt);
                }
                return;

            } catch (Exception e) {
                // 判断是否需要重试
                if (attempt <= PlaywrightManager.DEFAULT_MAX_RETRIES && isRetryableError(e)) {
                    log.warn("Playwright operation failed (attempt {}/{}), will retry in {}ms. Error: {}",
                            attempt, PlaywrightManager.DEFAULT_MAX_RETRIES + 1, retryDelay, e.getMessage());

                    // 归还连接池对象
                    if (playwright != null) {
                        try {
                            pool.returnObject(playwright);
                            playwright = null;
                        } catch (Exception returnEx) {
                            log.warn("Failed to return playwright object to pool during retry: {}", returnEx.getMessage());
                        }
                    }

                    // 等待后重试
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Playwright operation interrupted during retry", ie);
                    }

                    // 指数退避
                    retryDelay *= 2;
                    continue;
                }

                // 不需要重试或重试次数用尽
                log.error("Failed to execute playwright operation after {} attempt(s)", attempt, e);
                throw new RuntimeException("Playwright operation failed", e);

            } finally {
                if (playwright != null) {
                    try {
                        pool.returnObject(playwright);
                    } catch (Exception returnEx) {
                        log.warn("Failed to return playwright object to pool: {}", returnEx.getMessage());
                    }
                }
            }
        }
    }

    /**
     * 判断错误是否可重试
     *
     * @param e 异常
     * @return true表示可重试
     */
    private boolean isRetryableError(Exception e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof PlaywrightException) {
                String message = cause.getMessage();
                if (message != null) {
                    String upperMessage = message.toUpperCase();
                    // 资源类错误
                    if (upperMessage.contains("EAGAIN") ||
                        upperMessage.contains("EMFILE") ||
                        upperMessage.contains("ENFILE") ||
                        upperMessage.contains("RESOURCE TEMPORARILY UNAVAILABLE")) {
                        log.debug("Detected retryable resource error: {}", message);
                        return true;
                    }
                    // spawn 失败
                    if (message.contains("spawn") && message.contains("Error")) {
                        log.debug("Detected retryable spawn error: {}", message);
                        return true;
                    }
                    // 连接类错误
                    if (upperMessage.contains("ECONNREFUSED") ||
                        upperMessage.contains("ECONNRESET") ||
                        upperMessage.contains("ETIMEDOUT")) {
                        log.debug("Detected retryable connection error: {}", message);
                        return true;
                    }
                }
            }
            cause = cause.getCause();
        }
        return false;
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
        boolean pageClosed;
        boolean contextClosed;
        
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
            // 确保资源正确释放 - 使用独立的方法处理每个资源
            // 这样可以确保即使一个资源关闭失败，其他资源仍会被尝试关闭
            pageClosed = closeResourceSafely(page, "page");
            contextClosed = closeResourceSafely(browserContext, "browser context");
            
            // 记录资源释放状态
            if (!pageClosed) {
                log.warn("Page may not have been properly closed");
            }
            if (!contextClosed) {
                log.warn("BrowserContext may not have been properly closed");
            }
            
            log.debug("Resource cleanup completed - Page closed: {}, Context closed: {}", pageClosed, contextClosed);
        }
    }
    
    /**
     * 安全地关闭资源，捕获并记录任何异常
     * 
     * @param closeable 可关闭的资源
     * @param resourceName 资源名称（用于日志）
     * @return true如果资源成功关闭或不需要关闭，false如果关闭失败
     */
    private static boolean closeResourceSafely(AutoCloseable closeable, String resourceName) {
        if (closeable == null) {
            return true; // 资源不存在，视为已处理
        }
        
        try {
            closeable.close();
            log.debug("Successfully closed {}", resourceName);
            return true;
        } catch (Exception e) {
            log.warn("Failed to close {}: {}", resourceName, e.getMessage());
            return false;
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
     * 内部执行方法（带并发控制）
     */
    private void executeInternal(PlaywrightConfig config,
                               Consumer<BrowserContext> browserContextConsumer,
                               Consumer<Page> pageConsumer,
                               Playwright playwright) {
        if (config == null) {
            config = new PlaywrightConfig();
        }

        // 使用信号量控制 Browser 创建并发
        long acquireStart = System.currentTimeMillis();
        try {
            browserCreationSemaphore.acquire();
            long acquireTime = System.currentTimeMillis() - acquireStart;
            if (acquireTime > 100) {
                log.debug("Waited {}ms for browser creation permit", acquireTime);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for browser creation permit", e);
        }

        try (Browser browser = createBrowser(config, playwright)) {
            executeWithBrowser(config, browserContextConsumer, pageConsumer, browser);
        } finally {
            browserCreationSemaphore.release();
        }
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
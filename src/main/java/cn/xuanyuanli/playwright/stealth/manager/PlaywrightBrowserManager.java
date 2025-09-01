package cn.xuanyuanli.playwright.stealth.manager;

import cn.xuanyuanli.playwright.stealth.config.PlaywrightConfig;
import cn.xuanyuanli.playwright.stealth.pool.PlaywrightBrowserFactory;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Playwright浏览器管理器
 * 
 * <p>提供Browser实例的连接池管理，相比{@link PlaywrightManager}，该管理器直接管理Browser实例，
 * 适用于需要长时间保持浏览器会话的场景。主要特性：</p>
 * <ul>
 *   <li>维护Browser实例连接池，避免频繁启动浏览器的开销</li>
 *   <li>支持Browser实例复用和健康检查</li>
 *   <li>集成反检测脚本和配置管理</li>
 *   <li>提供连接池监控和管理功能</li>
 * </ul>
 * 
 * <p><strong>使用场景对比：</strong></p>
 * <ul>
 *   <li>{@link PlaywrightManager}: 适用于短时间、一次性操作</li>
 *   <li>{@link PlaywrightBrowserManager}: 适用于需要频繁操作、保持会话状态的场景</li>
 * </ul>
 * 
 * <p><strong>使用示例：</strong></p>
 * <pre>{@code
 * // 创建配置
 * PlaywrightConfig config = new PlaywrightConfig()
 *     .setHeadless(true)
 *     .setEnableStealthScript(true);
 * 
 * // 创建管理器，设置Browser连接池大小为5
 * PlaywrightBrowserManager manager = new PlaywrightBrowserManager(config, 5);
 * 
 * // 执行页面操作
 * manager.execute(page -> {
 *     page.navigate("https://example.com");
 *     System.out.println(page.title());
 * });
 * 
 * // 并发执行多个任务
 * IntStream.range(0, 10).parallel().forEach(i -> {
 *     manager.execute(page -> {
 *         // 并发页面操作
 *     });
 * });
 * 
 * // 记得关闭管理器
 * manager.close();
 * }</pre>
 *
 * @author xuanyuanli
 */
@Slf4j
public class PlaywrightBrowserManager implements AutoCloseable {

    /**
     * Browser实例连接池
     */
    private final GenericObjectPool<Browser> pool;
    
    /**
     * Playwright配置
     */
    private final PlaywrightConfig playwrightConfig;

    /**
     * 创建Playwright浏览器管理器
     *
     * @param playwrightConfig Playwright配置
     * @param capacity        连接池最大容量
     */
    public PlaywrightBrowserManager(PlaywrightConfig playwrightConfig, int capacity) {
        this(null, playwrightConfig, capacity);
    }

    /**
     * 创建Playwright浏览器管理器
     *
     * @param poolConfig       连接池配置，为null时使用默认配置
     * @param playwrightConfig Playwright配置
     * @param capacity         连接池最大容量
     */
    public PlaywrightBrowserManager(GenericObjectPoolConfig<Browser> poolConfig, 
                                   PlaywrightConfig playwrightConfig, 
                                   int capacity) {
        if (playwrightConfig == null) {
            playwrightConfig = new PlaywrightConfig();
        }
        this.playwrightConfig = playwrightConfig;

        // 设置默认连接池配置
        if (poolConfig == null) {
            poolConfig = new GenericObjectPoolConfig<>();
            // 启用借用时测试，确保Browser实例可用
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(false);
            // 设置连接池大小
            poolConfig.setMaxTotal(capacity);
            poolConfig.setMaxIdle(capacity);
            poolConfig.setMinIdle(1);
            // 设置空闲对象检查和清理
            poolConfig.setTimeBetweenEvictionRuns(Duration.ofMinutes(30));
            poolConfig.setMinEvictableIdleDuration(Duration.ofHours(1));
            poolConfig.setTestWhileIdle(true);
            // 设置等待策略
            poolConfig.setBlockWhenExhausted(true);
            poolConfig.setMaxWait(Duration.ofSeconds(30));
        }

        this.pool = new GenericObjectPool<>(new PlaywrightBrowserFactory(playwrightConfig), poolConfig);
        log.info("PlaywrightBrowserManager initialized with pool capacity: {}, config: {}", 
                capacity, playwrightConfig);
    }

    /**
     * 执行页面操作
     *
     * @param pageConsumer 页面操作函数
     * @throws RuntimeException 当操作执行失败时
     */
    public void execute(Consumer<Page> pageConsumer) {
        execute(null, pageConsumer);
    }

    /**
     * 执行页面操作，支持浏览器上下文自定义
     *
     * @param browserContextConsumer 浏览器上下文配置函数，可为null
     * @param pageConsumer          页面操作函数
     * @throws RuntimeException     当操作执行失败时
     */
    public void execute(Consumer<BrowserContext> browserContextConsumer, Consumer<Page> pageConsumer) {
        Browser browser = null;
        try {
            browser = pool.borrowObject();
            log.debug("Browser borrowed from pool. Pool status: {}", getPoolStatus());
            
            PlaywrightManager.executeWithBrowser(playwrightConfig, browserContextConsumer, pageConsumer, browser);
            
        } catch (Exception e) {
            log.error("Failed to execute browser operation", e);
            throw new RuntimeException("Browser operation failed", e);
        } finally {
            if (browser != null) {
                try {
                    pool.returnObject(browser);
                    log.debug("Browser returned to pool. Pool status: {}", getPoolStatus());
                } catch (Exception e) {
                    log.warn("Failed to return browser object to pool", e);
                }
            }
        }
    }

    /**
     * 获取连接池状态信息
     *
     * @return 连接池状态描述
     */
    public String getPoolStatus() {
        return String.format("Browser Pool Status - Active: %d, Idle: %d, Total: %d/%d, Waited: %d",
                pool.getNumActive(), pool.getNumIdle(), 
                pool.getNumActive() + pool.getNumIdle(), pool.getMaxTotal(),
                pool.getNumWaiters());
    }

    /**
     * 获取详细的连接池统计信息
     *
     * @return 连接池统计信息
     */
    public String getPoolStatistics() {
        return String.format(
                "Browser Pool Statistics - " +
                "Created: %d, Borrowed: %d, Returned: %d, Destroyed: %d, " +
                "Active: %d, Idle: %d, Waiters: %d",
                pool.getCreatedCount(), pool.getBorrowedCount(), pool.getReturnedCount(),
                pool.getDestroyedCount(), pool.getNumActive(), pool.getNumIdle(), pool.getNumWaiters());
    }

    /**
     * 清理空闲连接
     * 
     * <p>手动触发连接池中空闲Browser实例的清理，释放长时间未使用的资源</p>
     */
    public void evictIdleBrowsers() {
        try {
            pool.evict();
            log.info("Evicted idle browsers from pool");
        } catch (Exception e) {
            log.warn("Failed to evict idle browsers", e);
        }
    }

    /**
     * 预热连接池
     * 
     * <p>预先创建指定数量的Browser实例，避免首次使用时的延迟</p>
     *
     * @param count 预热的Browser实例数量
     */
    public void warmUpPool(int count) {
        log.info("Warming up browser pool with {} instances", count);
        for (int i = 0; i < count; i++) {
            try {
                Browser browser = pool.borrowObject();
                pool.returnObject(browser);
            } catch (Exception e) {
                log.warn("Failed to warm up browser pool at index {}", i, e);
                break;
            }
        }
        log.info("Browser pool warm up completed. Current status: {}", getPoolStatus());
    }

    @Override
    public void close() {
        try {
            log.info("Closing PlaywrightBrowserManager. Final statistics: {}", getPoolStatistics());
            pool.close();
            log.info("PlaywrightBrowserManager closed successfully");
        } catch (Exception e) {
            log.error("Error closing PlaywrightBrowserManager", e);
        }
    }
}
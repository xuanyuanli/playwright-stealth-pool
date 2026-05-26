package cn.xuanyuanli.playwright.stealth.pool;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Playwright;
import cn.xuanyuanli.playwright.stealth.config.PlaywrightConfig;
import cn.xuanyuanli.playwright.stealth.config.PoolLifecyclePolicy;
import cn.xuanyuanli.playwright.stealth.manager.PlaywrightManager;
import cn.xuanyuanli.playwright.stealth.manager.PoolLifecycleMetrics;
import cn.xuanyuanli.playwright.stealth.monitor.ChromiumProcessRegistry;
import cn.xuanyuanli.playwright.stealth.monitor.ChromiumResourceMonitor;
import cn.xuanyuanli.playwright.stealth.monitor.ChromiumResourceMonitors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * Playwright浏览器实例工厂类
 * 
 * <p>用于Apache Commons Pool2连接池创建和管理Browser实例。该工厂负责：</p>
 * <ul>
 *   <li>创建配置好的Browser实例</li>
 *   <li>维护Browser与Playwright的关联关系</li>
 *   <li>提供Browser实例的健康检查</li>
 *   <li>正确销毁Browser和关联的Playwright实例</li>
 *   <li>按生命周期策略滚动重建Browser实例</li>
 * </ul>
 * 
 * <p><strong>注意：</strong>该工厂维护了Browser到Playwright的映射关系缓存，
 * 确保在销毁Browser时能够正确关闭对应的Playwright实例，避免资源泄漏。</p>
 *
 * @author xuanyuanli
 */
@Slf4j
public class PlaywrightBrowserFactory extends BasePooledObjectFactory<Browser> {

    /**
     * Playwright配置
     */
    private final PlaywrightConfig config;

    private final PoolLifecyclePolicy lifecyclePolicy;
    private final PooledObjectLifecycleRegistry lifecycleRegistry = new PooledObjectLifecycleRegistry();
    @Getter
    private final PoolLifecycleMetrics lifecycleMetrics = new PoolLifecycleMetrics();
    private final ChromiumResourceMonitor resourceMonitor;
    private final PoolLifecycleEvaluator lifecycleEvaluator;
    
    /**
     * Browser到Playwright实例的映射缓存
     * 
     * <p>用于在销毁Browser时能够找到对应的Playwright实例并正确关闭，
     * 防止资源泄漏。使用ConcurrentHashMap保证线程安全。</p>
     */
    private final ConcurrentMap<Browser, Playwright> playwrightCache = new ConcurrentHashMap<>();

    /**
     * 创建Playwright浏览器工厂
     *
     * @param config Playwright配置，为null时使用默认配置
     */
    public PlaywrightBrowserFactory(PlaywrightConfig config) {
        this(config, PoolLifecyclePolicy.forBrowserPool());
    }

    /**
     * 创建Playwright浏览器工厂
     *
     * @param config           Playwright配置，为null时使用默认配置
     * @param lifecyclePolicy  生命周期策略，为null时使用Browser池默认策略
     */
    public PlaywrightBrowserFactory(PlaywrightConfig config, PoolLifecyclePolicy lifecyclePolicy) {
        this.config = config != null ? config : new PlaywrightConfig();
        this.lifecyclePolicy = lifecyclePolicy != null ? lifecyclePolicy : PoolLifecyclePolicy.forBrowserPool();
        ChromiumResourceMonitors.warnIfResourcePressureUnsupported(this.lifecyclePolicy);
        ChromiumProcessRegistry processRegistry = new ChromiumProcessRegistry();
        this.resourceMonitor = ChromiumResourceMonitors.create(this.lifecyclePolicy, processRegistry);
        this.lifecycleEvaluator = new PoolLifecycleEvaluator(
                this.lifecyclePolicy,
                this.resourceMonitor,
                lifecycleMetrics,
                () -> null);
        log.debug("PlaywrightBrowserFactory created with config: {}", this.config);
    }

    public PoolLifecyclePolicy getLifecyclePolicy() {
        return lifecyclePolicy;
    }

    /**
     * 任务完成后评估是否应淘汰实例。
     */
    public RetireDecision evaluateOnReturn(Browser browser) {
        if (browser == null) {
            return RetireDecision.keep();
        }
        recordTaskCompleted(browser);
        return lifecycleEvaluator.evaluate(lifecycleRegistry.get(browser), browser);
    }

    public void recordRetired(RetireReason reason) {
        lifecycleMetrics.recordRetired(reason);
    }

    public void recordValidationFailed() {
        lifecycleMetrics.recordRetired(RetireReason.VALIDATION_FAILED);
    }

    private void recordTaskCompleted(Browser browser) {
        PooledObjectLifecycle lifecycle = lifecycleRegistry.get(browser);
        if (lifecycle != null) {
            lifecycle.incrementTaskCount();
        }
    }

    /**
     * 创建新的Browser实例
     * 
     * <p>该方法会：</p>
     * <ol>
     *   <li>创建新的Playwright实例</li>
     *   <li>根据配置启动Browser</li>
     *   <li>将Browser和Playwright的映射关系存入缓存</li>
     * </ol>
     *
     * @return 配置好的Browser实例
     * @throws RuntimeException 当Browser创建失败时
     */
    @Override
    public Browser create() {
        try {
            Browser browser = ChromiumProcessRegistry.runWithLaunchIsolation(baseline -> {
                Playwright playwright = Playwright.create();
                Browser launched = PlaywrightManager.createBrowser(config, playwright);
                playwrightCache.put(launched, playwright);
                resourceMonitor.registerBrowser(launched, baseline);
                lifecycleRegistry.register(launched);
                return launched;
            });

            log.debug("Browser created successfully with config: {}", config);

            return browser;

        } catch (Exception e) {
            log.error("Failed to create browser instance", e);
            throw new RuntimeException("Browser creation failed", e);
        }
    }

    /**
     * 将Browser实例包装为连接池对象
     *
     * @param browser 要包装的Browser实例
     * @return 包装后的连接池对象
     */
    @Override
    public PooledObject<Browser> wrap(Browser browser) {
        return new DefaultPooledObject<>(browser);
    }

    @Override
    public void activateObject(PooledObject<Browser> pooledBrowser) {
        // 任务计数在归还时递增，避免借用失败导致计数漂移
    }

    @Override
    public void passivateObject(PooledObject<Browser> pooledBrowser) {
        if (!lifecyclePolicy.isCloseOrphanContextsBeforeDestroy()) {
            return;
        }
        Browser browser = pooledBrowser != null ? pooledBrowser.getObject() : null;
        closeOrphanContexts(browser);
    }

    /**
     * 验证Browser实例是否有效
     * 
     * <p>通过调用browser.contexts()方法来检查Browser是否仍然可用。
     * 如果Browser已经关闭或出现异常，该方法会返回false，
     * 连接池会将该实例标记为无效并创建新的实例。</p>
     *
     * @param pooledBrowser 要验证的连接池对象
     * @return true表示Browser有效，false表示无效
     */
    @Override
    public boolean validateObject(PooledObject<Browser> pooledBrowser) {
        if (pooledBrowser == null) {
            log.warn("PooledBrowser is null, validation failed");
            recordValidationFailed();
            return false;
        }
        
        Browser browser = pooledBrowser.getObject();
        if (browser == null) {
            log.warn("Browser object is null, validation failed");
            recordValidationFailed();
            return false;
        }
        
        try {
            if (!browser.isConnected()) {
                log.warn("Browser is not connected, validation failed");
                recordValidationFailed();
                return false;
            }
            
            browser.contexts();

            RetireDecision decision = lifecycleEvaluator.evaluate(lifecycleRegistry.get(browser), browser);
            if (decision.shouldRetire()) {
                log.info("Browser failed lifecycle validation on borrow: {}", decision.detail());
                recordRetired(decision.reason());
                return false;
            }

            return true;
        } catch (Exception e) {
            log.warn("Browser validation failed, will be destroyed and recreated: {}", e.getMessage());
            recordValidationFailed();
            return false;
        }
    }

    /**
     * 销毁Browser实例及其关联的Playwright实例
     * 
     * <p>该方法会：</p>
     * <ol>
     *   <li>从缓存中获取对应的Playwright实例</li>
     *   <li>关闭Playwright实例（这会自动关闭所有相关Browser）</li>
     *   <li>从缓存中移除映射关系</li>
     *   <li>确保Browser也被正确关闭</li>
     * </ol>
     *
     * @param pooledBrowser 要销毁的连接池对象
     */
    @Override
    public void destroyObject(PooledObject<Browser> pooledBrowser) {
        if (pooledBrowser == null) {
            log.warn("PooledBrowser is null, skipping destroy");
            return;
        }
        
        Browser browser = pooledBrowser.getObject();
        if (browser == null) {
            log.warn("Browser object is null, skipping destroy");
            return;
        }

        if (lifecyclePolicy.isCloseOrphanContextsBeforeDestroy()) {
            closeOrphanContexts(browser);
        }

        lifecycleRegistry.unregister(browser);
        resourceMonitor.unregisterBrowser(browser);
        
        Playwright playwright = null;
        boolean playwrightClosed = false;
        boolean browserClosed = false;
        
        try {
            playwright = playwrightCache.remove(browser);
            
            if (playwright != null) {
                try {
                    playwright.close();
                    playwrightClosed = true;
                    log.debug("Playwright instance closed successfully");
                } catch (Exception e) {
                    log.warn("Error closing Playwright instance: {}", e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.warn("Error during Playwright cleanup: {}", e.getMessage());
        } finally {
            if (!playwrightClosed || browser.isConnected()) {
                try {
                    browser.close();
                    browserClosed = true;
                    log.debug("Browser instance closed successfully");
                } catch (Exception e) {
                    log.warn("Error closing Browser instance: {}", e.getMessage());
                }
            } else {
                browserClosed = true;
                log.debug("Browser was already closed by Playwright");
            }
        }
        
        if (!playwrightClosed && playwright != null) {
            log.warn("Playwright instance may not have been properly closed");
        }
        if (!browserClosed) {
            log.warn("Browser instance may not have been properly closed");
        }
        
        log.debug("Browser destroyed. Playwright closed: {}, Browser closed: {}, Remaining cache size: {}", 
                playwrightClosed, browserClosed, playwrightCache.size());
    }

    private void closeOrphanContexts(Browser browser) {
        if (browser == null) {
            return;
        }
        try {
            for (BrowserContext context : browser.contexts()) {
                closeResourceSafely(context);
            }
        } catch (Exception e) {
            log.debug("Failed to close orphan contexts: {}", e.getMessage());
        }
    }

    private static void closeResourceSafely(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
            log.debug("Successfully closed orphan {}", "browser context");
        } catch (Exception e) {
            log.warn("Failed to close orphan {}: {}", "browser context", e.getMessage());
        }
    }
}

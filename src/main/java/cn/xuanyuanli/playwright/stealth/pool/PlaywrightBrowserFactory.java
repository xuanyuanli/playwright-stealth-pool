package cn.xuanyuanli.playwright.stealth.pool;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Playwright;
import cn.xuanyuanli.playwright.stealth.config.PlaywrightConfig;
import cn.xuanyuanli.playwright.stealth.manager.PlaywrightManager;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import cn.xuanyuanli.core.util.Envs;

/**
 * Playwright浏览器实例工厂类
 * 
 * <p>用于Apache Commons Pool2连接池创建和管理Browser实例。该工厂负责：</p>
 * <ul>
 *   <li>创建配置好的Browser实例</li>
 *   <li>维护Browser与Playwright的关联关系</li>
 *   <li>提供Browser实例的健康检查</li>
 *   <li>正确销毁Browser和关联的Playwright实例</li>
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
    
    /**
     * Browser到Playwright实例的映射缓存
     * 
     * <p>用于在销毁Browser时能够找到对应的Playwright实例并正确关闭，
     * 防止资源泄漏。使用ConcurrentHashMap保证线程安全。</p>
     */
    private static final ConcurrentMap<Browser, Playwright> PLAYWRIGHT_CACHE = new ConcurrentHashMap<>();

    /**
     * 创建Playwright浏览器工厂
     *
     * @param config Playwright配置，为null时使用默认配置
     */
    public PlaywrightBrowserFactory(PlaywrightConfig config) {
        this.config = config != null ? config : new PlaywrightConfig();
        log.debug("PlaywrightBrowserFactory created with config: {}", this.config);
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
            // 创建Playwright实例
            Playwright playwright = Playwright.create();
            
            // 根据配置创建Browser
            Browser browser = PlaywrightManager.createBrowser(config, playwright);
            
            // 缓存映射关系，用于后续销毁时正确关闭Playwright
            PLAYWRIGHT_CACHE.put(browser, playwright);

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
        Browser browser = pooledBrowser.getObject();
        try {
            // 尝试获取浏览器上下文列表来验证Browser是否有效
            browser.contexts();
            return true;
        } catch (Exception e) {
            log.warn("Browser validation failed, will be destroyed and recreated", e);
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
        Browser browser = pooledBrowser.getObject();
        Playwright playwright = null;
        
        try {
            // 获取关联的Playwright实例
            playwright = PLAYWRIGHT_CACHE.get(browser);
            
            if (playwright != null) {
                // 先关闭Playwright（会自动关闭相关Browser）
                playwright.close();
                log.debug("Playwright instance closed successfully");
            }
            
        } catch (Exception e) {
            log.warn("Error closing Playwright instance", e);
        } finally {
            // 清理缓存 - 需要检查null以避免NullPointerException
            if (browser != null) {
                PLAYWRIGHT_CACHE.remove(browser);
            }
            
            // 确保Browser也被关闭
            try {
                if (browser != null && browser.isConnected()) { // 检查连接状态
                    browser.close();
                }
            } catch (Exception e) {
                log.warn("Error closing Browser instance", e);
            }
        }
        
        log.debug("Browser destroyed successfully. Cache size: {}", PLAYWRIGHT_CACHE.size());
    }
}

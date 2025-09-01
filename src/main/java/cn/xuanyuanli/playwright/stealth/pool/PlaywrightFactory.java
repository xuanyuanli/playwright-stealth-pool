package cn.xuanyuanli.playwright.stealth.pool;

import com.microsoft.playwright.Playwright;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * Playwright实例工厂类
 * 
 * <p>用于Apache Commons Pool2连接池创建和管理Playwright实例。该工厂负责：</p>
 * <ul>
 *   <li>创建新的Playwright实例</li>
 *   <li>包装实例为连接池对象</li>
 *   <li>销毁不再使用的实例，释放资源</li>
 * </ul>
 * 
 * <p>Playwright实例是重量级对象，创建成本较高，通过连接池可以有效复用实例，
 * 提高应用性能并减少资源消耗。</p>
 *
 * @author xuanyuanli
 */
public class PlaywrightFactory extends BasePooledObjectFactory<Playwright> {


    /**
     * 创建新的Playwright实例
     * 
     * <p>该方法会创建一个新的Playwright实例，包含所有支持的浏览器引擎
     * （Chromium、Firefox、WebKit）。创建过程可能需要一些时间，
     * 因为需要初始化浏览器引擎和相关组件。</p>
     *
     * @return 新创建的Playwright实例
     * @throws RuntimeException 当Playwright初始化失败时
     */
    @Override
    public Playwright create() {
        return Playwright.create();
    }

    /**
     * 将Playwright实例包装为连接池对象
     * 
     * <p>将原始的Playwright实例包装为Apache Commons Pool2可管理的对象，
     * 添加创建时间、最后使用时间等元数据，用于连接池的生命周期管理。</p>
     *
     * @param playwright 要包装的Playwright实例
     * @return 包装后的连接池对象
     */
    @Override
    public PooledObject<Playwright> wrap(Playwright playwright) {
        return new DefaultPooledObject<>(playwright);
    }

    /**
     * 销毁Playwright实例，释放相关资源
     * 
     * <p>当连接池需要销毁某个实例时（如连接池关闭、空闲对象清理等），
     * 会调用此方法正确关闭Playwright实例，释放浏览器引擎和相关的系统资源。</p>
     *
     * @param pooledPlaywright 要销毁的连接池对象
     */
    @Override
    public void destroyObject(PooledObject<Playwright> pooledPlaywright) {
        try {
            pooledPlaywright.getObject().close();
        } catch (Exception e) {
            // 忽略关闭时的异常，确保销毁流程继续
        }
    }
}

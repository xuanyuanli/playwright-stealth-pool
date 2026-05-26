package cn.xuanyuanli.playwright.stealth.config;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Duration;

/**
 * 连接池生命周期策略配置。
 *
 * <p>控制池化实例何时因借用次数、存活时长或 Chromium 资源压力而滚动重建。</p>
 */
@Data
@Accessors(chain = true)
public class PoolLifecyclePolicy {

    /**
     * 单实例最大任务次数，&lt;=0 表示不限制。Browser 池建议 200~500。
     */
    private int maxBorrowCount = 500;

    /**
     * 单实例最大存活时间，null 表示不限制。建议 30 分钟~1 小时。
     */
    private Duration maxLifetime = Duration.ofHours(1);

    /**
     * 是否启用 Chromium 资源压力检测。
     *
     * <p>Browser 池默认开启；实际 RSS/FD 阈值仅在 Linux（/proc）下生效。</p>
     */
    private boolean resourcePressureEnabled = true;

    /**
     * 关联 Chromium 进程 RSS 总和上限（字节），0 表示不限制。
     *
     * <p>Browser 池默认值为 0（不按 RSS 淘汰），生产若需按内存重建请显式设置。</p>
     */
    private long maxChromiumRssBytes = 0;

    /**
     * 单 Chromium 进程 FD 上限，0 表示不限制。建议 512~1024。
     *
     * <p>Browser 池默认 800，在 Linux 下作为资源压力的主要默认阈值。</p>
     */
    private int maxChromiumFdCount = 800;

    /**
     * 资源扫描最小间隔，避免每次 borrow 都扫描 /proc。
     */
    private Duration resourceCheckInterval = Duration.ofSeconds(30);

    /**
     * 销毁前是否尝试关闭残留的 BrowserContext。
     */
    private boolean closeOrphanContextsBeforeDestroy = true;

    /**
     * 适用于 Playwright 实例池的默认策略（次数 + 时长，不启用资源压力）。
     */
    public static PoolLifecyclePolicy forPlaywrightPool() {
        return new PoolLifecyclePolicy()
                .setMaxBorrowCount(400)
                .setMaxLifetime(Duration.ofHours(1))
                .setResourcePressureEnabled(false);
    }

    /**
     * 适用于 Browser 实例池的默认策略。
     *
     * <p>等价于：次数 500、存活 1 小时、资源压力开启；
     * RSS 不限制（{@code maxChromiumRssBytes=0}），Linux 下按 FD 上限 800 重建。</p>
     */
    public static PoolLifecyclePolicy forBrowserPool() {
        return new PoolLifecyclePolicy()
                .setMaxBorrowCount(500)
                .setMaxLifetime(Duration.ofHours(1))
                .setResourcePressureEnabled(true)
                .setMaxChromiumRssBytes(0)
                .setMaxChromiumFdCount(800);
    }
}

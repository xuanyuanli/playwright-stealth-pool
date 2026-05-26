package cn.xuanyuanli.playwright.stealth.pool;

import cn.xuanyuanli.playwright.stealth.config.PoolLifecyclePolicy;
import cn.xuanyuanli.playwright.stealth.manager.PoolLifecycleMetrics;
import cn.xuanyuanli.playwright.stealth.monitor.ChromiumResourceMonitor;
import cn.xuanyuanli.playwright.stealth.monitor.ChromiumResourceSnapshot;
import com.microsoft.playwright.Browser;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * 评估池化实例是否应被淘汰。
 */
@Slf4j
public class PoolLifecycleEvaluator {

    private final PoolLifecyclePolicy policy;
    private final ChromiumResourceMonitor resourceMonitor;
    private final PoolLifecycleMetrics metrics;
    private final Supplier<Browser> browserSupplier;

    public PoolLifecycleEvaluator(PoolLifecyclePolicy policy,
                                  ChromiumResourceMonitor resourceMonitor,
                                  PoolLifecycleMetrics metrics) {
        this(policy, resourceMonitor, metrics, () -> null);
    }

    public PoolLifecycleEvaluator(PoolLifecyclePolicy policy,
                                  ChromiumResourceMonitor resourceMonitor,
                                  PoolLifecycleMetrics metrics,
                                  Supplier<Browser> browserSupplier) {
        this.policy = policy != null ? policy : PoolLifecyclePolicy.forBrowserPool();
        this.resourceMonitor = resourceMonitor;
        this.metrics = metrics != null ? metrics : new PoolLifecycleMetrics();
        this.browserSupplier = browserSupplier != null ? browserSupplier : () -> null;
    }

    public PoolLifecycleMetrics getMetrics() {
        return metrics;
    }

    public RetireDecision evaluate(PooledObjectLifecycle lifecycle) {
        return evaluate(lifecycle, browserSupplier.get());
    }

    public RetireDecision evaluate(PooledObjectLifecycle lifecycle, Browser browser) {
        if (lifecycle == null) {
            return RetireDecision.keep();
        }

        if (policy.getMaxBorrowCount() > 0
                && lifecycle.getTaskCount() >= policy.getMaxBorrowCount()) {
            return RetireDecision.retire(
                    RetireReason.MAX_BORROW_COUNT,
                    String.format("taskCount=%d >= maxBorrowCount=%d",
                            lifecycle.getTaskCount(), policy.getMaxBorrowCount()));
        }

        Duration maxLifetime = policy.getMaxLifetime();
        if (maxLifetime != null && !maxLifetime.isZero() && !maxLifetime.isNegative()) {
            Duration age = Duration.between(lifecycle.getCreatedAt(), Instant.now());
            if (age.compareTo(maxLifetime) >= 0) {
                return RetireDecision.retire(
                        RetireReason.MAX_LIFETIME,
                        String.format("age=%ds >= maxLifetime=%ds",
                                age.getSeconds(), maxLifetime.getSeconds()));
            }
        }

        if (!policy.isResourcePressureEnabled()) {
            return RetireDecision.keep();
        }

        if (!shouldRunResourceCheck(lifecycle)) {
            return RetireDecision.keep();
        }

        if (browser == null) {
            return RetireDecision.keep();
        }

        lifecycle.markResourceChecked();
        ChromiumResourceSnapshot snapshot = resourceMonitor.snapshot(browser);
        metrics.updateLastResourceSnapshot(snapshot);

        if (policy.getMaxChromiumRssBytes() > 0
                && snapshot.totalRssBytes() > policy.getMaxChromiumRssBytes()) {
            return RetireDecision.retire(
                    RetireReason.RESOURCE_PRESSURE,
                    String.format("totalRssBytes=%d > maxChromiumRssBytes=%d",
                            snapshot.totalRssBytes(), policy.getMaxChromiumRssBytes()));
        }

        if (policy.getMaxChromiumFdCount() > 0
                && snapshot.maxFdCount() > policy.getMaxChromiumFdCount()) {
            return RetireDecision.retire(
                    RetireReason.RESOURCE_PRESSURE,
                    String.format("maxFdCount=%d > maxChromiumFdCount=%d",
                            snapshot.maxFdCount(), policy.getMaxChromiumFdCount()));
        }

        if (snapshot.processCount() > 50) {
            log.warn("Chromium process count is unusually high: {}", snapshot.processCount());
        }

        return RetireDecision.keep();
    }

    private boolean shouldRunResourceCheck(PooledObjectLifecycle lifecycle) {
        Duration interval = policy.getResourceCheckInterval();
        if (interval == null || interval.isZero() || interval.isNegative()) {
            return true;
        }

        Instant lastCheck = lifecycle.getLastResourceCheckAt();
        if (lastCheck == null) {
            return true;
        }

        return Duration.between(lastCheck, Instant.now()).compareTo(interval) >= 0;
    }
}

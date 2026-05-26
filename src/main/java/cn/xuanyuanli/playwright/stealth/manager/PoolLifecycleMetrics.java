package cn.xuanyuanli.playwright.stealth.manager;

import cn.xuanyuanli.playwright.stealth.monitor.ChromiumResourceSnapshot;
import cn.xuanyuanli.playwright.stealth.pool.RetireReason;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * 连接池生命周期指标。
 */
public class PoolLifecycleMetrics {

    private final Map<RetireReason, LongAdder> retiredByReason = new EnumMap<>(RetireReason.class);
    private final AtomicReference<ChromiumResourceSnapshot> lastResourceSnapshot = new AtomicReference<>();

    public PoolLifecycleMetrics() {
        for (RetireReason reason : RetireReason.values()) {
            retiredByReason.put(reason, new LongAdder());
        }
    }

    public void recordRetired(RetireReason reason) {
        if (reason == null) {
            return;
        }
        LongAdder counter = retiredByReason.get(reason);
        if (counter != null) {
            counter.increment();
        }
    }

    public void updateLastResourceSnapshot(ChromiumResourceSnapshot snapshot) {
        lastResourceSnapshot.set(snapshot);
    }

    public long getTotalRetired() {
        return retiredByReason.values().stream().mapToLong(LongAdder::sum).sum();
    }

    /**
     * 各淘汰原因的累计次数快照（线程安全读取）。
     */
    public Map<RetireReason, Long> getRetiredByReason() {
        Map<RetireReason, Long> snapshot = new EnumMap<>(RetireReason.class);
        for (Map.Entry<RetireReason, LongAdder> entry : retiredByReason.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().sum());
        }
        return Collections.unmodifiableMap(snapshot);
    }

    public ChromiumResourceSnapshot getLastResourceSnapshot() {
        return lastResourceSnapshot.get();
    }

    @Override
    public String toString() {
        Map<RetireReason, Long> counts = getRetiredByReason();
        return String.format(
                "Retired[maxBorrow=%d, maxLifetime=%d, resourcePressure=%d, validationFailed=%d, manual=%d], %s",
                counts.getOrDefault(RetireReason.MAX_BORROW_COUNT, 0L),
                counts.getOrDefault(RetireReason.MAX_LIFETIME, 0L),
                counts.getOrDefault(RetireReason.RESOURCE_PRESSURE, 0L),
                counts.getOrDefault(RetireReason.VALIDATION_FAILED, 0L),
                counts.getOrDefault(RetireReason.MANUAL, 0L),
                formatLastSnapshot());
    }

    private String formatLastSnapshot() {
        ChromiumResourceSnapshot snapshot = lastResourceSnapshot.get();
        if (snapshot == null) {
            return "LastResourceSnapshot{none}";
        }
        return String.format(
                "LastResourceSnapshot{rss=%dMB, maxFd=%d, processes=%d}",
                snapshot.totalRssBytes() / 1024 / 1024,
                snapshot.maxFdCount(),
                snapshot.processCount());
    }
}

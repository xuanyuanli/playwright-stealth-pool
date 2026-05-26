package cn.xuanyuanli.playwright.stealth.pool;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 池化实例的生命周期元数据。
 */
public class PooledObjectLifecycle {

    private final Instant createdAt;
    private final AtomicInteger taskCount = new AtomicInteger(0);
    private final AtomicReference<Instant> lastResourceCheckAt = new AtomicReference<>();

    public PooledObjectLifecycle() {
        this.createdAt = Instant.now();
    }

    public PooledObjectLifecycle(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public int getTaskCount() {
        return taskCount.get();
    }

    public int incrementTaskCount() {
        return taskCount.incrementAndGet();
    }

    public Instant getLastResourceCheckAt() {
        return lastResourceCheckAt.get();
    }

    public void markResourceChecked() {
        lastResourceCheckAt.set(Instant.now());
    }
}

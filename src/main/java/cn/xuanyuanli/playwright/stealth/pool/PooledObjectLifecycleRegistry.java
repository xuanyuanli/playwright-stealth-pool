package cn.xuanyuanli.playwright.stealth.pool;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 维护池化对象与其生命周期元数据的映射。
 */
public class PooledObjectLifecycleRegistry {

    private final ConcurrentMap<Object, PooledObjectLifecycle> lifecycles = new ConcurrentHashMap<>();

    public PooledObjectLifecycle register(Object key) {
        return lifecycles.computeIfAbsent(key, ignored -> new PooledObjectLifecycle());
    }

    public PooledObjectLifecycle get(Object key) {
        return lifecycles.get(key);
    }

    public void unregister(Object key) {
        lifecycles.remove(key);
    }

    public int size() {
        return lifecycles.size();
    }
}

package cn.xuanyuanli.playwright.stealth.monitor;

import com.microsoft.playwright.Browser;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 维护 Browser 与 Chromium 进程 PID 的关联关系（按工厂/管理器实例隔离）。
 */
@Slf4j
public class ChromiumProcessRegistry {

    /**
     * 串行化 baseline 捕获与 Chromium launch，避免并发 launch 时 PID 差分归属错误。
     */
    private static final Object LAUNCH_MONITOR = new Object();

    private final ConcurrentMap<Browser, Set<Long>> browserPids = new ConcurrentHashMap<>();

    /**
     * 捕获当前 JVM 子树中的 Chromium 进程快照，用作 launch 前的 baseline。
     */
    public static Set<Long> captureBaseline() {
        return discoverAllChromiumPids();
    }

    /**
     * 在 launch 隔离区内执行：先捕获 baseline，再 launch 并注册 PID 归属。
     *
     * <p>调用方应在 {@code action} 内完成 Browser 创建，并在创建后立即调用
     * {@link #register(Browser, Set)}（或通过 monitor 的 {@code registerBrowser}）。</p>
     */
    public static <T> T runWithLaunchIsolation(LaunchAction<T> action) {
        synchronized (LAUNCH_MONITOR) {
            Set<Long> baseline = captureBaseline();
            return action.execute(baseline);
        }
    }

    @FunctionalInterface
    public interface LaunchAction<T> {
        T execute(Set<Long> baseline);
    }

    public void register(Browser browser) {
        register(browser, captureBaseline());
    }

    public void register(Browser browser, Set<Long> baseline) {
        if (browser == null) {
            return;
        }
        Set<Long> current = discoverAllChromiumPids();
        Set<Long> baselinePids = baseline != null ? baseline : Set.of();
        Set<Long> newPids = current.stream()
                .filter(pid -> !baselinePids.contains(pid))
                .collect(Collectors.toSet());

        browserPids.put(browser, Set.copyOf(newPids));
        if (newPids.isEmpty()) {
            log.debug("No new chromium pids detected for browser; resource attribution deferred");
        } else {
            log.debug("Registered {} chromium pids for browser", newPids.size());
        }
    }

    /**
     * 解析 Browser 关联的 PID（含子进程）。已登记但为空时不再无 baseline 重注册，避免误绑。
     */
    public Set<Long> resolvePids(Browser browser) {
        if (browser == null) {
            return Set.of();
        }
        Set<Long> stored = browserPids.get(browser);
        if (stored == null) {
            return Set.of();
        }
        if (stored.isEmpty()) {
            return Set.of();
        }

        Set<Long> expanded = new HashSet<>(stored);
        for (Long pid : stored) {
            expanded.addAll(collectDescendantPids(pid));
        }
        return expanded;
    }

    public void unregister(Browser browser) {
        if (browser != null) {
            browserPids.remove(browser);
        }
    }

    private static Set<Long> discoverAllChromiumPids() {
        return ProcessHandle.current().descendants()
                .filter(ChromiumProcessRegistry::isChromiumProcess)
                .map(ProcessHandle::pid)
                .collect(Collectors.toSet());
    }

    private static boolean isChromiumProcess(ProcessHandle processHandle) {
        return processHandle.info().commandLine()
                .map(command -> {
                    String lower = command.toLowerCase(Locale.ROOT);
                    return lower.contains("chrome")
                            || lower.contains("chromium")
                            || lower.contains("ms-playwright");
                })
                .orElse(false);
    }

    private static Set<Long> collectDescendantPids(long pid) {
        return ProcessHandle.of(pid)
                .map(handle -> handle.descendants()
                        .map(ProcessHandle::pid)
                        .collect(Collectors.toSet()))
                .orElseGet(Set::of);
    }
}

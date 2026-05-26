package cn.xuanyuanli.playwright.stealth.monitor;

import com.microsoft.playwright.Browser;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

/**
 * 基于 {@link ProcessHandle} 的 Chromium 资源监控降级实现。
 *
 * <p>Windows/macOS 上无法精确统计 FD，RSS/FD 指标为 0，由次数/时长策略兜底。</p>
 */
@Slf4j
public class ProcessHandleChromiumResourceMonitor implements ChromiumResourceMonitor {

    private final ChromiumProcessRegistry processRegistry;

    public ProcessHandleChromiumResourceMonitor(ChromiumProcessRegistry processRegistry) {
        this.processRegistry = processRegistry != null ? processRegistry : new ChromiumProcessRegistry();
    }

    @Override
    public void registerBrowser(Browser browser) {
        processRegistry.register(browser);
    }

    @Override
    public void registerBrowser(Browser browser, Set<Long> baselinePids) {
        processRegistry.register(browser, baselinePids);
    }

    @Override
    public void unregisterBrowser(Browser browser) {
        processRegistry.unregister(browser);
    }

    @Override
    public ChromiumResourceSnapshot snapshot(Browser browser) {
        if (browser == null) {
            return ChromiumResourceSnapshot.empty();
        }

        Set<Long> pids = processRegistry.resolvePids(browser);
        long totalRss = 0;
        Set<Long> alivePids = new HashSet<>();

        for (Long pid : pids) {
            if (pid == null || pid <= 0) {
                continue;
            }
            ProcessHandle handle = ProcessHandle.of(pid).orElse(null);
            if (handle == null || !handle.isAlive()) {
                continue;
            }
            alivePids.add(pid);
            // ProcessHandle 无直接 RSS API；RSS/FD 置 0，由次数/时长策略兜底。
        }

        return new ChromiumResourceSnapshot(totalRss, 0, alivePids.size(), Set.copyOf(alivePids));
    }
}

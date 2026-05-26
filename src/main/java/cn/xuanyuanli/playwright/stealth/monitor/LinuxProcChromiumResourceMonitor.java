package cn.xuanyuanli.playwright.stealth.monitor;

import com.microsoft.playwright.Browser;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * 基于 Linux /proc 的 Chromium 资源监控实现。
 */
@Slf4j
public class LinuxProcChromiumResourceMonitor implements ChromiumResourceMonitor {

    private final ChromiumProcessRegistry processRegistry;

    public LinuxProcChromiumResourceMonitor(ChromiumProcessRegistry processRegistry) {
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
        if (!LinuxProcReader.isLinux()) {
            return new ProcessHandleChromiumResourceMonitor(processRegistry).snapshot(browser);
        }

        Set<Long> pids = processRegistry.resolvePids(browser);
        ChromiumResourceSnapshot snapshot = LinuxProcReader.snapshotForPids(pids);
        log.debug("Linux resource snapshot for browser: rss={} bytes, maxFd={}, processes={}",
                snapshot.totalRssBytes(), snapshot.maxFdCount(), snapshot.processCount());
        return snapshot;
    }
}

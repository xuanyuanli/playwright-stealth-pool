package cn.xuanyuanli.playwright.stealth.monitor;

import java.util.Set;

/**
 * Chromium 进程资源快照。
 */
public record ChromiumResourceSnapshot(
        long totalRssBytes,
        int maxFdCount,
        int processCount,
        Set<Long> pids) {

    public static ChromiumResourceSnapshot empty() {
        return new ChromiumResourceSnapshot(0, 0, 0, Set.of());
    }
}

package cn.xuanyuanli.playwright.stealth.monitor;

import cn.xuanyuanli.playwright.stealth.config.PoolLifecyclePolicy;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * Chromium 资源监控工厂。
 */
@Slf4j
public final class ChromiumResourceMonitors {

    private ChromiumResourceMonitors() {
    }

    public static Set<Long> captureProcessBaseline() {
        return ChromiumProcessRegistry.captureBaseline();
    }

    /**
     * 非 Linux 环境下 RSS/FD 阈值无法通过 /proc 生效时记录告警。
     */
    public static void warnIfResourcePressureUnsupported(PoolLifecyclePolicy policy) {
        if (policy == null || !policy.isResourcePressureEnabled() || LinuxProcReader.isLinux()) {
            return;
        }
        if (policy.getMaxChromiumRssBytes() > 0 || policy.getMaxChromiumFdCount() > 0) {
            log.warn(
                    "Chromium RSS/FD thresholds are only enforced on Linux (/proc). "
                            + "On {} they are ignored; maxBorrowCount/maxLifetime still apply.",
                    System.getProperty("os.name", "unknown"));
        }
    }

    public static ChromiumResourceMonitor create(PoolLifecyclePolicy policy) {
        return create(policy, new ChromiumProcessRegistry());
    }

    public static ChromiumResourceMonitor create(PoolLifecyclePolicy policy,
                                                   ChromiumProcessRegistry processRegistry) {
        if (policy == null || !policy.isResourcePressureEnabled()) {
            return new NoOpChromiumResourceMonitor();
        }
        ChromiumProcessRegistry registry = processRegistry != null ? processRegistry : new ChromiumProcessRegistry();
        if (LinuxProcReader.isLinux()) {
            return new LinuxProcChromiumResourceMonitor(registry);
        }
        return new ProcessHandleChromiumResourceMonitor(registry);
    }
}

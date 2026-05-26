package cn.xuanyuanli.playwright.stealth.monitor;

import com.microsoft.playwright.Browser;

import java.util.Set;

/**
 * 采集 Browser 关联 Chromium 进程的资源使用情况。
 */
public interface ChromiumResourceMonitor {

    ChromiumResourceSnapshot snapshot(Browser browser);

    /**
     * Browser 创建后登记进程关联关系。
     */
    void registerBrowser(Browser browser);

    /**
     * Browser 创建后登记进程关联关系（带 launch 前 baseline）。
     */
    default void registerBrowser(Browser browser, Set<Long> baselinePids) {
        registerBrowser(browser);
    }

    /**
     * Browser 销毁时清理登记信息。
     */
    void unregisterBrowser(Browser browser);
}

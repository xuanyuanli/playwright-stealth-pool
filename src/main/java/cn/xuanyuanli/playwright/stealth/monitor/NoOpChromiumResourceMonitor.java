package cn.xuanyuanli.playwright.stealth.monitor;

import com.microsoft.playwright.Browser;
import lombok.extern.slf4j.Slf4j;

/**
 * 不采集资源指标的空实现。
 */
@Slf4j
public class NoOpChromiumResourceMonitor implements ChromiumResourceMonitor {

    @Override
    public ChromiumResourceSnapshot snapshot(Browser browser) {
        return ChromiumResourceSnapshot.empty();
    }

    @Override
    public void registerBrowser(Browser browser) {
        log.debug("No-op registerBrowser for {}", browser);
    }

    @Override
    public void unregisterBrowser(Browser browser) {
        log.debug("No-op unregisterBrowser for {}", browser);
    }
}

package cn.xuanyuanli.playwright.stealth.monitor;

import com.microsoft.playwright.Browser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChromiumProcessRegistry 测试")
class ChromiumProcessRegistryTest {

    @Mock
    private Browser browserA;

    @Mock
    private Browser browserB;

    @Test
    @DisplayName("PID 差分为空时不应回退为全部 Chromium 进程")
    void shouldNotAssignAllChromiumPidsWhenDiffIsEmpty() {
        ChromiumProcessRegistry registry = new ChromiumProcessRegistry();
        Set<Long> baseline = ChromiumProcessRegistry.captureBaseline();
        registry.register(browserA, baseline);
        registry.register(browserB, baseline);

        assertThat(registry.resolvePids(browserA)).isEmpty();
        assertThat(registry.resolvePids(browserB)).isEmpty();

        registry.unregister(browserA);
        registry.unregister(browserB);
    }

    @Test
    @DisplayName("未登记的 Browser 解析 PID 应返回空集")
    void shouldReturnEmptyWhenBrowserNotRegistered() {
        ChromiumProcessRegistry registry = new ChromiumProcessRegistry();
        assertThat(registry.resolvePids(browserA)).isEmpty();
    }

    @Test
    @DisplayName("已登记但为空 PID 时不应无 baseline 重注册")
    void shouldNotReRegisterWhenStoredPidsEmpty() {
        ChromiumProcessRegistry registry = new ChromiumProcessRegistry();
        Set<Long> baseline = ChromiumProcessRegistry.captureBaseline();
        registry.register(browserA, baseline);

        assertThat(registry.resolvePids(browserA)).isEmpty();
        assertThat(registry.resolvePids(browserA)).isEmpty();

        registry.unregister(browserA);
    }

    @Test
    @DisplayName("runWithLaunchIsolation 应串行执行并传入 baseline")
    void shouldRunLaunchIsolationSerially() throws InterruptedException {
        AtomicInteger concurrentRuns = new AtomicInteger(0);
        AtomicInteger maxConcurrentRuns = new AtomicInteger(0);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);

        Runnable task = () -> {
            try {
                startGate.await(5, TimeUnit.SECONDS);
                ChromiumProcessRegistry.runWithLaunchIsolation(baseline -> {
                    int current = concurrentRuns.incrementAndGet();
                    maxConcurrentRuns.updateAndGet(max -> Math.max(max, current));
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        concurrentRuns.decrementAndGet();
                    }
                    assertThat(baseline).isNotNull();
                    return baseline;
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        };

        Thread t1 = Thread.ofVirtual().start(task);
        Thread t2 = Thread.ofVirtual().start(task);
        startGate.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        t1.join(5000);
        t2.join(5000);

        assertThat(maxConcurrentRuns.get()).isEqualTo(1);
    }
}

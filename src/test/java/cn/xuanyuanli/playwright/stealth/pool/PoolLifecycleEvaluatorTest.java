package cn.xuanyuanli.playwright.stealth.pool;

import cn.xuanyuanli.playwright.stealth.config.PoolLifecyclePolicy;
import cn.xuanyuanli.playwright.stealth.manager.PoolLifecycleMetrics;
import cn.xuanyuanli.playwright.stealth.monitor.ChromiumResourceSnapshot;
import cn.xuanyuanli.playwright.stealth.monitor.NoOpChromiumResourceMonitor;
import com.microsoft.playwright.Browser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("PoolLifecycleEvaluator 测试")
class PoolLifecycleEvaluatorTest {

    @Mock
    private Browser browser;

    @Test
    @DisplayName("任务次数达到上限时应淘汰")
    void shouldRetireWhenMaxBorrowCountReached() {
        PoolLifecyclePolicy policy = new PoolLifecyclePolicy().setMaxBorrowCount(3);
        PoolLifecycleEvaluator evaluator = new PoolLifecycleEvaluator(
                policy, new NoOpChromiumResourceMonitor(), new PoolLifecycleMetrics());

        PooledObjectLifecycle lifecycle = new PooledObjectLifecycle();
        lifecycle.incrementTaskCount();
        lifecycle.incrementTaskCount();
        lifecycle.incrementTaskCount();

        RetireDecision decision = evaluator.evaluate(lifecycle);
        assertThat(decision.shouldRetire()).isTrue();
        assertThat(decision.reason()).isEqualTo(RetireReason.MAX_BORROW_COUNT);
    }

    @Test
    @DisplayName("存活时长达到上限时应淘汰")
    void shouldRetireWhenMaxLifetimeReached() {
        PoolLifecyclePolicy policy = new PoolLifecyclePolicy()
                .setMaxBorrowCount(0)
                .setMaxLifetime(Duration.ofMinutes(30));
        PoolLifecycleEvaluator evaluator = new PoolLifecycleEvaluator(
                policy, new NoOpChromiumResourceMonitor(), new PoolLifecycleMetrics());

        PooledObjectLifecycle lifecycle = new PooledObjectLifecycle(
                Instant.now().minus(Duration.ofMinutes(31)));

        RetireDecision decision = evaluator.evaluate(lifecycle);
        assertThat(decision.shouldRetire()).isTrue();
        assertThat(decision.reason()).isEqualTo(RetireReason.MAX_LIFETIME);
    }

    @Test
    @DisplayName("未达阈值时应保留")
    void shouldKeepWhenWithinLimits() {
        PoolLifecyclePolicy policy = new PoolLifecyclePolicy()
                .setMaxBorrowCount(10)
                .setMaxLifetime(Duration.ofHours(1));
        PoolLifecycleEvaluator evaluator = new PoolLifecycleEvaluator(
                policy, new NoOpChromiumResourceMonitor(), new PoolLifecycleMetrics());

        RetireDecision decision = evaluator.evaluate(new PooledObjectLifecycle());
        assertThat(decision.shouldRetire()).isFalse();
    }

    @Test
    @DisplayName("RSS 超阈值时应淘汰")
    void shouldRetireWhenRssExceeded() {
        PoolLifecyclePolicy policy = new PoolLifecyclePolicy()
                .setMaxBorrowCount(0)
                .setMaxLifetime(null)
                .setResourcePressureEnabled(true)
                .setMaxChromiumRssBytes(1024)
                .setResourceCheckInterval(Duration.ZERO);

        PoolLifecycleEvaluator evaluator = new PoolLifecycleEvaluator(
                policy,
                new NoOpChromiumResourceMonitor() {
                    @Override
                    public ChromiumResourceSnapshot snapshot(Browser browser) {
                        return new ChromiumResourceSnapshot(2048, 0, 1, Set.of(1L));
                    }
                },
                new PoolLifecycleMetrics());

        RetireDecision decision = evaluator.evaluate(new PooledObjectLifecycle(), browser);
        assertThat(decision.shouldRetire()).isTrue();
        assertThat(decision.reason()).isEqualTo(RetireReason.RESOURCE_PRESSURE);
    }

    @Test
    @DisplayName("FD 超阈值时应淘汰")
    void shouldRetireWhenFdExceeded() {
        PoolLifecyclePolicy policy = new PoolLifecyclePolicy()
                .setMaxBorrowCount(0)
                .setMaxLifetime(null)
                .setResourcePressureEnabled(true)
                .setMaxChromiumFdCount(100)
                .setResourceCheckInterval(Duration.ZERO);

        PoolLifecycleEvaluator evaluator = new PoolLifecycleEvaluator(
                policy,
                new NoOpChromiumResourceMonitor() {
                    @Override
                    public ChromiumResourceSnapshot snapshot(Browser browser) {
                        return new ChromiumResourceSnapshot(0, 150, 1, Set.of(1L));
                    }
                },
                new PoolLifecycleMetrics());

        RetireDecision decision = evaluator.evaluate(new PooledObjectLifecycle(), browser);
        assertThat(decision.shouldRetire()).isTrue();
        assertThat(decision.reason()).isEqualTo(RetireReason.RESOURCE_PRESSURE);
    }
}

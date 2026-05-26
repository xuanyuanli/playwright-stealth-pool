package cn.xuanyuanli.playwright.stealth.monitor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LinuxProcReader 测试")
class LinuxProcChromiumResourceMonitorTest {

    @Test
    @DisplayName("空 PID 集合应返回空快照")
    void shouldReturnEmptySnapshotForEmptyPids() {
        ChromiumResourceSnapshot snapshot = LinuxProcReader.snapshotForPids(Set.of());
        assertThat(snapshot.totalRssBytes()).isZero();
        assertThat(snapshot.maxFdCount()).isZero();
        assertThat(snapshot.processCount()).isZero();
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    @DisplayName("Linux 上应能读取当前 JVM 进程 RSS")
    void shouldReadCurrentProcessRssOnLinux() {
        long pid = ProcessHandle.current().pid();
        long rss = LinuxProcReader.readRssBytes(pid);
        assertThat(rss).isGreaterThan(0);
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    @DisplayName("Linux 上应能读取当前 JVM 进程 FD 数量")
    void shouldReadCurrentProcessFdCountOnLinux() {
        long pid = ProcessHandle.current().pid();
        Path fdPath = Path.of("/proc", String.valueOf(pid), "fd");
        if (!Files.isDirectory(fdPath)) {
            return;
        }
        int fdCount = LinuxProcReader.readFdCount(pid);
        assertThat(fdCount).isGreaterThan(0);
    }
}

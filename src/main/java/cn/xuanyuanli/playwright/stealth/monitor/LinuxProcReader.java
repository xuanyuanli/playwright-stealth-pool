package cn.xuanyuanli.playwright.stealth.monitor;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Linux /proc 读取工具。
 */
@Slf4j
final class LinuxProcReader {

    private LinuxProcReader() {
    }

    static boolean isLinux() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("linux");
    }

    static long readRssBytes(long pid) {
        Path statusPath = Path.of("/proc", String.valueOf(pid), "status");
        if (!Files.isRegularFile(statusPath)) {
            return 0;
        }

        try (Stream<String> lines = Files.lines(statusPath)) {
            Optional<String> vmRssLine = lines
                    .filter(line -> line.startsWith("VmRSS:"))
                    .findFirst();
            if (vmRssLine.isEmpty()) {
                return 0;
            }
            String[] parts = vmRssLine.get().trim().split("\\s+");
            if (parts.length < 2) {
                return 0;
            }
            long rssKb = Long.parseLong(parts[1]);
            return rssKb * 1024;
        } catch (IOException | NumberFormatException e) {
            log.debug("Failed to read RSS for pid {}: {}", pid, e.getMessage());
            return 0;
        }
    }

    static int readFdCount(long pid) {
        Path fdPath = Path.of("/proc", String.valueOf(pid), "fd");
        if (!Files.isDirectory(fdPath)) {
            return 0;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(fdPath)) {
            int count = 0;
            for (Path ignored : stream) {
                count++;
            }
            return count;
        } catch (IOException e) {
            log.debug("Failed to read FD count for pid {}: {}", pid, e.getMessage());
            return 0;
        }
    }

    static ChromiumResourceSnapshot snapshotForPids(Set<Long> pids) {
        if (pids == null || pids.isEmpty()) {
            return ChromiumResourceSnapshot.empty();
        }

        long totalRss = 0;
        int maxFd = 0;
        Set<Long> alivePids = new HashSet<>();

        for (Long pid : pids) {
            if (pid == null || pid <= 0) {
                continue;
            }
            if (!Files.isDirectory(Path.of("/proc", String.valueOf(pid)))) {
                continue;
            }
            alivePids.add(pid);
            totalRss += readRssBytes(pid);
            maxFd = Math.max(maxFd, readFdCount(pid));
        }

        return new ChromiumResourceSnapshot(totalRss, maxFd, alivePids.size(), Set.copyOf(alivePids));
    }
}

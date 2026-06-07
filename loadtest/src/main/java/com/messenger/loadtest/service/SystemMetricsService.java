package com.messenger.loadtest.service;

import com.messenger.loadtest.dto.SystemMetricsHistoryEntry;
import com.messenger.loadtest.dto.SystemMetricsResponse;
import com.sun.management.OperatingSystemMXBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class SystemMetricsService {
    private static final long MB = 1024L * 1024L;
    private static final int MAX_HISTORY_SIZE = 7200;

    private final List<SystemMetricsHistoryEntry> history = new ArrayList<>();
    private long lastDiskBytes = -1;
    private long lastSampleNanos;

    public SystemMetricsResponse collect() {
        java.lang.management.OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        if (!(os instanceof OperatingSystemMXBean sunOs)) {
            return new SystemMetricsResponse(0, new SystemMetricsResponse.Ram(0, 0), new SystemMetricsResponse.Disk(0));
        }

        double cpu = sunOs.getCpuLoad();
        if (cpu < 0) {
            cpu = 0;
        } else {
            cpu *= 100.0;
        }

        long ramMaxMb = sunOs.getTotalMemorySize() / MB;
        long ramCurrentMb = (sunOs.getTotalMemorySize() - sunOs.getFreeMemorySize()) / MB;
        double diskSpeedMbPerSec = calculateDiskSpeedMbPerSec();

        double roundedCpu = round(cpu);
        double roundedDiskSpeed = round(diskSpeedMbPerSec);
        recordHistory(roundedCpu, ramMaxMb, ramCurrentMb, roundedDiskSpeed);

        return new SystemMetricsResponse(
                roundedCpu,
                new SystemMetricsResponse.Ram(ramMaxMb, ramCurrentMb),
                new SystemMetricsResponse.Disk(roundedDiskSpeed)
        );
    }

    public List<SystemMetricsHistoryEntry> getHistory() {
        synchronized (history) {
            return List.copyOf(history);
        }
    }

    private void recordHistory(double cpu, long ramMaxMb, long ramCurrentMb, double diskSpeedMbPerSec) {
        synchronized (history) {
            history.add(new SystemMetricsHistoryEntry(
                    System.currentTimeMillis(),
                    cpu,
                    ramMaxMb,
                    ramCurrentMb,
                    diskSpeedMbPerSec
            ));
            if (history.size() > MAX_HISTORY_SIZE) {
                history.remove(0);
            }
        }
    }

    private synchronized double calculateDiskSpeedMbPerSec() {
        long diskBytes = readProcessIoBytes();
        long now = System.nanoTime();
        double speed = 0;

        if (diskBytes >= 0 && lastDiskBytes >= 0) {
            double seconds = (now - lastSampleNanos) / 1_000_000_000.0;
            if (seconds > 0) {
                speed = (diskBytes - lastDiskBytes) / (double) MB / seconds;
            }
        }

        if (diskBytes >= 0) {
            lastDiskBytes = diskBytes;
            lastSampleNanos = now;
        }

        return Math.max(speed, 0);
    }

    private static long readProcessIoBytes() {
        Path ioStats = Path.of("/proc/self/io");
        if (!Files.isReadable(ioStats)) {
            return -1;
        }

        long readBytes = 0;
        long writeBytes = 0;
        try {
            for (String line : Files.readAllLines(ioStats)) {
                if (line.startsWith("read_bytes:")) {
                    readBytes = Long.parseLong(line.split("\\s+")[1]);
                } else if (line.startsWith("write_bytes:")) {
                    writeBytes = Long.parseLong(line.split("\\s+")[1]);
                }
            }
            return readBytes + writeBytes;
        } catch (IOException | NumberFormatException e) {
            return -1;
        }
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

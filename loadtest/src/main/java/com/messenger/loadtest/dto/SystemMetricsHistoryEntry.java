package com.messenger.loadtest.dto;

public class SystemMetricsHistoryEntry {
    private final long timestamp;
    private final double cpu;
    private final long ramMax;
    private final long ramCurrent;
    private final double diskSpeed;

    public SystemMetricsHistoryEntry(long timestamp, double cpu, long ramMax, long ramCurrent, double diskSpeed) {
        this.timestamp = timestamp;
        this.cpu = cpu;
        this.ramMax = ramMax;
        this.ramCurrent = ramCurrent;
        this.diskSpeed = diskSpeed;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getCpu() {
        return cpu;
    }

    public long getRamMax() {
        return ramMax;
    }

    public long getRamCurrent() {
        return ramCurrent;
    }

    public double getDiskSpeed() {
        return diskSpeed;
    }
}

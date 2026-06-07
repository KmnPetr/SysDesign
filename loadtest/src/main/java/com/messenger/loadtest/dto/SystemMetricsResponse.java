package com.messenger.loadtest.dto;

public class SystemMetricsResponse {
    private double cpu;
    private Ram ram;
    private Disk disk;

    public SystemMetricsResponse(double cpu, Ram ram, Disk disk) {
        this.cpu = cpu;
        this.ram = ram;
        this.disk = disk;
    }

    public double getCpu() {
        return cpu;
    }

    public Ram getRam() {
        return ram;
    }

    public Disk getDisk() {
        return disk;
    }

    public static class Ram {
        private final long max;
        private final long current;

        public Ram(long max, long current) {
            this.max = max;
            this.current = current;
        }

        public long getMax() {
            return max;
        }

        public long getCurrent() {
            return current;
        }
    }

    public static class Disk {
        private final double speed;

        public Disk(double speed) {
            this.speed = speed;
        }

        public double getSpeed() {
            return speed;
        }
    }
}

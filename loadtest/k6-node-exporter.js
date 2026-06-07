const MB = 1024 * 1024;

function extractLabel(line, key) {
    const match = line.match(new RegExp(`${key}="([^"]*)"`));
    return match ? match[1] : null;
}

function shouldIncludeDevice(device) {
    if (!device) {
        return false;
    }
    return !/^(loop|ram|dm-|sr\d|fd\d|zram|nbd)/.test(device);
}

function round(value) {
    return Math.round(value * 100) / 100;
}

function readCpuCounters(lines) {
    let idle = 0;
    let total = 0;

    for (const line of lines) {
        if (!line.startsWith('node_cpu_seconds_total')) {
            continue;
        }
        const mode = extractLabel(line, 'mode');
        const value = parseFloat(line.trim().split(/\s+/).pop());
        total += value;
        if (mode === 'idle') {
            idle += value;
        }
    }

    return { idle, total };
}

function readMemoryBytes(lines) {
    let memTotal = null;
    let memAvailable = null;

    for (const line of lines) {
        if (line.startsWith('node_memory_MemTotal_bytes ')) {
            memTotal = parseFloat(line.trim().split(/\s+/).pop());
        } else if (line.startsWith('node_memory_MemAvailable_bytes ')) {
            memAvailable = parseFloat(line.trim().split(/\s+/).pop());
        }
    }

    return { memTotal, memAvailable };
}

function readDiskBytes(lines) {
    let bytes = 0;

    for (const line of lines) {
        if (!line.startsWith('node_disk_read_bytes_total') && !line.startsWith('node_disk_written_bytes_total')) {
            continue;
        }
        const device = extractLabel(line, 'device');
        if (!shouldIncludeDevice(device)) {
            continue;
        }
        bytes += parseFloat(line.trim().split(/\s+/).pop());
    }

    return bytes;
}

export function buildMetricsSampleFromBody(body, timestamp, previous) {
    const lines = body.split('\n');
    const memory = readMemoryBytes(lines);

    if (memory.memTotal == null || memory.memAvailable == null) {
        return null;
    }

    const cpu = readCpuCounters(lines);
    const disk = readDiskBytes(lines);

    let cpuPercent = 0;
    let diskSpeedMbPerSec = 0;

    if (previous) {
        const cpuDelta = cpu.total - previous.cpu.total;
        const idleDelta = cpu.idle - previous.cpu.idle;
        if (cpuDelta > 0) {
            cpuPercent = (1 - idleDelta / cpuDelta) * 100;
        }

        const diskDelta = disk - previous.disk;
        const seconds = (timestamp - previous.timestamp) / 1000;
        if (seconds > 0 && diskDelta >= 0) {
            diskSpeedMbPerSec = diskDelta / MB / seconds;
        }
    }

    const ramMax = Math.round(memory.memTotal / MB);
    const ram = Math.round((memory.memTotal - memory.memAvailable) / MB);

    return {
        sample: {
            timestamp,
            cpu: round(Math.max(cpuPercent, 0)),
            ram,
            ramMax,
            disk: round(Math.max(diskSpeedMbPerSec, 0)),
        },
        state: {
            timestamp,
            cpu,
            disk,
        },
    };
}

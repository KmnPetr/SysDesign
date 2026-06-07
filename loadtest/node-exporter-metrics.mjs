import fs from 'node:fs';

const MB = 1024 * 1024;

const exporterUrl = process.env.NODE_EXPORTER_URL || 'http://localhost:9100';
const historyPath = process.env.K6_METRICS_HISTORY;
const durationMs = parseDuration(process.env.TEST_DURATION || '30s');

if (!historyPath) {
    console.error('K6_METRICS_HISTORY is not set');
    process.exit(1);
}

const history = [];
let lastState = null;

function parseDuration(value) {
    const match = /^(\d+)(ms|s|m|h)$/.exec(value || '30s');
    if (!match) {
        return 30000;
    }

    const amount = Number(match[1]);
    switch (match[2]) {
        case 'ms':
            return amount;
        case 's':
            return amount * 1000;
        case 'm':
            return amount * 60000;
        case 'h':
            return amount * 3600000;
        default:
            return 30000;
    }
}

function sleep(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
}

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

function buildSample(body, timestamp) {
    const lines = body.split('\n');
    const memory = readMemoryBytes(lines);

    if (memory.memTotal == null || memory.memAvailable == null) {
        return null;
    }

    const cpu = readCpuCounters(lines);
    const disk = readDiskBytes(lines);

    let cpuPercent = 0;
    let diskSpeedMbPerSec = 0;

    if (lastState) {
        const cpuDelta = cpu.total - lastState.cpu.total;
        const idleDelta = cpu.idle - lastState.cpu.idle;
        if (cpuDelta > 0) {
            cpuPercent = (1 - idleDelta / cpuDelta) * 100;
        }

        const diskDelta = disk - lastState.disk;
        const seconds = (timestamp - lastState.timestamp) / 1000;
        if (seconds > 0 && diskDelta >= 0) {
            diskSpeedMbPerSec = diskDelta / MB / seconds;
        }
    }

    lastState = { timestamp, cpu, disk };

    return {
        timestamp,
        cpu: round(Math.max(cpuPercent, 0)),
        ram: Math.round((memory.memTotal - memory.memAvailable) / MB),
        ramMax: Math.round(memory.memTotal / MB),
        disk: round(Math.max(diskSpeedMbPerSec, 0)),
    };
}

async function collectSample() {
    const response = await fetch(`${exporterUrl}/metrics`);
    if (!response.ok) {
        return;
    }

    const sample = buildSample(await response.text(), Date.now());
    if (sample) {
        history.push(sample);
    }
}

const endAt = Date.now() + durationMs;

while (Date.now() < endAt) {
    const started = Date.now();

    try {
        await collectSample();
    } catch (error) {
        // skip failed sample
    }

    const remaining = endAt - Date.now();
    if (remaining <= 0) {
        break;
    }

    const elapsed = Date.now() - started;
    await sleep(Math.min(Math.max(0, 1000 - elapsed), remaining));
}

fs.writeFileSync(historyPath, JSON.stringify(history, null, 2));
console.log(`Metrics history saved: ${historyPath} (${history.length} samples)`);

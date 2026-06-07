import fs from 'node:fs';
import { buildMetricsSampleFromBody } from './k6-node-exporter.js';

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

const exporterUrl = process.env.NODE_EXPORTER_URL || 'http://localhost:9100';
const historyPath = process.env.K6_METRICS_HISTORY;
const durationMs = parseDuration(process.env.TEST_DURATION || '30s');

if (!historyPath) {
    console.error('K6_METRICS_HISTORY is not set');
    process.exit(1);
}

const history = [];
let lastState = null;
const endAt = Date.now() + durationMs;

while (Date.now() < endAt) {
    const started = Date.now();

    try {
        const response = await fetch(`${exporterUrl}/metrics`);
        if (response.ok) {
            const body = await response.text();
            const parsed = buildMetricsSampleFromBody(body, Date.now(), lastState);
            if (parsed) {
                lastState = parsed.state;
                history.push(parsed.sample);
            }
        }
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

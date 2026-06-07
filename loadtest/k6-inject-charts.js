import { injectIntoHtml } from './k6-metrics-chart.js';

const reportPath = __ENV.K6_WEB_DASHBOARD_EXPORT;
const historyPath = __ENV.K6_METRICS_HISTORY;

let baseHtml = null;
let historySamples = null;

if (reportPath) {
    try {
        baseHtml = open(reportPath);
    } catch (e) {
        console.log(`Не удалось прочитать отчёт: ${reportPath}`);
    }
} else {
    console.log('K6_WEB_DASHBOARD_EXPORT не задан');
}

if (historyPath) {
    try {
        historySamples = JSON.parse(open(historyPath));
    } catch (e) {
        console.log(`Не удалось прочитать историю метрик: ${historyPath}`);
    }
} else {
    console.log('K6_METRICS_HISTORY не задан');
}

export const options = {
    iterations: 1,
    vus: 1,
};

export default function () {}

export function handleSummary() {
    if (!reportPath || !baseHtml) {
        return {};
    }
    if (!historySamples || historySamples.length === 0) {
        console.log('Нет данных истории метрик для внедрения');
        return {};
    }

    const merged = injectIntoHtml(baseHtml, historySamples);
    console.log(`\n📈 Графики сервера внедрены в конец: ${reportPath}`);

    return {
        [reportPath]: merged,
    };
}

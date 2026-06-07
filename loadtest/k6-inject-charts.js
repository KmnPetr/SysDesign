import http from 'k6/http';
import { injectIntoHtml } from './k6-metrics-chart.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:4200';

export const options = {
    iterations: 1,
    vus: 1,
};

export default function () {}

export function handleSummary() {
    const reportPath = __ENV.K6_WEB_DASHBOARD_EXPORT;
    if (!reportPath) {
        console.log('K6_WEB_DASHBOARD_EXPORT не задан');
        return {};
    }

    let baseHtml;
    try {
        baseHtml = open(reportPath);
    } catch (e) {
        console.log(`Не удалось прочитать отчёт: ${reportPath}`);
        return {};
    }

    const historyResponse = http.get(`${BASE_URL}/api/info/metrics/history`);
    if (historyResponse.status !== 200) {
        console.log('Не удалось получить /api/info/metrics/history');
        return {};
    }

    const samples = JSON.parse(historyResponse.body);
    const merged = injectIntoHtml(baseHtml, samples);
    console.log(`\n📈 Графики сервера внедрены в конец: ${reportPath}`);

    return {
        [reportPath]: merged,
    };
}

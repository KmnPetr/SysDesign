//команда на запуск
//k6 version
//$env:K6_WEB_DASHBOARD="true"; k6 run k6-createrandom-test.js     //для PowerShell

import http from 'k6/http';
import { check, group } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const createRandomReqs = new Counter('endpoint_create_random_reqs');
const createRandomDuration = new Trend('endpoint_create_random_duration', true);

// ramping-arrival-rate считает ИТЕРАЦИИ/с (1 итерация = 1 POST /api/messages/createrandom).
export const options = {
    scenarios: {
        load_test: {
            executor: 'ramping-arrival-rate',
            startRate: 0,
            timeUnit: '1s',
            preAllocatedVUs: 100,
            maxVUs: 5000,
            stages: [
                { duration: '30m', target: 5000 },
            ],
            gracefulStop: '1m',
        },
    },

    thresholds: {
        'http_req_duration': ['p(95)<1000'],
        'http_req_failed': ['rate<0.05'],
        'http_req_duration{name:create_random}': ['p(95)<1000'],
        'endpoint_create_random_duration': ['p(95)<1000'],
    },
};

const BASE_URL = 'http://localhost:4200';

const requestParams = {
    headers: {
        'Content-Type': 'application/json',
    },
    timeout: '10s',
};

export default function () {
    group('create_random', function () {
        const response = http.post(`${BASE_URL}/api/messages/createrandom`, null, {
            ...requestParams,
            tags: { name: 'create_random' },
        });
        createRandomReqs.add(1);
        createRandomDuration.add(response.timings.duration);

        check(response, {
            'create_random: статус 201': (r) => r.status === 201,
        });
    });
}

export function handleSummary(data) {
    const totalReqs = data.metrics.http_reqs.values.count;
    const iterations = data.metrics.iterations?.values.count ?? 0;

    console.log('\n📊 ИТОГИ ТЕСТИРОВАНИЯ (createrandom):');
    console.log(`✅ Всего HTTP-запросов: ${totalReqs}`);
    console.log(`🔁 Итераций: ${iterations}`);
    console.log(`⏱️  Среднее время ответа: ${data.metrics.http_req_duration.values.avg.toFixed(2)} мс`);
    console.log(`🐌 p95 время ответа: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)} мс`);
    console.log(`❌ Процент ошибок: ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%`);

    const reqs = data.metrics.endpoint_create_random_reqs?.values;
    const duration = data.metrics.endpoint_create_random_duration?.values;
    if (reqs && duration) {
        console.log('\n📍 create_random:');
        console.log(`   запросов: ${reqs.count}`);
        console.log(`   avg: ${duration.avg.toFixed(2)} мс, p95: ${duration['p(95)'].toFixed(2)} мс`);
    }

    return {
        'summary-createrandom.json': JSON.stringify(data, null, 2),
    };
}

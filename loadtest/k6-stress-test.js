//команда на запуск (дашборд + HTML-отчёт с графиками в report/)
//.\k6-run.ps1 k6-stress-test.js

import http from 'k6/http';
import { check, group } from 'k6';
import { Trend } from 'k6/metrics';

const serverCpu = new Trend('server_cpu_percent');
const serverRamCurrent = new Trend('server_ram_current_mb');
const serverRamMax = new Trend('server_ram_max_mb');
const serverDiskSpeed = new Trend('server_disk_speed_mbps');

const MESSAGES_TO_CREATE = 5;
const TEST_DURATION = '30s';

// Настройки теста
// ramping-arrival-rate считает ИТЕРАЦИИ/с (1 итерация = 7 HTTP-запросов: 2 GET + 5 POST).
// На дашборде смотрите http_reqs — там каждый запрос считается отдельно.
export const options = {
    scenarios: {
        load_test: {
            executor: 'ramping-arrival-rate',
            startRate: 0,
            timeUnit: '1s',
            preAllocatedVUs: 100,
            maxVUs: 7000,
            stages: [
                { duration: TEST_DURATION, target: 300 },
            ],
            gracefulStop: '30s',
        },
        metrics_poll: {
            executor: 'constant-arrival-rate',
            exec: 'pollServerMetrics',
            rate: 1,
            timeUnit: '1s',
            duration: TEST_DURATION,
            preAllocatedVUs: 1,
            maxVUs: 1,
            gracefulStop: '30s',
        },
    },

    thresholds: {
        'http_req_duration': ['p(95)<500'],
        'http_req_failed': ['rate<0.05'],
        'http_req_duration{name:users_random}': ['p(95)<500'],
        'http_req_duration{name:messages_by_chat}': ['p(95)<1000'],
        'http_req_duration{name:create_message}': ['p(95)<1000'],
        'server_cpu_percent': ['avg>=0'],
        'server_ram_current_mb': ['avg>=0'],
        'server_disk_speed_mbps': ['avg>=0'],
    },
};

const BASE_URL = 'http://localhost:4200';

const requestParams = {
    headers: {
        'Content-Type': 'application/json',
    },
    timeout: '10s',
};

export function pollServerMetrics() {
    const response = http.get(`${BASE_URL}/api/info/metrics`, {
        ...requestParams,
        tags: { name: 'info_metrics' },
    });

    check(response, {
        'info_metrics: статус 200': (r) => r.status === 200,
    });

    if (response.status !== 200) {
        return;
    }

    try {
        const metrics = JSON.parse(response.body);
        serverCpu.add(metrics.cpu);
        serverRamCurrent.add(metrics.ram.current);
        serverRamMax.add(metrics.ram.max);
        serverDiskSpeed.add(metrics.disk.speed);
    } catch (e) {
        return;
    }
}

export default function () {
    let chatId;
    let userId;

    group('users_random', function () {
        const usersResponse = http.get(`${BASE_URL}/api/users/random`, {
            ...requestParams,
            tags: { name: 'users_random' },
        });
        check(usersResponse, {
            'users_random: статус 200': (r) => r.status === 200,
            'users_random: есть chats': (r) => {
                try {
                    const body = JSON.parse(r.body);
                    return body.chats && body.chats.length > 0;
                } catch (e) {
                    return false;
                }
            },
        });

        if (usersResponse.status !== 200) {
            return;
        }

        let userData;
        try {
            userData = JSON.parse(usersResponse.body);
        } catch (e) {
            return;
        }

        const chats = userData.chats;
        if (!chats || chats.length === 0 || !userData.user) {
            return;
        }

        chatId = chats[Math.floor(Math.random() * chats.length)].id;
        userId = userData.user.id;
    });

    if (!chatId || !userId) {
        return;
    }

    group('messages_by_chat', function () {
        const messagesResponse = http.get(`${BASE_URL}/api/messages/${chatId}`, {
            ...requestParams,
            tags: { name: 'messages_by_chat' },
        });
        check(messagesResponse, {
            'messages_by_chat: статус 200': (r) => r.status === 200,
            'messages_by_chat: есть messages': (r) => {
                try {
                    const body = JSON.parse(r.body);
                    return Array.isArray(body.messages);
                } catch (e) {
                    return false;
                }
            },
        });
    });

    group('create_message', function () {
        for (let i = 0; i < MESSAGES_TO_CREATE; i++) {
            const createResponse = http.post(`${BASE_URL}/api/messages/${chatId}`, null, {
                ...requestParams,
                tags: { name: 'create_message' },
            });
            check(createResponse, {
                'create_message: статус 201': (r) => r.status === 201,
            });
        }
    });
}

// Дополнительная информация в конце теста
export function handleSummary(data) {
    const totalReqs = data.metrics.http_reqs.values.count;
    const iterations = data.metrics.iterations?.values.count ?? 0;

    console.log('\n📊 ИТОГИ ТЕСТИРОВАНИЯ:');
    console.log(`✅ Всего HTTP-запросов: ${totalReqs}`);
    console.log(`🔁 Итераций (цепочек): ${iterations}`);
    console.log(`⏱️  Среднее время ответа: ${data.metrics.http_req_duration.values.avg.toFixed(2)} мс`);
    console.log(`🐌 p95 время ответа: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)} мс`);
    console.log(`❌ Процент ошибок: ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%`);
}
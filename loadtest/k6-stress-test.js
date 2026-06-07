//команда на запуск
//k6 version
//$env:K6_WEB_DASHBOARD="true"; k6 run k6-stress-test.js     //для PowerShell




import http from 'k6/http';
import { check, group } from 'k6';
import { Counter, Trend } from 'k6/metrics';

// Встроенный web dashboard не разбивает http_req_* по тегу name.
// Отдельные custom metrics — каждый эндпоинт виден на дашборде своей строкой.
const usersRandomReqs = new Counter('endpoint_users_random_reqs');
const messagesByChatReqs = new Counter('endpoint_messages_by_chat_reqs');
const createMessageReqs = new Counter('endpoint_create_message_reqs');
const usersRandomDuration = new Trend('endpoint_users_random_duration', true);
const messagesByChatDuration = new Trend('endpoint_messages_by_chat_duration', true);
const createMessageDuration = new Trend('endpoint_create_message_duration', true);

const MESSAGES_TO_CREATE = 5;

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
                { duration: '10m', target: 1000 },
            ],
            gracefulStop: '1m',
        },
    },

    thresholds: {
        'http_req_duration': ['p(95)<500'],
        'http_req_failed': ['rate<0.05'],
        'http_req_duration{name:users_random}': ['p(95)<500'],
        'http_req_duration{name:messages_by_chat}': ['p(95)<1000'],
        'http_req_duration{name:create_message}': ['p(95)<1000'],
        'endpoint_users_random_duration': ['p(95)<500'],
        'endpoint_messages_by_chat_duration': ['p(95)<1000'],
        'endpoint_create_message_duration': ['p(95)<1000'],
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
    let chatId;
    let userId;

    group('users_random', function () {
        const usersResponse = http.get(`${BASE_URL}/api/users/random`, {
            ...requestParams,
            tags: { name: 'users_random' },
        });
        usersRandomReqs.add(1);
        usersRandomDuration.add(usersResponse.timings.duration);

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
        messagesByChatReqs.add(1);
        messagesByChatDuration.add(messagesResponse.timings.duration);

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
            createMessageReqs.add(1);
            createMessageDuration.add(createResponse.timings.duration);

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

    const endpointStats = [
        ['users_random', 'endpoint_users_random_reqs', 'endpoint_users_random_duration'],
        ['messages_by_chat', 'endpoint_messages_by_chat_reqs', 'endpoint_messages_by_chat_duration'],
        ['create_message', 'endpoint_create_message_reqs', 'endpoint_create_message_duration'],
    ];
    for (const [label, reqsMetric, durationMetric] of endpointStats) {
        const reqs = data.metrics[reqsMetric]?.values;
        const duration = data.metrics[durationMetric]?.values;
        if (reqs && duration) {
            console.log(`\n📍 ${label}:`);
            console.log(`   запросов: ${reqs.count}`);
            console.log(`   avg: ${duration.avg.toFixed(2)} мс, p95: ${duration['p(95)'].toFixed(2)} мс`);
        }
    }

    return {
        'summary.json': JSON.stringify(data, null, 2),
    };
}
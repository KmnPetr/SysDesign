//команда на запуск
//k6 version
//$env:K6_WEB_DASHBOARD="true"; k6 run k6-stress-test.js     //для PowerShell




import http from 'k6/http';
import { check, sleep } from 'k6';

// Настройки теста
export const options = {
    scenarios: {
        // Основная нагрузка
        load_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 20 },  // Разгон до 20 пользователей за 30 сек
                { duration: '1m', target: 20 },   // Держим 20 пользователей 1 минуту
                { duration: '30s', target: 0 },   // Спад до 0 за 30 сек
            ],
            gracefulRampDown: '30s',
        },
    },

    // Пороги (критерии успешности теста)
    thresholds: {
        'http_req_duration': ['p(95)<500'],  // 95% запросов должны быть быстрее 500мс
        'http_req_failed': ['rate<0.05'],    // Ошибок должно быть меньше 5%
    },
};

export default function () {
    // URL для тестирования
    const url = 'http://localhost:4200/api/users/random';

    // Параметры запроса
    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
        // Таймаут (опционально)
        timeout: '10s',
    };

    // Отправляем GET запрос
    const response = http.get(url, params);

    // Проверки
    check(response, {
        '✅ статус 200': (r) => r.status === 200,
        '✅ тело ответа не пустое': (r) => r.body && r.body.length > 0,
        '✅ ответ содержит JSON': (r) => {
            try {
                JSON.parse(r.body);
                return true;
            } catch (e) {
                return false;
            }
        },
        '✅ время ответа < 1 сек': (r) => r.timings.duration < 1000,
    });

    // Пауза между запросами (эмуляция реального пользователя)
    sleep(1);
}

// Дополнительная информация в конце теста
export function handleSummary(data) {
    console.log('\n📊 ИТОГИ ТЕСТИРОВАНИЯ:');
    console.log(`✅ Всего запросов: ${data.metrics.http_reqs.values.count}`);
    console.log(`⏱️  Среднее время ответа: ${data.metrics.http_req_duration.values.avg.toFixed(2)} мс`);
    console.log(`🐌 p95 время ответа: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)} мс`);
    console.log(`❌ Процент ошибок: ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%`);

    return {
        'summary.json': JSON.stringify(data, null, 2),  // Сохраняем детальный отчет в JSON
    };
}
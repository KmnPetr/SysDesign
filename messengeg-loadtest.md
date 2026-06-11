# результаты нагрузочного тестирования

## входные данные

Таблицы:
   - users: 2_390_000 записей
   - chats: 13_115_058 записей
   - users_chats: 26_230_116 записей
   - messages: 162_576_450 записей

SQL таблиц: [`loadtest/src/main/resources/db/migration/V1__create_user_chat_message.sql`](loadtest/src/main/resources/db/migration/V1__create_user_chat_message.sql)

вес постгресс данных: 37.9GB

генерация данных: данные генерировались с учетом получения кореляции приближенной к 0, все писались с предварительно перемешанными id и FK

## сценарий тестирования
(сценарий - это попытка воссоздать пользовательский опыт использования приложения)

### 1 шаг: пользователь входит в приложение, подгружаются данные список его чатов и доп информация
делается запрос, на сервер делается запрос, выдает данные по случайному из имеющихся пользователей

```http
GET http://localhost:4200/api/users/random
```
```json
{
  user: { ... текущий пользователь ...},
  chats: [ ... список его чатов ... ],
  user_chats: [ ... список связей many to many с доп инфой ...]
}
```

### 2 шаг: пользователь открывает один из чатов: скрипт js нагрузочного тестирования выбирает один из чатов и по его id делает запрос

```http
GET http://localhost:4200/api/messages/{chat_id}
```
```json
{
  "chat": { ... информация по чату ...},
  "users": [ ... сам пользователь и его собеседник ... ],
  "user_chats": [ ... список связей many to many обоих пользователей ... ],
  "messages": [ ... все имеющиеся сообщения чата ... ]
}
```

### 3 шаг: пользователь пишет 5 сообщений в чат

```http
POST http://localhost:4200/api/messages/{chat_id}
```
сервер сам создает message со случайной строкой


все 3 шага в сумме = 7 запросов

## Домашний комп:

- cpu: 8x16
- ram: 16 (но докеру доступно не все)
- диск: Netac NVMe SSD 512GB
- виртуальный стек: windows/wsl/docker/postgresql-18
- доп иструменты: java21, node_exporter, k6

---

стресс тест проводился в течении 30 мин с монотонно возрастающей нагрузкой до 3500 r/s; maxVUs: 20000

report: [`./loadtest/report/stress-2026-06-08_02-34-08.html`](./loadtest/report/stress-2026-06-08_02-34-08.html)


## Недомашний комп:
- cpu: AMD Ryzen 9 9950X (16 cores / 32 threads)
- ram: 4 * 32 = 128 GB DDR5
- диск: Samsung SSD 9100 PRO 4TB
- виртуальный стек: ubuntu24/docker/postgresql-18
- доп иструменты: java21, node_exporter, k6

---

стресс тест проводился в течении 30 мин с монотонно возрастающей нагрузкой до 3500 r/s; maxVUs: 20000

report: [`./loadtest/report/stress-2026-06-08_08-51-46.html`](./loadtest/report/stress-2026-06-08_08-51-46.html)

---

стресс тест проводился в течении 30 мин с монотонно возрастающей нагрузкой до 7000 r/s; maxVUs: 20000

report: [`./loadtest/report/stress-2026-06-08_09-37-20.html`](./loadtest/report/stress-2026-06-08_09-37-20.html)

---

поправил конфиги постгреса, увеличил дефолтные значения

```
listen_addresses = '*' → оставить как есть
max_connections = 100 → 200 (если нет пула соединений; иначе лучше 100 и PgBouncer)
shared_buffers = 128MB → 25GB
effective_cache_size = 4GB → 70–80GB
work_mem = 4MB → 32MB (до 64MB если тяжёлые сортировки/агрегации)
maintenance_work_mem = 64MB → 2GB
autovacuum = on → оставить как есть
autovacuum_max_workers = 3 → 10
autovacuum_naptime = 1min → 10–20s (для активной записи)
autovacuum_vacuum_cost_delay = 2ms → 0–1ms
autovacuum_vacuum_cost_limit = -1 → 2000–5000
autovacuum_worker_slots = 16 → 16 (оставить, если это кастомная сборка)
wal_level = replica → replica (оставить, logical только если нужен CDC)
fsync = on → оставить как есть (обязательно для безопасности)
synchronous_commit = on → off (если можно потерять последние миллисекунды ради скорости)
wal_compression = off → on
wal_buffers = -1 → 64MB
checkpoint_timeout = 5min → 15min
checkpoint_completion_target = 0.9 → 0.9 (оставить)
max_wal_size = 1GB → 32GB
min_wal_size = 80MB → 2GB
effective_io_concurrency = 16 → 200
maintenance_io_concurrency = 16 → 200
max_worker_processes = 8 → 30
max_parallel_workers = 8 → 16
max_parallel_workers_per_gather = 2 → 6
random_page_cost = 4.0 → 1.1 (SSD/NVMe)
seq_page_cost = 1.0 → 1.0 (оставить)
shared_preload_libraries = '' → оставить пустым (если нет расширений)
```

---

стресс тест проводился в течении 30 мин с монотонно возрастающей нагрузкой до 7000 r/s; maxVUs: 20000  повтор

report: [`./loadtest/report/stress-2026-06-08_10-48-14.html`](./loadtest/report/stress-2026-06-08_10-48-14.html)

метрики на конец теста: [`./loadtest/report/111111.png`](./loadtest/report/111111.png)

---

стресс тест проводился в течении 30 мин с монотонно возрастающей нагрузкой до 14000 r/s; maxVUs: 20000

report: [`./loadtest/report/stress-2026-06-08_11-28-12.html`](./loadtest/report/stress-2026-06-08_11-28-12.html)

метрики на конец теста: [`./loadtest/report/222222.png`](./loadtest/report/222222.png)


---

увеличил размер даты до 1.2Тб
{
"users":"78_441_000",
"chats":"431_233_507",
"users_chats":"862_467_014",
"messages":"5_114_770_149",
"endpoints":[
"GET http://localhost:4200/api/info",
"GET http://localhost:4200/api/users/random",
"GET http://localhost:4200/api/messages/{chat_id}",
"POST http://localhost:4200/api/messages/{chat_id}",
"GET http://localhost:4200/api/messages/random",
"GET http://localhost:4200/api/write/stop"]}

стресс тест проводился в течении 30 мин с монотонно возрастающей нагрузкой до 14000 r/s; maxVUs: 20000
report: ./loadtest/report/stress-2026-06-11_06-08-47.html
image: ./loadtest/report/33333.png

---

стресс тест проводился в течении 30 мин с монотонно возрастающей нагрузкой до 28000 r/s; maxVUs: 40000
report: ./loadtest/report/stress-2026-06-11_06-56-08.html

 tablename | attname | correlation 
-----------+---------+-------------
 users     | id      |  0.99999994
 chats     | id      |  0.99999994
 messages  | id      |  0.99999946
 
 SELECT
    tablename,
    attname,
    correlation,
    n_distinct
FROM pg_stats
WHERE (tablename, attname) IN (
    ('messages', 'chat_id'),
    ('messages', 'user_id'),
    ('users_chats', 'chat_id'),
    ('users_chats', 'user_id'),
    ('users_chats', 'last_read_msg_id')
)
ORDER BY tablename, attname;
  tablename  |     attname      | correlation |   n_distinct   
-------------+------------------+-------------+----------------
 messages    | chat_id          |   0.9943944 |   7.384133e+07
 messages    | user_id          |         0.4 | -0.00013333559
 users_chats | chat_id          |  0.99999994 |   5.941421e+06
 users_chats | last_read_msg_id |             |              0
 users_chats | user_id          |  0.99999994 |         632328




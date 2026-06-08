результаты нагрузочного тестирования

входные данные

Таблицы:
   - users: 2_390_000 записей
   - chats: 13_115_058 записей
   - users_chats: 26_230_116 записей
   - messages: 162_576_450 записей

SQL таблиц: loadtest/src/main/resources/db/migration/V1__create_user_chat_message.sql

вес постгресс данных: 37.9GB

генерация данных: данные генерировались с учетом получения кореляции приближенной к 0, все писались с предварительно перемешанными id и FK

сценарий тестирования
(сценарий - это попытка воссоздать пользовательский опыт использования приложения)
1 шаг: пользователь входит в приложение, подгружаются данные список его чатов и доп информация
делается запрос, на сервер делается запрос, выдает данные по случайному из имеющихся пользователей
GET http://localhost:4200/api/users/random
responce:
{
user: { ... текущий пользователь ...},
chats: [ ... список его чатов ... ],
user_chats: [ ... список связей many to many с доп инфой ...]
}
2 шаг: пользователь открывает один из чатов: скрипт js нагрузочного тестирования выбирает один из чатов и по его id делает запрос
GET http://localhost:4200/api/messages/{chat_id}
responce:
   {
   "chat": { ... информация по чату ...},
   "users": [ ... сам пользователь и его собеседник ... ],
   "user_chats": [ ... список связей many to many обоих пользователей ... ],
   "messages": [ ... все имеющиеся сообщения чата ... ]
   }
3 шаг: пользователь пишет 5 сообщений в чат
POST http://localhost:4200/api/messages/{chat_id}


все 3 шага в сумме = 7 запросов

Домашний комп:
cpu: 8x16
ram: 16 (но докеру доступно не все)
диск: Netac NVMe SSD 512GB
виртуальный стек: windows/wsl/docker/postgresql-18
доп иструменты: java21, node_exporter, k6

стресс тест проводился в течении 30 мин с монотонно возрастающей нагрузкой до 3500 r/s; maxVUs: 20000
report: ./loadtest/report/stress-2026-06-08_02-34-08.html


Недомашний комп:
cpu: 32
ram: 132
диск: Samsung SSD 9100 PRO 4TB
виртуальный стек: ubuntu24/docker/postgresql-18
доп иструменты: java21, node_exporter, k6
стресс тест проводился в течении 30 мин с монотонно возрастающей нагрузкой до 3500 r/s; maxVUs: 20000
report: ./loadtest/report/stress-2026-06-08_08-51-46.html
стресс тест проводился в течении 30 мин с монотонно возрастающей нагрузкой до 7000 r/s; maxVUs: 20000
report ./loadtest/report/stress-2026-06-08_09-37-20.html

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

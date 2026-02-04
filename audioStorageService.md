# Схема создания аудио контента, хранения, сбора статистики и анализа

### Создание, хранение, публикация
- клиент обращается на сервис для создания нового аудиотрека
- сервис обрабатывает аудиофайл, приводит его к единому формату при помощи утилиты ffmpeg
- сохраняет файл в S3 хранилище
- сохраняет метаданные аудиофайла (ссылка файла в S3, атрибуты оценки произведения выставленные создателем, стиль, жанр и др.) в бд Postgres
- отправляет запись в Elasticsearch
- делает запрос на ноды CDN для предварительного кеширования (можно простой запрос на получение файла автоматом закеширует файл)
- свеже созданные записи будут активно продвигаться первые сутки, CDN файлы cо свежим временем создания кеширует на большее время
- система рекомендаций активно продвигает новые записи

```mermaid
graph LR


    CLIENT["<b>Client</b>"]


    S3[("<b> · S3 Minio 📀🎵   · </b><br/>• Хранение аудио файлов")]
    MUSIC_STORAGE["<b>Audio Service 🎶</b><br/>• Создание новой записи audio<br/> • Валидация форматов ffmpeg"]
    POSTGRES[("<b>Postgresql</b><br/>• Хранение ссылок и атрибутов на файлы audio")]
    ELAST[("Elasticsearch")]
    EDGE_NODE["<b>Edge Nodes (CDN)</b><br/>• предварительный кеш новых audio"]

    CLIENT ---> |create audio| MUSIC_STORAGE
    MUSIC_STORAGE ---> POSTGRES
    MUSIC_STORAGE ---> S3
    MUSIC_STORAGE ---> ELAST
    MUSIC_STORAGE ---> |запрос для кеша|EDGE_NODE

    %% Стили
    style EDGE_NODE fill:#d1ecff,stroke:#3399cc,stroke-width:2px
    style CLIENT fill:#e6f0ff,stroke:#3366cc,stroke-width:2px
    style POSTGRES fill:#d4edda,stroke:#33cc66,stroke-width:2px
    style S3 fill:#fff3b3,stroke:#e6b800,stroke-width:2px
```

### Сбор статистики, просчет рекомендаций для пользователя
- Statistic Service собирает статистику по прослушиваниям треков а также делает просчет рекомендаций для пользователей (можно поделить на 2 сервиса)
- каждая Edge Node (CDN) ведет подсчет количесва обращений пользователей за аудио, а также запоминает id самих пользователей, некоторые вспомогательные метаданные например время прослушивания
- в определенные промежутки времени скидывает статистику на Statistic Service
- Statistic Service пишет логи прослушиваний в ClickHouse (append-only)
- раз в сутки запускаются Runners анализа collaborative filtering, полученные результаты созраняются в туже или свою ClickHouse

```mermaid
graph LR

    EDGE_NODE1["<b>Edge Node (CDN)</b><br/>• Подсчет статистики<br/>• Сброс логов в Statistic Service"]
    EDGE_NODE2["<b>Edge Node (CDN)</b><br/>• Подсчет статистики<br/>• Сброс логов в Statistic Service"]
    STAT["<b>Statistic Service 📊</b>"]
    CLHS[("<b>ClickHouse</b><br/>• (append-only)")]
    RUNNERS["Runners<br/>• анализ предпочтений пользователей<br/>• collaborative filtering"]


    EDGE_NODE1 ---> |logs| STAT
    EDGE_NODE2 ---> |logs| STAT
    STAT ---> |logs| CLHS
    STAT ---> RUNNERS
    

    %% Стили
    style EDGE_NODE1 fill:#d1ecff,stroke:#3399cc,stroke-width:2px
    style EDGE_NODE2 fill:#d1ecff,stroke:#3399cc,stroke-width:2px
    style CLHS fill:#d4edda,stroke:#33cc66,stroke-width:2px
    style STAT fill:#ffe6f0,stroke:#cc3399,stroke-width:2px
    style RUNNERS fill:#c3e6cb,stroke:#28a745,stroke-width:1px
```

### Чтение пользователями записей audio
- пользователь обращается на Audio Service для получения списков audio
- Audio Service обращается к Elasticsearch при запросе от пользователя поиска аудиотреков по имени или другим текстовым атрибутам
- Elasticsearch выдает сервису список подходящих id аудиотреков
- сервис по полученным id запрашивает обьекты audio у Redis или Postgresql
- для получения списка id рекомендованных пользвателю audio  Audio Service делает запрос на Statistic Service
- Audio Service может сортировать записи перед отдачей например если их создатели платят за продвижение
- после получения метаданных о треках пользователь загружает файл-audio c CDN

```mermaid
graph LR

    CLIENT["<b>Client</b>"]
    MUSIC_STORAGE["<b>Audio Service 🎶</b>"]
    ELAST[("Elasticsearch")]
    REDIS[("<b>Redis</b><br/>• Кеш для read-запросов")]
    POSTGRES[("<b>Postgresql</b>")]
    EDGE_NODE["<b>Edge Node (CDN)</b>"]
    STAT["<b>Statistic Service 📊</b>"]

    CLIENT ---> |GET search or recommendations| MUSIC_STORAGE
    MUSIC_STORAGE ---> |search| ELAST[("Elasticsearch")]
    MUSIC_STORAGE ---> |cache| REDIS
    MUSIC_STORAGE ---> POSTGRES
    MUSIC_STORAGE ---> |recommendations| STAT
    CLIENT ---> |load audio| EDGE_NODE

    %% Стили
    style EDGE_NODE fill:#d1ecff,stroke:#3399cc,stroke-width:2px
    style CLIENT fill:#e6f0ff,stroke:#3366cc,stroke-width:2px
    style POSTGRES fill:#d4edda,stroke:#33cc66,stroke-width:2px
    style STAT fill:#ffe6f0,stroke:#cc3399,stroke-width:2px
```


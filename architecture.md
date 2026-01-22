# Архитектура музыкальной платформы ТУНЕЦ

```mermaid
graph LR


    CLIENT["<b><font size='5'>Client</font></b><br/>• Web<br/>• iOS<br/>• Android"]

    subgraph LB["<b>Load Balancers</b>"]
        direction LR
        LB1[LB-1]
        LB2[LB-2]
    end

    CDN(("<b> ·   CDN 📀🎵🖼️   · </b><br/>• Хранение аудиофайлов, изображений"))


    subgraph K8S["<b>Kubernetes Cluster</b>"]
        direction TB
        INGRESS1[NGINX Ingress 1]
        INGRESS2[NGINX Ingress 2]

        AUTH["<b><font size='5'>Authentication Service 🔐</font></b><br/>• Регистрация<br/>• Авторизация<br/>• Аутентификация<br/>• Генерация токенов<br/>• Поддержка входа<br/> через аккаунты<br/> (Google, Apple, Yandex)<br/>• Заблок. токены"]
        PAYMENT["<b>Payment Service 💳</b><br/>• Оплата подписки<br/>• Промокоды<br/>• Акции"]

        subgraph MUSIC_STORAGE["<b>Audio Storage Service 🎶</b>"]
            direction LR
            NOTE2["• Хранение ссылок<br/> на audio в CDN<br/>• Хранение плейлистов<br/>• Логика публикации<br/> новых audio<br/> пользователями"]
            REDIS[("Redis")]
        end

        USER_DATA["<b>User Data Service 👤</b><br/>• История<br/> воспроизведений<br/>• Лайков<br/>• Личные альбомы<br/>• Аватарка"]

        subgraph CDN_SR["<b>CDN Service</b>"]
            direction LR
            NOTE["• Валидация форматов<br/> медиафайлов перед<br/> отправкой в CDN"]
            FFMPEG(("ffmpeg"))
        end

        MONITOR["<b>Monitoring Services 📊</b><br/>• Prometheus<br/>• Grafana<br/>• Loki<br/>• Alertmanager<br/>• Node Exporter<br/>• kube-state-metrics"]
    end

    STORE1[("<b>SQL Store</b>")]
    OAUTH(("OAuth2 Providers<br/>• Google<br/>• Apple<br/>• Yandex"))
    STORE2[("<b>SQL Store</b>")]
    STORE3[("<b>SQL Store</b>")]
    STORE4[("<b>SQL Store</b>")]
    BILLING((Billing<br/>Stripe · PayPal<br/> · СБП · др.))


    %%VAULT["<b>Vault 🔑</b><br/>• Хранение секретов"]




    %% Соединения
    CLIENT --> LB1
    CLIENT --> LB2

    LB1 --> INGRESS1
    LB1 --> INGRESS2
    LB2 --> INGRESS1
    LB2 --> INGRESS2



    INGRESS1 ---> AUTH
    INGRESS2 ---> PAYMENT
    INGRESS2 ---> MUSIC_STORAGE
    INGRESS1 ---> USER_DATA
    INGRESS2 ---> CDN_SR

    AUTH ---> STORE1

    CLIENT ---> CDN
    CDN <---> CDN_SR
    AUTH ---> OAUTH
    PAYMENT---> STORE2
    PAYMENT---> BILLING
    USER_DATA--->STORE3
    MUSIC_STORAGE--->STORE4

    %% Стили
    style CLIENT fill:#e6f0ff,stroke:#3366cc,stroke-width:2px
    style LB fill:#d4edda,stroke:#33cc66,stroke-width:2px
    style LB1 fill:#c3e6cb,stroke:#28a745,stroke-width:1px
    style LB2 fill:#c3e6cb,stroke:#28a745,stroke-width:1px
    style K8S fill:#f0d4ff,stroke:#9933cc,stroke-width:2px
    style STORE1 fill:#d4edda,stroke:#33cc66,stroke-width:2px
    style STORE2 fill:#d4edda,stroke:#33cc66,stroke-width:2px
    style STORE3 fill:#d4edda,stroke:#33cc66,stroke-width:2px
    style STORE4 fill:#d4edda,stroke:#33cc66,stroke-width:2px
    style INGRESS1 fill:#e6ccff,stroke:#9933cc,stroke-width:1px
    style INGRESS2 fill:#e6ccff,stroke:#9933cc,stroke-width:1px
    style AUTH fill:#e6f0ff,stroke:#3366cc,stroke-width:2px
    style PAYMENT fill:#d4edda,stroke:#33cc66,stroke-width:2px
    style MUSIC_STORAGE fill:#fddede,stroke:#cc3333,stroke-width:2px
    style USER_DATA fill:#ffe6f0,stroke:#cc3399,stroke-width:2px
    style CDN fill:#fff3b3,stroke:#e6b800,stroke-width:2px
    style OAUTH fill:#fff3cd,stroke:#e6b800,stroke-width:2px
    style NOTE fill:transparent,stroke-width:0
    style MONITOR fill:#d1f0ff,stroke:#3399cc,stroke-width:2px
    style NOTE2 fill:transparent,stroke-width:0
```

----
----
## Пример горизонтального масштабирования Authentication Service
```mermaid
graph LR
    subgraph AUTH["<b><font size='5'>Authentication Service</font></b>"]
        direction TB
        IM1((Impl 1))
        IM2((Impl 2))
        IM3((Impl 3))
    end
    
    subgraph STORE["<b>Store</b>"]
        direction TB
        PG1[(PostgreSQL 1)]
        PG2[(PostgreSQL 2)]
        PG3[(PostgreSQL 3)]
    end

    IM1--->PG1
    IM1--->PG2
    IM2--->PG1
    IM2--->PG3
    IM3--->PG2
    IM3--->PG3

    style AUTH fill:#e6f0ff,stroke:#3366cc,stroke-width:2px
    style STORE fill:#d4edda,stroke:#33cc66,stroke-width:2px
```
| Таблица       | Поля                     | Назначение                                                              |
|---------------|--------------------------|-------------------------------------------------------------------------|
| **users**     | uuid, email, phone, …    | Основная таблица пользователей (хранит полный профиль пользователя)     |
| **protoindex**| (email/phone), uuid      | Вспом.таблица для распределённого поиска (по уникальному полю ищет uuid)|


```mermaid
sequenceDiagram
    participant Auth as Authentication Service
    participant Store as Store

    Auth->>Store: SELECT uuid FROM protoindex WHERE (email/phone) = ?
    Store-->>Auth: return uuid

    Auth->>Store: SELECT * FROM users WHERE uuid = ?
    Store-->>Auth: return user
```

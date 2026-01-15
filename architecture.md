# Архитектура музыкальной платформы ТУНЕЦ

## Клиентские приложения

```mermaid
graph LR
    CLIENT["<b><font size='5'>Client</font></b><br/>• Web<br/>• iOS<br/>• Android"]

    subgraph LB["Load Balancers"]
        direction LR
        LB1[LB-1]
        LB2[LB-2]
    end

    subgraph K8S["Kubernetes Cluster"]
        direction TB
        INGRESS1[NGINX Ingress 1]
        INGRESS2[NGINX Ingress 2]

        AUTH["<b><font size='5'>Authentication Service</font></b><br/>• Регистрация<br/>• Авторизация<br/>• Аутентификация<br/>• Генерация токенов<br/>• Поддержка входа через аккаунты (Google, Apple, Yandex) — OAuth2<br/>• Заблок. токены"]
        PAYMENT["Payment Service"]
        AUDIO_PUBLISH["Audio Publishing Service"]
        MUSIC_STORAGE["Audio Storage Service"]
        USER_DATA["User Data Service"]
        SDN_SR["SDN Service"]
    end

    subgraph STORE["<b>Store</b>"]
        direction TB
        PG1[(PostgreSQL 1)]
        PG2[(PostgreSQL 2)]
    end


    CDN(("<b>CDN</b>"))
    OAUTH(("OAuth2 Providers<br/>• Google<br/>• Apple<br/>• Yandex"))

    %% Соединения
    CLIENT --> LB1
    CLIENT --> LB2

    LB1 --> INGRESS1
    LB1 --> INGRESS2
    LB2 --> INGRESS1
    LB2 --> INGRESS2

    INGRESS1 ---> AUTH
    INGRESS2 ---> PAYMENT
    INGRESS1 ---> AUDIO_PUBLISH
    INGRESS2 ---> MUSIC_STORAGE
    INGRESS1 ---> USER_DATA
    INGRESS2 ---> SDN_SR

    AUTH ---> STORE

    CLIENT ---> CDN
    CDN ---> SDN_SR
    AUTH ---> OAUTH

    %% Стили
    style CLIENT fill:#e6f0ff,stroke:#3366cc,stroke-width:2px

    style LB fill:#d4edda,stroke:#33cc66,stroke-width:2px
    style LB1 fill:#c3e6cb,stroke:#28a745,stroke-width:1px
    style LB2 fill:#c3e6cb,stroke:#28a745,stroke-width:1px
    style K8S fill:#f0d4ff,stroke:#9933cc,stroke-width:2px
    style STORE fill:#d4edda,stroke:#33cc66,stroke-width:2px
    style INGRESS1 fill:#e6ccff,stroke:#9933cc,stroke-width:1px
    style INGRESS2 fill:#e6ccff,stroke:#9933cc,stroke-width:1px
    style AUTH fill:#e6f0ff,stroke:#3366cc,stroke-width:2px
    style PAYMENT fill:#d4edda,stroke:#33cc66,stroke-width:2px
    style AUDIO_PUBLISH fill:#fff3cd,stroke:#e6b800,stroke-width:2px
    style MUSIC_STORAGE fill:#fddede,stroke:#cc3333,stroke-width:2px
    style USER_DATA fill:#ffe6f0,stroke:#cc3399,stroke-width:2px
    style CDN fill:#fff3b3,stroke:#e6b800,stroke-width:2px
    style OAUTH fill:#fff3cd,stroke:#e6b800,stroke-width:2px
```

----
----
## Authentication Service

```mermaid
graph LR
    INVISIBLE["     "]
    AUTH["<b><font size='5'>Authentication Service</font></b><br/>• Регистрация<br/>• Авторизация<br/>• Аутентификация<br/>• Генерация токенов<br/>• Поддержка входа через аккаунты (Google, Apple, Yandex) — OAuth2<br/>• Заблок. токены"]
    
    PG[(PostgreSQL)]
    OAUTH(("OAuth2 Providers<br/>• Google<br/>• Apple<br/>• Yandex"))

    INVISIBLE --->|HTTP/gRPC/Kafka| AUTH
    AUTH ---> PG
     AUTH ---> OAUTH

    style INVISIBLE fill:transparent,stroke-width:0
    style AUTH fill:#e6f0ff,stroke:#3366cc,stroke-width:2px
    style OAUTH fill:#fff3cd,stroke:#e6b800,stroke-width:2px
```

### горизонтальное масштабирование Authentication Service
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

UI отправляет запрос к API, который обрабатывает запрос и взаимодействует с базой данных PostgreSQL и сервисом авторизации (Auth Service).  

## Диаграмма потока

```mermaid
graph TD
    %% Authentication Service
    AUTH["<b><font size='5'>Authentication Service</font></b><br/>• Регистрация<br/>• Авторизация<br/>• Аутентификация<br/>• Генерация токенов<br/>• Поддержка входа через аккаунты (Google, Apple, Yandex) — OAuth2<br/>"]
    style AUTH fill:#e6f0ff,stroke:#3366cc,stroke-width:2px
    
    %% Payment Service
    PAYMENT["Payment Service\n- Обработка платежей\n- Проверка карт\n- Создание транзакций"]
    
    %% Audio Publishing Service
    AUDIO_PUBLISH["Audio Publishing Service\n- Принимает аудиофайл\n- Конвертация через FFmpeg\n- Отправка в CDN\n- Сохранение инфо о треке в БД"]
    
    %% Music Storage Service
    MUSIC_STORAGE["Music Storage Service\n- Хранение ссылок на треки в CDN\n- Хранение плейлистов в БД\n- Раздача треков пользователю"]
    
    %% Non-Auth User Data Service
    USER_DATA["Non-Auth User Data Service\n- История прослушивания\n- Личные плейлисты\n- Список понравившихся песен"]

    %% Стили
    style AUTH fill:#e6f0ff,stroke:#3366cc,stroke-width:2px
    style PAYMENT fill:#d4edda,stroke:#33cc66,stroke-width:2px
    style AUDIO_PUBLISH fill:#fff3cd,stroke:#e6b800,stroke-width:2px
    style MUSIC_STORAGE fill:#fddede,stroke:#cc3333,stroke-width:2px
    style USER_DATA fill:#ffe6f0,stroke:#cc3399,stroke-width:2px
```

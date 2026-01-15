# Архитектура музыкальной платформы ТУНЕЦ

## Клиентские приложения

```mermaid
graph LR
    subgraph CLIENT["Client"]
        direction LR
        ANDROID[Android]
        IOS[iOS]
        WEB[Web]

        ANDROID --- IOS
        IOS --- WEB
    end

    %% скрываем линии
    linkStyle 0 stroke-width:0
    linkStyle 1 stroke-width:0

    style CLIENT fill:#e6f0ff,stroke:#3366cc,stroke-width:3px
    style ANDROID fill:#fff3cd,stroke:#e6b800,stroke-width:1px
    style IOS fill:#fff3cd,stroke:#e6b800,stroke-width:1px
    style WEB fill:#fff3cd,stroke:#e6b800,stroke-width:1px
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

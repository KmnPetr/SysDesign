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
## Authentication Service

```mermaid
graph LR
    INVISIBLE["     "]
    AUTH["<b><font size='5'>Authentication Service</font></b><br/>• Регистрация<br/>• Авторизация<br/>• Аутентификация<br/>• Генерация токенов<br/>• Поддержка входа через аккаунты (Google, Apple, Yandex) — OAuth2<br/>• Заблок. токены"]
    
    subgraph STORE["<b>Store</b>"]
        direction TB
        POSTGRES1[(PostgreSQL 1)]
        POSTGRES2[(PostgreSQL 2)]
        POSTGRES3[(PostgreSQL 3)]
    end
    OAUTH(("OAuth2 Providers<br/>• Google<br/>• Apple<br/>• Yandex"))

    INVISIBLE --->|HTTP/gRPC/Kafka| AUTH
    AUTH ---> STORE
     AUTH ---> OAUTH

    style INVISIBLE fill:transparent,stroke-width:0
    style AUTH fill:#e6f0ff,stroke:#3366cc,stroke-width:2px
    style OAUTH fill:#fff3cd,stroke:#e6b800,stroke-width:2px
    style STORE fill:#d4edda,stroke:#33cc66,stroke-width:2px
```
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

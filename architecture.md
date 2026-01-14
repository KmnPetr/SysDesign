# Архитектура приложения — Функциональный поток

```mermaid
graph LR
    subgraph CLIENT["Client"]
        direction LR
        ANDROID[Android]
        IOS[iOS]
        WEB[Web]
    end

    style CLIENT fill:#e6f0ff,stroke:#3366cc,stroke-width:3px
    style ANDROID fill:#fff3cd,stroke:#e6b800,stroke-width:1px
    style IOS fill:#fff3cd,stroke:#e6b800,stroke-width:1px
    style WEB fill:#fff3cd,stroke:#e6b800,stroke-width:1px

## Сначала ***пользователь*** открывает веб-интерфейс (UI).  
UI отправляет запрос к API, который обрабатывает запрос и взаимодействует с базой данных PostgreSQL и сервисом авторизации (Auth Service).  

## Диаграмма потока

```mermaid
graph TD
    %% Основные компоненты
    UI[Web UI] --> API_CONTAINER
    API_CONTAINER --> DB[(PostgreSQL)]

    %% API контейнер с задачами внутри
    subgraph API_CONTAINER["API"]
        direction TB
        T1[Validate credentials]
        T2[Check token]
        T3[Load user roles]
        T4[Audit login]

        %% Внутренние связи между задачами
        T1 --> T2
        T2 --> T3
        T3 --> T4
    end

    %% Стили блоков
    style UI fill:#cce5ff,stroke:#3399ff,stroke-width:2px
    style API_CONTAINER fill:#d4edda,stroke:#33cc66,stroke-width:3px
    style DB fill:#cce5ff,stroke:#3366cc,stroke-width:2px
    style T1 fill:#fff3cd,stroke:#e6b800,stroke-width:1px
    style T2 fill:#fff3cd,stroke:#e6b800,stroke-width:1px
    style T3 fill:#fff3cd,stroke:#e6b800,stroke-width:1px
    style T4 fill:#fff3cd,stroke:#e6b800,stroke-width:1px

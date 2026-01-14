# Функциональный поток

Сначала ***пользователь*** открывает UI.  
UI отправляет запрос к API, который обрабатывает запрос и взаимодействует с базой данных PostgreSQL и сервисом авторизации.

```mermaid
graph TD
    %% Основные компоненты
    UI[Web UI] --> API[API]

    %% Задачи внутри API (Auth Service)
    API --> T1[Validate credentials]
    API --> T2[Check token]
    API --> T3[Load user roles]
    API --> T4[Audit login]

    API --> DB[(PostgreSQL)]

    %% Стили
    style UI fill:#cce5ff,stroke:#3399ff,stroke-width:2px
    style API fill:#d4edda,stroke:#33cc66,stroke-width:3px
    style DB fill:#cce5ff,stroke:#3366cc,stroke-width:2px
    style T1 fill:#fff3cd,stroke:#e6b800,stroke-width:1px
    style T2 fill:#fff3cd,stroke:#e6b800,stroke-width:1px
    style T3 fill:#fff3cd,stroke:#e6b800,stroke-width:1px
    style T4 fill:#fff3cd,stroke:#e6b800,stroke-width:1px

# Функциональный поток

Сначала ***пользователь*** открывает веб-интерфейс (UI).  
UI отправляет запрос к API, который обрабатывает запрос и взаимодействует с базой данных PostgreSQL и сервисом авторизации.

```mermaid
graph TD
    UI[Web UI] --> API[API]
    API --> Auth[Auth Service]
    API --> DB[(PostgreSQL)]
    Auth --> T1[Validate credentials]
    Auth --> T2[Check token]
    Auth --> T3[Load user roles]
    Auth --> T4[Audit login]

    %% Стили для блоков
    style UI fill:#cce5ff,stroke:#3399ff,stroke-width:2px
    style API fill:#d4edda,stroke:#33cc66,stroke-width:2px
    style DB fill:#cce5ff,stroke:#3366cc,stroke-width:2px
    style Auth fill:#e6f0ff,stroke:#3366cc,stroke-width:2px
    style T1 fill:#fff3cd,stroke:#e6b800,stroke-width:1px
    style T2 fill:#fff3cd,stroke:#e6b800,stroke-width:1px
    style T3 fill:#fff3cd,stroke:#e6b800,stroke-width:1px
    style T4 fill:#fff3cd,stroke:#e6b800,stroke-width:1px

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

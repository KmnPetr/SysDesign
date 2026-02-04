
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

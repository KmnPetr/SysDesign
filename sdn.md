# Архитектура CDN

## Выбор Edge Node (Geo DNS)
- перед обращением клиента на один из CDN серверов необходимо определить ближайший EdgeNode
- вариант с Geo DNS предполагает при определении ip-адреса Edge Node будет обращаться на Geo DNS, который в свою очередь будет выдавать пользователю ip подходящего для него по региону Edge Node


### Варианты Geo DNS

- **Anycast IP + BGP**  
  Доступно только у сетевых провайдеров как отдельная услуга. (мне больше нравится)

- **F5 BIG-IP DNS (F5 Networks)**  
  Платное решение. Для небольших проектов дорого, но работает стабильнее и функциональнее большинства open-source вариантов.

- **Open Source решения**  
  - PowerDNS, Knot DNS, BIND, greenstatic GeoDNS потыкать времени не было:)

```mermaid
graph LR

    CLIENT["<b><font size='5'>Client</font></b>"]

    GEO_DNS["<b>Geo DNS 🌐</b>"]

    EDGE_NODE1["<b>Edge Node (Россия)</b>"]
    EDGE_NODE2["<b>Edge Node (Россия)</b>"]
    EDGE_NODE3["<b>Edge Node (Германия)</b>"]
    EDGE_NODE4["<b>Edge Node (США)</b>"]

    BASE_STORAGE[("<b>Base Storage 🗄️</b>")]

    
    CLIENT ---> GEO_DNS
    CLIENT ---> EDGE_NODE1

    EDGE_NODE1 ---> BASE_STORAGE
    EDGE_NODE2 ---> BASE_STORAGE
    EDGE_NODE3 ---> BASE_STORAGE
    EDGE_NODE4 ---> BASE_STORAGE



    style EDGE_NODE1 fill:#d1ecff,stroke:#3399cc,stroke-width:2px
    style EDGE_NODE2 fill:#d1ecff,stroke:#3399cc,stroke-width:2px
    style EDGE_NODE3 fill:#d1ecff,stroke:#3399cc,stroke-width:2px
    style EDGE_NODE4 fill:#d1ecff,stroke:#3399cc,stroke-width:2px
    style CLIENT fill:#e6f0ff,stroke:#3366cc,stroke-width:2px
    style BASE_STORAGE fill:#d1ffd1,stroke:#33cc33,stroke-width:2px

```
## Выбор Edge Node ( 2-вариант ВК)
- выбор ближайшей Edge Node за счет проб пути с клиента
- клиент делает пробные запросы на каждую edge ноду, по времени отклика или по количеству hop (так делают ВК:)) выбирается ближайший
- минусы: множественные запросы на Edge_Node для проверки длины пути; дополнительный код на клиенте
- работает при небольшом количестве Edge_Node

```mermaid
graph LR

    CLIENT["<b><font size='5'>Client</font></b>"]

    HOP1["<b>Hop 1</b>"]
    HOP2["<b>Hop 2</b>"]
    HOP3["<b>Hop 3</b>"]

    EDGE_NODE["<b>Edge Node</b>"]

  CLIENT ---> HOP1
  HOP1 ---> HOP2
  HOP2 ---> HOP3
  HOP3 ---> EDGE_NODE
  EDGE_NODE ---> |TTL=3| CLIENT
  
    style EDGE_NODE fill:#d1ecff,stroke:#3399cc,stroke-width:2px
    style CLIENT fill:#e6f0ff,stroke:#3366cc,stroke-width:2px

```


## Архетиктура Edge Node

- файлы раздаються с nginx потому что он имеет более широкий функционал для раздачи файлов, кеширования, сжатия
- для описания более сложной логики на nginx используется Lua (например для общения с местным ранером Statistic Counter)
- подсчет статистики: количества запросов файлов, определения hot/cold файлов, управление временем кешированя ведется на каждой Edge Node отдельно при помощи ранера Statistic Counter.
- Это дает более точную статистику наиболее популярных файлов для конкретного региона, разгружает центральные сервера от множественных запросов статистики, возможность динамически определять размер кеша от обьема диска и др.
- можно дать запрос с центрального сервера на каждый Edge Node предварительно закешировать потенциально популярный контент или обновить старый (напр. html, js файлы)
- есть возможность настроить местную распределенную сеть S3 при больших обьемах кеширования
- настройка местной защиты от ddos

  - **Как считать hot/cold файлы**
  - Statistic Counter считает количество обращений за каждым файлом
  - раз в сутки количество опращений за файлом уменьшается на 10% (файлы потерявшие популярность будут постепенно понижаться до cold)
  - изходя из размера диска и общего количества запросов каждый файл расчитывается как cold/hot
  - дается команда nginx закешировать файл на определенное фремя, либо Statistic Counter сам управляет кешированием удаляя и добавляя файлы в хранилище

```mermaid
graph LR

    CLIENT["<b><font size='5'>Client</font></b>"]

    subgraph EDGE_NODE["<b>Edge Node</b>"]
      direction TB
      NGINX_LUA["Nginx + Lua 🌐<br/>• cashe"]
      STAT_COUNTER["<b>Statistic Counter</b><br/>• подсчет статистики<br/>• выставляет время кеширования<br/>• упреждающее кеширование<br/>• продление TTL<br/>• ddos"]
    end


    CLIENT ---> NGINX_LUA
    NGINX_LUA --> STAT_COUNTER
    STAT_COUNTER ---> NGINX_LUA

    BASE_STORAGE[("<b>Base Storage 🗄️</b><br/>• Хранение аудио/медиа файлов<br/>• S3 API совместимое<br/>• MinIO / Ceph / AWS S3")]

    NGINX_LUA ---> BASE_STORAGE

    style EDGE_NODE fill:#d1ecff,stroke:#3399cc,stroke-width:2px
    style CLIENT fill:#e6f0ff,stroke:#3366cc,stroke-width:2px
    style BASE_STORAGE fill:#d1ffd1,stroke:#33cc33,stroke-width:2px

```


# Base Storage

- центральное хранилище, хранит все файлы
- отдает по запросу на Edge Node файлы
- для хранения используется распределенная сеть из S3 MinIO
- на входе стоят Load Balancers перенаправляют запрос на узел MinIO по round-robin
- 
```mermaid
graph LR



    CLIENT["<b><font size='5'>Edge Node</font></b>"]

    subgraph LB["<b>Load Balancers</b>"]
        direction TB
        LB1[LB-1]
        LB2[LB-2]
    end

    subgraph S3["<b>S3</b>"]
        direction TB

        STORE1[("<b>MinIO</b>")]
        STORE2[("<b>MinIO</b>")]
        STORE3[("<b>MinIO</b>")]
        STORE4[("<b>MinIO</b>")]
    end

        CLIENT ---> LB1
        CLIENT ---> LB2
        LB1 ---> STORE1
        LB2 ---> STORE2
        LB1 ---> STORE3
        LB2 ---> STORE4

        STORE1 <---> STORE2
        STORE2 <---> STORE3
        STORE3 <---> STORE4




    style CLIENT fill:#e6f0ff,stroke:#3366cc,stroke-width:2px
    style LB fill:#d4edda,stroke:#33cc66,stroke-width:2px
    style LB1 fill:#c3e6cb,stroke:#28a745,stroke-width:1px
    style LB2 fill:#c3e6cb,stroke:#28a745,stroke-width:1px
    style S3 fill:#f0d4ff,stroke:#9933cc,stroke-width:2px
    style STORE1 fill:#d4edda,stroke:#33cc66,stroke-width:2px
    style STORE2 fill:#d4edda,stroke:#33cc66,stroke-width:2px
    style STORE3 fill:#d4edda,stroke:#33cc66,stroke-width:2px
    style STORE4 fill:#d4edda,stroke:#33cc66,stroke-width:2px
```


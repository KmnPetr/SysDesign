# Общая архетиктура

- схема логаута при использовании jwt
  1) клиент делает logout
  2) auth сервис вносит keyid jwt токена в revocation list редиса
  3) сервис, предоставляющий некую информацию пользователю при проверке jwt проверяет наличие его keyid в revocation list
```mermaid
graph LR


    CLIENT["<b><font size='5'>Client</font>"]

    REDIS[("<b>Redis</b><br/>• revocation list")]

    AUTH["<b><font size='5'>Authentication Service 🔐</font></b>"]

    DATA_SERVICE["<b>Data Service</b><br/>• Некоторый полезный ресурс"]







    CLIENT ---> |logout| AUTH
    AUTH ---> |keyid| REDIS
    CLIENT ---> |jwt| DATA_SERVICE
    DATA_SERVICE ---> |check keyid| REDIS



    style DATA_SERVICE fill:#ffe6f0,stroke:#cc3399,stroke-width:2px
    style CLIENT fill:#e6f0ff,stroke:#3366cc,stroke-width:2px
    style AUTH fill:#e6f0ff,stroke:#3366cc,stroke-width:2px
```

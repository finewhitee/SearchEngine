пше 
# SearchEngine Search Bot

![logo](https://img.shields.io/badge/SearchEngine-Поисковик-blue)

**SearchEngine — Всё найдём!**  
Локальный поисковик на Java, который можно запустить где угодно — на ПК, сервере, даже на холодильнике (если там есть JVM).

---

## 📦 Что это?

**SearchEngine** — локальный поисковый движок, написанный на Java с использованием Spring Boot. Предназначен для индексации сайтов и выполнения полнотекстового поиска с учётом морфологии.

Возможности использования:
- локальный поиск по закрытым сетям;
- добавление поиска на свой сайт через API;
- учебный проект для отработки Java, Spring и баз данных.

---

## 🚀 Возможности

- Индексация сайтов (полная, по одной странице, повторная)
- Лемматизация (русский и английский языки)
- Веб-интерфейс: Dashboard, Management, Search
- REST API с авторизацией
- Морфологический анализ (Lucene Morphology)
- Spring Security с ролями ADMIN и SEARCH_API_USER

---

## 🛠️ Технологии

- Java 17
- Spring Boot 2.7.x
- PostgreSQL
- Hibernate (JPA)
- Thymeleaf
- JSoup
- Apache Lucene Morphology
- Crawler-Commons
- Lombok
- JUnit 5, Mockito

---

## 🗂 Интерфейс

- **Dashboard** — статистика по сайтам, страницам, ошибкам
- **Management** — запуск/остановка индексации, добавление URL
- **Search** — поиск по сайту или всем сайтам

---

## 🔌 REST API

Все методы требуют авторизации.

| Метод | URL | Описание |
|-------|-----|----------|
| `GET` | `/api/startIndexing` | Запуск полной индексации |
| `GET` | `/api/stopIndexing` | Остановка индексации |
| `POST` | `/api/indexPage?url=...` | Индексация одной страницы |
| `POST` | `/api/indexSite?siteUrl=...` | Индексация одного сайта |
| `GET` | `/api/statistics` | Получение статистики |
| `GET` | `/api/search?query=...` | Поиск по проиндексированным данным |

**Формат ошибки:**
```json
{ "result": false, "error": "Сообщение об ошибке" }
```

---

## ⚙️ Конфигурация (`application.yaml`)

```yaml
server:
  port: 8080

spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/search_engine
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

indexing-settings:
  sites:
    - url: https://www.lenta.ru
      name: Лента.ру
    - url: https://www.skillbox.ru
      name: Skillbox
    - url: https://www.playback.ru
      name: PlayBack.Ru
    - url: https://www.svetlovka.ru/
      name: Svetlovka.ru
```

---

## 🧪 Сборка и запуск

Убедитесь, что установлены:
- JDK 17
- Maven 3
- PostgreSQL 14+

**Сборка:**
```bash
mvn clean install
```

**Запуск:**
```bash
java -jar target/search-engine-1.0.jar
```

**Веб-интерфейс:** [http://localhost:8080/admin](http://localhost:8080/admin)

---

## 🔐 Авторизация

| Логин | Пароль | Роль |
|-------|--------|------|
| `webAdmin` | `webPassword` | `ADMIN` |
| `searchUser` | `searchUserPass` | `SEARCH_API_USER` |

---

## 📋 Лицензия

MIT License — свободное использование и модификация.

---

## 🤝 Благодарности

Проект создан в рамках учебной программы. Спасибо всем, кто вдохновлял и помогал сделать его возможным!

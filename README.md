# java-explore-with-me-plus

## О проекте

Explore With Me — микросервисное приложение для публикации событий и подачи заявок на участие.

## Стек технологий

- Java 21
- Spring Boot 3
- Spring Cloud
- Spring Data JPA
- PostgreSQL
- Docker Compose
- OpenFeign
- Eureka Discovery Server
- Config Server
- Maven
- Lombok
- MapStruct

## Архитектура

Проект состоит из следующих сервисов:

- config-server
- discovery-server
- gateway-server
- user-service
- event-service
- request-service

Каждый сервис использует собственную базу данных PostgreSQL.

Взаимодействие между сервисами реализовано через OpenFeign и Eureka Discovery.

## Запуск проекта

Запуск Docker-контейнеров:

```bash
docker compose up -d
```

Запуск сервисов:

1. config-server
2. discovery-server
3. stats-server
4. user-service
5. event-service
6. request-service
7. gateway-server

## Основной функционал

### Пользователи

- создание пользователей;
- получение списка пользователей;
- удаление пользователей.

### События

- создание событий;
- обновление событий;
- публикация событий;
- поиск событий по фильтрам.

### Заявки

- создание заявки на участие;
- отмена заявки;
- подтверждение и отклонение заявок организатором.

### Статистика

- сохранение просмотров событий;
- получение статистики просмотров.
# java‑explore‑with‑me‑plus

## Feature: Комментарии

### Описание

Контроллер `PrivateUserCommentController` обрабатывает CRUD‑операции с комментариями зарегистрированных пользователей.

---

### Endpoints

### Private API (для зарегистрированных пользователей)

#### 1. Добавление комментария к событию

**Метод:** `POST`  
**Путь:** `/users/{userId}/event/{eventId}/comment`

**Описание:**  
Добавляет комментарий пользователя к указанному событию.

**Требования:**
- Пользователь должен быть зарегистрирован (`404 Not Found`, если нет).
- Событие должно существовать и быть в статусе `PUBLISHED` (`404 Not Found` для удалённых, `409 Conflict` для `PENDING`/`CANCELLED`).
- Текст комментария: **10–2000 символов** (`400 BAD REQUEST` при нарушении).
- Новый комментарий получает статус `PENDING` и ждет модерации администратором

**Request Body (JSON):**
```json
{
  "comment": "Текст коммента"
}
```
**Response Body (JSON, 201 Created):**
```json
{
  "id": 1,
  "author": {
    "id": 1,
    "name": "Author Name"
  },
  "eventId": 1,
  "comment": "Comment text",
  "createdOn": "2026-02-04 17:07:11",
  "state": "PENDING",
  "publishedOn": null
}
```

#### 2. Удаление комментария к событию

**Метод:** `DELETE`  
**Путь:** `/users/{userId}/comment/{commentId}`

**Описание:**  
Удаляет комментарий пользователя к указанному событию.

**Требования:**
- Пользователь должен быть зарегистрирован (`404 Not Found`, если нет).
- Событие должно существовать и быть в статусе `PUBLISHED` (`404 Not Found` для удалённых, `409 Conflict` для `PENDING`/`CANCELLED`).
- Пользователь должен быть автором удаляемого комментария (`409 Conflict`)

**Request Body (JSON):**
```json

```
**Response Body (JSON, 204 No Content):**
```json

```

#### 3. Обновление комментария к событию

**Метод:** `PATCH`  
**Путь:** `/users/{userId}/comment/{commentId}`

**Описание:**  
Редактирует комментарий пользователя к указанному событию.

**Требования:**
- Пользователь должен быть зарегистрирован (`404 Not Found`, если нет).
- Событие должно существовать и быть в статусе `PUBLISHED` (`404 Not Found` для удалённых, `409 Conflict` для `PENDING`/`CANCELLED`).
- Пользователь должен быть автором изменяемого комментария (`409 Conflict`)
- Текст комментария: **10–2000 символов** (`400 BAD REQUEST` при нарушении).
-  Обновленный комментарий получает статус `PENDING`, сбрасывается дата публикации и ждет модерации администратором

**Request Body (JSON):**
```json
{
  "comment": "Обновленный текст комментария"
}
```
**Response Body (JSON, 200 OK):**
```json
{
  "id": 1,
  "author": {
    "id": 1,
    "name": "Author Name"
  },
  "eventId": 1,
  "comment": "Обновленный текст комментария",
  "createdOn": "2026-02-04 17:07:11",
  "state": "PENDING",
  "publishedOn": null
}
```
#### 4. Получение комментария пользователя

**Метод:** `GET`  
**Путь:** `/users/{userId}/comment/{commentId}`

**Описание:**  
Находит комментарий пользователя по ID комментария.

**Требования:**
- Пользователь должен быть зарегистрирован (`404 Not Found`, если нет).
- Комментарий не должен быть удален (`404 Not Found`, если нет).
- Пользователь должен быть автором комментария (`409 Conflict`)

**Request Body (JSON):**
```json

```
**Response Body (JSON, 200 OK):**
```json
{
  "id": 1,
  "author": {
    "id": 1,
    "name": "Author Name"
  },
  "eventId": 1,
  "comment": "Обновленный текст комментария",
  "createdOn": "2026-02-04 17:07:11",
  "state": "PENDING",
  "publishedOn": null
}
```

#### 5. Получение комментариев пользователя

**Метод:** `GET`  
**Путь:** `/users/{userId}
**Параметры запроса:**
- `from` — смещение (номер элемента, с которого начинается выборка) по умолчанию `0`.
- `size` — количество элементов в ответе (размер страницы) по умолчанию `10`..

**Описание:**  
Находит комментарии пользователя автором которых он является с сортировкой по самым новым.

**Требования:**
- Пользователь должен быть зарегистрирован (`404 Not Found`, если нет).

**Request Body (JSON):**
```json

```
**Response Body (JSON, 200 OK):**
```json
[
  {
    "id": 2,
    "author": {
      "id": 1,
      "name": "Author Name"
    },
    "eventId": 2,
    "comment": "Новый комментарий",
    "createdOn": "2026-02-04 17:20:11",
    "state": "PENDING",
    "publishedOn": null
  },
  {
    "id": 1,
    "author": {
      "id": 1,
      "name": "Author Name"
    },
    "eventId": 1,
    "comment": "Обновленный текст комментария",
    "createdOn": "2026-02-04 17:07:11",
    "state": "PENDING",
    "publishedOn": null
  }
]
```
### Описание
Функциональность позволяет пользователям оставлять комментарии к опубликованным событиям. Комментарии проходят модерацию администратором.

### Жизненный цикл комментария
1. **Создание** → `PENDING` (ожидает модерации)
2. **Модерация администратором** → `PUBLISHED` (опубликован) или `REJECTED` (отклонен)
3. **Публичный доступ** → только `PUBLISHED` комментарии видны всем
4. **Редактирование автором** → снова переходит в `PENDING`

---

### Admin API (для администраторов)

#### 1. Публикация комментария

**Метод:** `PATCH`
**Путь:** `/admin/comment/{commentId}/publish`
**Описание:** Изменяет статус комментария на `PUBLISHED`
**Требования:** Комментарий должен быть в статусе `PENDING`

```json
{
    "id": 18,
    "author": {
        "id": 6,
        "name": "Lamar Lynch"
    },
    "eventId": 6,
    "comment": "Еще какой-то текст комментария",
    "createdOn": "2026-02-07 11:15:35",
    "state": "PUBLISHED",
    "publishedOn": "2026-02-07 11:16:30"
}
```

#### 2. Отклонение комментария  
**Метод:** `PATCH`
**Путь:** `/admin/comment/{commentId}/reject`
**Описание:** Изменяет статус комментария на `REJECTED`
**Требования:** Комментарий должен быть в статусе `PENDING`

```json
{
    "id": 17,
    "author": {
        "id": 3,
        "name": "Roland Pacocha DVM"
    },
    "eventId": 7,
    "comment": "Какой-то текст комментария",
    "createdOn": "2026-02-05 15:39:28",
    "state": "REJECTED",
    "publishedOn": null
}
```

#### 3. Поиск комментариев
**Метод:** `GET`
**Путь:** `/admin/comment`
**Параметры:** `userIds`, `eventIds`, `states`, `rangeStart`, `rangeEnd`, `from`, `size`

```json
[
    {
        "id": 17,
        "author": {
            "id": 3,
            "name": "Roland Pacocha DVM"
        },
        "eventId": 7,
        "comment": "Какойто текст комментария",
        "createdOn": "2026-02-05 15:39:28",
        "state": "REJECTED",
        "publishedOn": null
    },
    {
        "id": 16,
        "author": {
            "id": 3,
            "name": "Roland Pacocha DVM"
        },
        "eventId": 6,
        "comment": "ОБНОВЛЕНО новый коммент для события",
        "createdOn": "2026-02-05 15:17:55",
        "state": "PENDING",
        "publishedOn": null
    },
    {
        "id": 15,
        "author": {
            "id": 3,
            "name": "Roland Pacocha DVM"
        },
        "eventId": 6,
        "comment": "Комментарий события",
        "createdOn": "2026-02-05 15:17:27",
        "state": "PENDING",
        "publishedOn": null
    }
]

```
### Public API (публичный доступ)

#### 1. Получение комментариев события
**Метод:** `GET`
**Путь:** `/events/{eventId}/comments`
**Требования:** Событие должно быть в статусе `PUBLISHED`

```json
[
{
"id": 15,
"author": {
"id": 3,
"name": "Roland Pacocha DVM"
},
"eventId": 6,
"comment": "Комментарий события",
"publishedOn": "2026-02-07 11:30:57"
},
{
"id": 18,
"author": {
"id": 6,
"name": "Lamar Lynch"
},
"eventId": 6,
"comment": "Еще какойто текст комментария",
"publishedOn": "2026-02-07 11:16:30"
}
]
```

#### 2. Получение комментария по ID
**Метод:** `GET`
**Путь:** `/events/{eventId}/comments/{commentId}`
**Требования:** Событие должно быть в статусе `PUBLISHED`

```json
{
    "id": 18,
    "author": {
        "id": 6,
        "name": "Lamar Lynch"
    },
    "eventId": 6,
    "comment": "Еще какойто текст комментария",
    "publishedOn": "2026-02-07 11:16:30"
}
```
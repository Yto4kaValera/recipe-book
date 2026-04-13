# Recipe Book

## Быстрый запуск

1. Запустите backend:

```cmd
start-backend.cmd
```

2. В отдельном окне запустите frontend:

```cmd
start-frontend.cmd
```

3. Откройте в браузере:

```text
http://localhost:5173
```

## Что важно

- Backend запускается на `http://localhost:8080`
- Frontend запускается на `http://localhost:5173`
- Запросы с frontend на backend идут через Vite proxy по пути `/api`
- Данные сохраняются в файл `D:\university\testing2\data\db.json`

## Альтернативные команды

Backend:

```cmd
cd backend-java
mvnw.cmd spring-boot:run
```

Frontend:

```cmd
cd frontend
npm.cmd run dev
```

## Проверка backend

После запуска backend можно открыть:

```text
http://localhost:8080/api/health
```

Должен вернуться ответ:

```json
{"status":"ok"}
```

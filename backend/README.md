# Task Manager Backend

Spring Boot REST API for the Android Task Manager app.

## Requirements

- Java 17 or newer
- PostgreSQL running on `localhost:5432`
- Database user configured in `src/main/resources/application.properties`

Default config:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/tasks_manager_db
spring.datasource.username=postgres
spring.datasource.password=123456
```

Create the database before running the backend:

```sql
CREATE DATABASE tasks_manager_db;
```

You can override the local values with environment variables:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/tasks_manager_db'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='123456'
```

## Run

From the backend directory:

```powershell
cd backend
$env:JAVA_HOME='D:\Android\Android Studio\jbr'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat bootRun
```

The API runs at:

```text
http://localhost:8080/api/tasks
```

Android Emulator should call it through:

```text
http://192.168.1.3:8080/api/tasks
```

## Endpoints

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/tasks` requires `Authorization: Bearer <token>`
- `GET /api/tasks/{id}` requires `Authorization: Bearer <token>`
- `POST /api/tasks` requires `Authorization: Bearer <token>`
- `PUT /api/tasks/{id}` requires `Authorization: Bearer <token>`
- `DELETE /api/tasks/{id}` requires `Authorization: Bearer <token>`

## Auth payloads

Register:

```json
{
  "username": "demo",
  "email": "demo@example.com",
  "password": "123456"
}
```

Login accepts username or email in `identifier`:

```json
{
  "identifier": "demo",
  "password": "123456"
}
```

Auth responses include the user profile and JWT:

```json
{
  "id": "...",
  "username": "demo",
  "email": "demo@example.com",
  "createdAt": 123456789,
  "token": "...",
  "tokenType": "Bearer",
  "expiresAt": 123456789
}
```

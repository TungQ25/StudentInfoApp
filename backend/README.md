# Task Manager Backend

Spring Boot REST API for the Android Task Manager app.

## Requirements

- Java 17 or newer
- MySQL running on `localhost:3306`
- Database user configured in `src/main/resources/application.properties`

Default config:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/tasks_manager_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=
```

Update `spring.datasource.password` if your MySQL root account has a password.

## Run

From the repo root:

```powershell
$env:JAVA_HOME='D:\Android\Android Studio\jbr'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :backend:bootRun
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

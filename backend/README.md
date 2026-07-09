# Task Manager Backend

Spring Boot REST API for the Android Task Manager app.

## Requirements

- Java 17 or newer
- PostgreSQL
- Database credentials provided through environment variables

Create the database before running the backend:

```sql
CREATE DATABASE tasks_manager_db;
```

Local environment example:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/tasks_manager_db'
$env:SPRING_DATASOURCE_USERNAME='<db-user>'
$env:SPRING_DATASOURCE_PASSWORD='<db-password>'
$env:JWT_SECRET='<long-random-secret>'
$env:APP_CORS_ALLOWED_ORIGIN_PATTERNS='http://localhost:3000,http://localhost:8080'
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
http://localhost:8080
```

## Verify

```powershell
.\gradlew.bat test
.\gradlew.bat bootJar
```

## API Contract

The current REST contract is documented in:

```text
backend/openapi.yaml
```

Main endpoint groups:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `POST /api/auth/logout-all`
- `/api/tasks`
- `/api/categories`
- `/api/habits`
- `/api/habit-completions`

Authenticated endpoints require:

```text
Authorization: Bearer <access-token>
```

## Schema

During development, Hibernate manages schema changes automatically:

```properties
spring.jpa.hibernate.ddl-auto=${SPRING_JPA_HIBERNATE_DDL_AUTO:update}
```

If the dev database drifts, recreate the PostgreSQL database or point the backend at a fresh one.

## Production

See:

```text
backend/DEPLOYMENT.md
```

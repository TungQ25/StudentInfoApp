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
http://10.0.2.2:8080/api/tasks
```

## Endpoints

- `GET /api/tasks`
- `GET /api/tasks/{id}`
- `POST /api/tasks`
- `PUT /api/tasks/{id}`
- `DELETE /api/tasks/{id}`

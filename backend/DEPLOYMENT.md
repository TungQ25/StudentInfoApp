# Backend Deployment

Recommended baseline for production:

- Run the Spring Boot backend as a Docker container.
- Use managed PostgreSQL for the database.
- Store secrets in the host/cloud secret manager, not in files.
- Use Hibernate auto schema while the app is still in development: `SPRING_JPA_HIBERNATE_DDL_AUTO=update`.
- Set CORS explicitly with `APP_CORS_ALLOWED_ORIGIN_PATTERNS`.

## Required Environment

```text
PORT=8080
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/<database>
SPRING_DATASOURCE_USERNAME=<db-user>
SPRING_DATASOURCE_PASSWORD=<db-password>
JWT_SECRET=<long-random-secret>
JWT_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=2592000000
APP_CORS_ALLOWED_ORIGIN_PATTERNS=https://app.example.com
SERVER_ERROR_INCLUDE_MESSAGE=never
LOGGING_LEVEL_APP=INFO
LOGGING_LEVEL_SECURITY=INFO
```

No database migration files are used right now. If the development schema drifts, recreate the PostgreSQL database or point the service to a fresh database.

## Verification

```powershell
.\gradlew.bat test
.\gradlew.bat bootJar
```

After deploy:

- Confirm `/api/auth/login` returns access and refresh tokens.
- Confirm authenticated endpoints reject missing or expired access tokens.
- Confirm CORS denies unexpected origins.
- Confirm Hibernate created or updated the expected tables in the database.

# AURA IA Backend

Spring Boot backend for the AURA IA user panel. It owns authentication, PostgreSQL persistence, user data, and the HTTP client boundary for the future Python AI service.

## Requirements

- JDK 21
- Docker, only for Testcontainers integration tests
- Supabase PostgreSQL connection string for dev/prod runtime

This repository includes Maven Wrapper scripts. The wrapper downloads its own Maven runtime the first time it runs, but Java 21 still needs to be installed and available through `JAVA_HOME` or `PATH`.

## Configuration

Copy `.env.example` to `.env` in your local environment manager or export the variables before running. Never commit real credentials.

Important variables:

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET` as a base64 secret of at least 64 bytes decoded
- `FRONTEND_BASE_URL`, used for email verification links
- `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM`
- `AI_SERVICE_URL`, `AI_SERVICE_ENABLED`, `AI_SERVICE_TIMEOUT_MS`
- `ADMIN_EMAILS`, comma-separated emails promoted to admin

## Run

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

Healthcheck:

```text
GET /actuator/health
```

## Auth Flow

1. `POST /api/v1/auth/register` creates the user and returns `202 Accepted`.
2. A verification email is sent to `${FRONTEND_BASE_URL}/verify-email?token=...`.
3. `POST /api/v1/auth/verify-email?token=...` verifies the account.
4. `POST /api/v1/auth/login` returns `accessToken` and `refreshToken`.
5. `POST /api/v1/auth/refresh` rotates the refresh token.
6. `POST /api/v1/auth/logout` revokes the current refresh token.

Users cannot log in until email is verified. Passwords require 12 characters with uppercase, lowercase, number, and symbol.

## AI Service Contract

The backend uses Spring `RestClient`. When `AI_SERVICE_ENABLED=false`, or when the service is unavailable, it returns deterministic Spanish mock responses.

Future Python service endpoints:

```http
POST /analyze
Content-Type: application/json

{ "text": "..." }
```

```json
{ "sentiment": "negative", "score": 0.82, "emotions": ["anxiety", "sadness"] }
```

```http
POST /chat
Content-Type: application/json

{ "history": [], "message": "..." }
```

```json
{
  "reply": "Estoy contigo...",
  "sentiment": "supportive",
  "riskLevel": "low",
  "emotions": ["anxiety"]
}
```

Crisis/autolesion keywords in mock mode return a safety response referencing Spain `112` and `024`.

## Tests

```bash
./mvnw test
```

Integration tests use Testcontainers PostgreSQL.

## Frontend Contract Notes

The backend intentionally uses the secure contract rather than the old localStorage mock contract. The frontend should update:

- Register now returns pending verification, not tokens.
- Demo password must be strong if seeded from backend.
- Mood uses `beforeLevel` and `afterLevel` from 1 to 10.
- API JSON field names are stable English; messages are localized through `Accept-Language`.

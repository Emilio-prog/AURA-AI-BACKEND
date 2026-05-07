# AURA IA Backend

Spring Boot backend for the AURA IA user panel. It owns authentication, PostgreSQL persistence, user data, and the HTTP client boundary for the future Python AI service.

## Requirements

- JDK 21
- Docker, only for Testcontainers integration tests
- Supabase PostgreSQL connection string for dev/prod runtime

This repository includes Maven Wrapper scripts. The wrapper downloads its own Maven runtime the first time it runs. On this workstation it auto-detects the installed JDK 21 under `C:\Users\Usuario\.jdks\ms-21.0.10`.

## Configuration

Copy `.env.example` to `.env` in your local environment manager or export the variables before running. Never commit real credentials.

Important variables:

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET` as a base64 secret of at least 64 bytes decoded
- `FRONTEND_BASE_URL`, used for email verification links
- `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM`
- `EMAIL_ENABLED=true` and `EMAIL_AUTO_VERIFY_WHEN_DISABLED=false` for real email verification
- `AI_SERVICE_URL`, `AI_SERVICE_ENABLED`, `AI_SERVICE_TIMEOUT_MS`
- `ADMIN_EMAILS`, comma-separated emails promoted to admin

The Supabase project currently used for dev is `AURA-AI` (`aexcwfxhbiifvcxdgcxm`). Local development uses the Supabase Session pooler because the direct DB host is IPv6-only in this project:

```text
jdbc:postgresql://aws-1-eu-west-2.pooler.supabase.com:5432/postgres?sslmode=require
username: postgres.aexcwfxhbiifvcxdgcxm
```

The public backend schema was applied to Supabase through MCP as migration `init_aura_backend_schema`. Flyway profiles use `baseline-on-migrate=true` so an already-migrated Supabase schema is not recreated at runtime.

### Resend SMTP

The recommended email provider is Resend through Spring Mail SMTP. Resend requires a verified sending domain before external recipients can receive verification emails.

```text
SMTP_HOST=smtp.resend.com
SMTP_PORT=587
SMTP_USERNAME=resend
SMTP_PASSWORD=<resend-api-key>
SMTP_AUTH=true
SMTP_STARTTLS=true
SMTP_FROM=AURA IA <no-reply@aura-ia.es>
```

Resend MCP is authenticated by API key. The configured sending domain is `aura-ia.es`; it must be `verified` in Resend before `EMAIL_ENABLED=true` is used in local or production runtime.

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
2. In prod, a verification email is sent to `${FRONTEND_BASE_URL}/#/verify-email?token=...` for the current HashRouter frontend.
3. In dev, if `EMAIL_ENABLED=false` and `EMAIL_AUTO_VERIFY_WHEN_DISABLED=true`, the account is verified immediately.
4. `POST /api/v1/auth/verify-email?token=...` verifies the account when email verification is active.
5. `POST /api/v1/auth/login` returns `accessToken` and `refreshToken`.
6. `POST /api/v1/auth/refresh` rotates the refresh token.
7. `POST /api/v1/auth/logout` revokes the current refresh token.

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

Integration tests use Testcontainers PostgreSQL and are opt-in so regular builds do not fail on machines where Docker Desktop is not running:

```bash
./mvnw test -Daura.integration-tests=true
```

## Frontend Contract Notes

The backend intentionally uses the secure contract rather than the old localStorage mock contract. The frontend should update:

- Register now returns pending verification, not tokens.
- Demo password must be strong if seeded from backend.
- Mood uses `beforeLevel` and `afterLevel` from 1 to 10.
- API JSON field names are stable English; messages are localized through `Accept-Language`.

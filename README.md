# AURA IA Backend

Spring Boot backend for the AURA IA user panel. It owns authentication, PostgreSQL persistence, user data, billing, and the Gemini AI integration.

## Requirements

- JDK 21
- Docker, only for Testcontainers integration tests
- Supabase PostgreSQL connection string for dev/prod runtime

This repository includes Maven Wrapper scripts. The wrapper downloads its own Maven runtime the first time it runs. The machine must expose JDK 21 through `JAVA_HOME` or `PATH`.

## Instalacion local limpia para evaluador

AURA IA se entrega en dos repositorios Git independientes. Para levantar la
aplicacion completa con los scripts versionados, clona backend y frontend como
carpetas hermanas dentro de una misma carpeta de trabajo:

```text
AURA-IA/
|-- AURA-AI-BACKEND/
`-- AURA-AI-FRONTEND/
```

Clonado recomendado:

```bash
mkdir AURA-IA
cd AURA-IA
git clone https://github.com/Emilio-prog/AURA-AI-BACKEND.git
git clone https://github.com/Emilio-prog/AURA-AI-FRONTEND.git
```

Requisitos minimos:

- JDK 21. Esta version esta fijada en `pom.xml` mediante `<java.version>21</java.version>`.
- Node.js 20 o superior para el frontend.
- PostgreSQL accesible. Puede ser Supabase o una base local si se ajustan las
  variables `SPRING_DATASOURCE_*`.

Archivos locales a crear despues del clonado:

```bash
cp AURA-AI-BACKEND/.env.example AURA-AI-BACKEND/.env
cp AURA-AI-FRONTEND/.env.example AURA-AI-FRONTEND/.env.local
```

En Windows PowerShell:

```powershell
Copy-Item AURA-AI-BACKEND\.env.example AURA-AI-BACKEND\.env
Copy-Item AURA-AI-FRONTEND\.env.example AURA-AI-FRONTEND\.env.local
```

Los archivos reales `.env` y `.env.local` no se suben a GitHub porque pueden
contener credenciales. El evaluador debe rellenar la conexion PostgreSQL y puede
dejar desactivadas las integraciones opcionales para arrancar en local:
`AI_SERVICE_ENABLED=false`, `TURNSTILE_ENABLED=false`, `GOOGLE_OAUTH_ENABLED=false`,
`WEB_PUSH_ENABLED=false` y `SOS_SMS_ENABLED=false`.

Si el script `start-dev.ps1` se ejecuta en un clon limpio sin `.env`, copiara
los `.env.example` necesarios y se detendra con un mensaje indicando que falta
rellenar la configuracion del backend.

Arranque completo desde la carpeta `AURA-IA`:

```powershell
.\AURA-AI-FRONTEND\scripts\start-dev.ps1
```

```bash
chmod +x AURA-AI-FRONTEND/scripts/start-dev.sh
./AURA-AI-FRONTEND/scripts/start-dev.sh
```

Verificacion rapida:

- El navegador abre `http://localhost:5173`.
- El backend responde `GET http://localhost:8080/actuator/health` con
  `{"status":"UP"}`.
- Swagger local queda disponible en `http://localhost:8080/swagger-ui.html`.

## Configuration

Copy `.env.example` to `.env` in your local environment manager or export the variables before running. Never commit real credentials.

Important variables:

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET` as a base64 secret of at least 64 bytes decoded
- `FRONTEND_BASE_URL`, used for email verification links
- `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM`
- `EMAIL_ENABLED=true` and `EMAIL_AUTO_VERIFY_WHEN_DISABLED=false` for real email verification
- `AI_SERVICE_ENABLED`, `GEMINI_API_KEY`, `GEMINI_MODEL`, `AI_MAX_HISTORY_MESSAGES`
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

Verification emails are sent as HTML with a plain-text fallback. The HTML template includes the AURA IA brand block, a primary verification button, and a short security note.

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

### Full local dev stack

From the workspace root `AURA-IA`, the full development stack can be started with one command.

Windows:

```powershell
.\AURA-AI-FRONTEND\scripts\start-dev.ps1
```

macOS/Linux:

```bash
chmod +x AURA-AI-FRONTEND/scripts/start-dev.sh
./AURA-AI-FRONTEND/scripts/start-dev.sh
```

The scripts start the backend at `http://localhost:8080`, the frontend at `http://localhost:5173`, wait for both services, and open the browser at `http://localhost:5173`.

If `AURA-AI-BACKEND/.env` defines `SERVER_PORT`, the scripts use that real backend port and print a warning. Keep the frontend local env and OAuth callback aligned with it:

```text
VITE_API_BASE_URL=http://localhost:<SERVER_PORT>/api/v1
GOOGLE_OAUTH_REDIRECT_URI=http://localhost:<SERVER_PORT>/api/v1/auth/oauth/google/callback
```

To stop the local stack:

```powershell
.\AURA-AI-FRONTEND\scripts\start-dev.ps1 -Stop
```

```bash
./AURA-AI-FRONTEND/scripts/start-dev.sh stop
```

### Verification after the changes

- Browser URL: `http://localhost:5173`.
- Backend health: `GET http://localhost:8080/actuator/health` returns `{"status":"UP"}`. If `SERVER_PORT` is set in `.env`, use that port instead.
- Frontend dev server: Vite logs `Local: http://localhost:5173/`.
- API calls from the browser target `http://localhost:<SERVER_PORT>/api/v1`.
- If API calls still target `127.0.0.1`, check `AURA-AI-FRONTEND/.env.local` and set `VITE_API_BASE_URL=http://localhost:<SERVER_PORT>/api/v1`.
- On Windows, inspect `.dev-logs/backend-dev.err.log`, `.dev-logs/backend-dev.out.log`, `.dev-logs/frontend-dev.err.log`, and `.dev-logs/frontend-dev.out.log`.
- On macOS/Linux, inspect `.dev-logs/backend-dev.log` and `.dev-logs/frontend-dev.log`.
- If Google OAuth is enabled locally, `GOOGLE_OAUTH_REDIRECT_URI` must match the backend URL, for example `http://localhost:8080/api/v1/auth/oauth/google/callback`.

## Version Control

Backend changes follow GitFlow: work starts in `feature`, integrates into `develop`,
is stabilized in `release`, and reaches `main` only through a tagged release.
Critical production fixes use `hotfix` and are merged back into `develop`.

### Stripe Billing Webhooks

Checkout can open from local development, but Stripe cannot call `localhost` directly after payment. To make plan changes sync locally on Windows, start the full dev stack from the workspace root `AURA-IA` with:

```powershell
.\AURA-AI-FRONTEND\scripts\start-dev.ps1 -StripeWebhook
```

That command reads `STRIPE_SECRET_KEY`, obtains the local Stripe CLI webhook signing secret, injects it into the backend process as `STRIPE_WEBHOOK_SECRET`, and forwards Stripe events to:

```text
http://localhost:8080/api/v1/webhooks/stripe
```

If Stripe CLI is not installed, the script falls back to Docker Desktop and runs the official `stripe/stripe-cli` image. Stop everything with:

```powershell
.\AURA-AI-FRONTEND\scripts\start-dev.ps1 -Stop
```

Production must use a real public backend URL, currently expected as:

```text
https://api.aura-ia.es/api/v1/webhooks/stripe
```

Use the Dashboard webhook signing secret for production/Dokploy, and the local listener secret only for local runs.

Production deployment details are tracked in [`docs/stripe-production.md`](docs/stripe-production.md).

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

The backend integrates Gemini directly from Spring Boot through `RestClient`; there is no Python/FastAPI service in the current architecture. Runtime behavior is controlled by:

```text
AI_SERVICE_ENABLED=true
GEMINI_API_KEY=<google-ai-studio-api-key>
GEMINI_MODEL=gemini-flash-latest
AI_MAX_HISTORY_MESSAGES=12
AI_CHAT_RATE_LIMIT_CAPACITY=20
AI_CHAT_RATE_LIMIT_REFILL_MINUTES=5
```

When `AI_SERVICE_ENABLED=false`, `GEMINI_API_KEY` is missing, Gemini is unavailable, or Gemini returns an invalid response, the backend returns a deterministic safe fallback instead of breaking the chat.

Authenticated chat endpoints:

```http
GET /api/v1/chatbot/sessions
POST /api/v1/chatbot/sessions
GET /api/v1/chatbot/sessions/{id}
POST /api/v1/chatbot/sessions/{id}/messages
DELETE /api/v1/chatbot/sessions/{id}
```

Message request:

```json
{ "message": "Estoy nervioso hoy" }
```

Session response:

```json
{
  "id": "e5c679e7-366c-404a-a4ff-2c92992fd464",
  "title": "Estoy nervioso hoy",
  "messages": [
    {
      "role": "user",
      "content": "Estoy nervioso hoy",
      "timestamp": "2026-05-12T09:06:24.790608500Z"
    },
    {
      "role": "assistant",
      "content": "Estoy contigo. Probemos una respiracion breve...",
      "timestamp": "2026-05-12T09:06:28.286856700Z",
      "riskLevel": "low",
      "emotions": ["anxiety"],
      "sentiment": "supportive"
    }
  ],
  "startedAt": "2026-05-12T09:06:24.548620Z",
  "updatedAt": "2026-05-12T09:06:28.286856700Z"
}
```

Only the current message and the most recent messages from the same chat session are sent to Gemini. Diary entries, mood logs, onboarding data, contacts, billing data, and user profile details are not sent to Gemini.

Safety rules are enforced before and after Gemini. Messages involving suicidal ideation, self-harm, harm to others, or immediate danger return a high-risk safety response that references Spain `112` and `024`. Normal anxiety, panic, sleep, or calming requests receive grounding and breathing support without emergency numbers.

## Tests

```bash
./mvnw test
```

Integration tests use Testcontainers PostgreSQL and are opt-in so regular builds do not fail on machines where Docker Desktop is not running:

```bash
./mvnw test -Daura.integration-tests=true
```

## Frontend Contract Notes

The backend exposes the secure contract consumed by the current frontend:

- Register returns pending verification, not tokens.
- Demo password must be strong if seeded from backend.
- Mood uses `beforeLevel` and `afterLevel` from 1 to 10.
- API JSON field names are stable English; messages are localized through `Accept-Language`.

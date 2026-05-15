# AURA IA operations runbook

## Production surfaces

- Frontend: `https://aura-ia.es`
- Frontend alias: `https://www.aura-ia.es` redirects permanently to the apex domain.
- Backend API: `https://api.aura-ia.es`
- Backend health: `https://api.aura-ia.es/actuator/health`
- Backend image: `ghcr.io/emilio-prog/aura-ai-backend`
- Frontend image: `ghcr.io/emilio-prog/aura-ai-frontend`
- Database: Supabase PostgreSQL. Do not deploy a database container in Dokploy.
- Dokploy panel: the self-hosted Dokploy instance installed on the Hostinger VPS.

## Deployment model

Both applications are Docker Compose apps in Dokploy under the `aura-ia` project. GitHub Actions builds and pushes immutable images to GHCR, then calls the Dokploy API endpoint `POST /api/compose.deploy`. Dokploy only pulls and recreates containers; it does not build from source.

The production source of truth is the self-hosted Dokploy instance installed on the Hostinger VPS. Do not use Dokploy Cloud, old Dokploy Cloud server entries, or compose IDs from a removed SSH connection. If the Dokploy MCP/API shows a deleted Cloud server, ignore it for production and reconfigure the integration against the Hostinger VPS Dokploy instance.

GitHub Actions requires these repository secrets:

- Backend repository: `DOKPLOY_API_URL`, `DOKPLOY_API_KEY`, `DOKPLOY_BACKEND_COMPOSE_ID`.
- Frontend repository: `DOKPLOY_API_URL`, `DOKPLOY_API_KEY`, `DOKPLOY_FRONTEND_COMPOSE_ID`, `VITE_TURNSTILE_SITE_KEY`.

These values must come from the Hostinger VPS Dokploy instance:

- `DOKPLOY_API_URL`: public URL of the self-hosted Hostinger Dokploy panel, without a trailing slash.
- `DOKPLOY_API_KEY`: API key generated inside the self-hosted Hostinger Dokploy panel.
- `DOKPLOY_BACKEND_COMPOSE_ID`: compose ID for the `aura-backend` compose in the self-hosted Hostinger Dokploy panel.
- `DOKPLOY_FRONTEND_COMPOSE_ID`: compose ID for the `aura-frontend` compose in the self-hosted Hostinger Dokploy panel.

Do not copy compose IDs, server IDs, API keys or webhook values from Dokploy Cloud. A successful GitHub Actions `curl` only proves that an API accepted the request; it does not prove that the active Hostinger VPS instance was targeted unless these secrets point to the self-hosted Dokploy URL.

Runtime secrets are stored only in Dokploy and GitHub Actions secrets. Do not commit `.env`, `.env.*`, `*.env`, `.mcp.json`, webhook URLs, API keys, JWT secrets, encryption keys or VAPID private keys.

## Rollback

1. Open the affected Dokploy app.
2. Change the image tag from `latest` to a previous immutable tag, such as `sha-<short>` or `vX.Y.Z`.
3. Redeploy the app.
4. Verify health and logs.

Target rollback time: under 5 minutes after the previous image tag is known.

## Logs

- Use Dokploy UI or MCP logs for the affected app.
- Backend logs should remain at INFO for `com.auraia.backend` and WARN for noisy framework/security logs.
- Do not log diary entries, chatbot messages, SOS contacts, OAuth tokens, webhook signatures, email recipients beyond operational event records, or other PII.

## Manual restart

Use Dokploy UI/MCP restart or redeploy after changing environment variables.

- Backend: restart/redeploy is enough after env changes.
- Frontend: Vite variables are build-time values. Changing `VITE_API_BASE_URL`, `VITE_DEFAULT_LOCALE`, `VITE_TURNSTILE_SITE_KEY` or `VITE_DEV_MODE` requires a new GitHub Actions release build.

## Forced redeploy

Run the corresponding GitHub Actions release workflow manually on `main` or call `POST /api/compose.deploy` for the app after confirming the desired image tag exists in GHCR.

## Secret rotation

- `JWT_SECRET`: rotating invalidates active JWT sessions and refresh tokens effectively become unusable once new tokens cannot be validated. Plan a user re-login window.
- `CONTENT_ENCRYPTION_KEY`: do not rotate casually. Existing encrypted content depends on this key. A safe rotation requires a migration/re-encryption plan.
- `WEB_PUSH_VAPID_PRIVATE_KEY`: rotating requires users/browsers to resubscribe to push notifications.
- `SMTP_PASSWORD` / Resend API key: rotate in Resend, update Dokploy, restart backend, then test email verification.
- `RESEND_WEBHOOK_SECRET`: update the Resend webhook signing secret and Dokploy together.
- `STRIPE_SECRET_KEY` and `STRIPE_WEBHOOK_SECRET`: update Stripe/Dokploy together, then send a test webhook event.
- `GOOGLE_OAUTH_CLIENT_SECRET`: update Google Cloud and Dokploy, then test login/link flow.
- `TURNSTILE_SECRET_KEY`: update Cloudflare and Dokploy, then test registration.
- `TWILIO_AUTH_TOKEN`: currently unused because `SOS_SMS_ENABLED=false`; if enabled later, rotate in Twilio and Dokploy.

## Supabase backups

Backups are managed by Supabase according to the active Supabase plan. Before production launch, confirm the retention period and restore procedure in the Supabase dashboard.

## Flyway migrations

Flyway runs on backend startup.

- Never modify or rename an applied migration.
- The current repository intentionally keeps `V14__google_oauth.sql` and documents the missing `V13` history in `docs/flyway-policy.md`.
- The next migration must be `V15__<description>.sql`.
- Keep `SPRING_FLYWAY_OUT_OF_ORDER=false` in production.
- Do not introduce another version gap.

## Production smoke checks

1. `curl -fsS https://api.aura-ia.es/actuator/health`
2. `curl -fsSI https://aura-ia.es`
3. Check TLS certificate dates for `aura-ia.es`, `www.aura-ia.es`, `api.aura-ia.es`.
4. Confirm backend logs have no Flyway, Supabase, encryption or missing-secret failures.
5. Register a user, verify email, log in.
6. Create a diary entry.
7. Send a chatbot message.
8. Trigger Stripe and Resend webhook tests.
9. Register Web Push subscription and send a test notification.
10. Confirm CORS from `https://aura-ia.es` to `https://api.aura-ia.es/api/v1/*`.

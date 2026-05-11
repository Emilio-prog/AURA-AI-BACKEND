# Stripe Production Checklist

This backend uses Stripe Checkout for subscriptions, Stripe Customer Portal for self-service billing, and webhooks to keep `users.plan` in sync after payment events.

Checkout success URLs include Stripe's `{CHECKOUT_SESSION_ID}` placeholder. When the user returns to `/#/dashboard/billing?checkout=success&session_id=...`, the frontend calls:

```text
POST /api/v1/billing/checkout/sync
```

This authenticated sync retrieves the Checkout Session from Stripe and refreshes the subscription immediately. Webhooks remain the canonical source of truth for renewals, cancellations, failed payments, and delayed updates.

## 1. Stripe Live Setup

Create live Stripe objects separately from test objects. Test prices and webhook endpoints cannot be reused with live keys.

Products and prices:

- `AURA IA Personal`: EUR 6.99 monthly recurring price.
- `AURA IA Premium`: EUR 12.00 monthly recurring price.

Save the live price IDs:

```text
STRIPE_PRICE_ID_PERSONAL=price_live_personal
STRIPE_PRICE_ID_PREMIUM=price_live_premium
```

Customer Portal:

- Enable the Customer Portal in live mode.
- Allow customers to update payment methods.
- Allow invoice/receipt viewing.
- Allow cancellation according to the product policy.
- If plan switching is enabled, include only the live Personal and Premium prices.

Save the live portal configuration ID:

```text
STRIPE_PORTAL_CONFIGURATION_ID=bpc_live_configuration
```

Webhook endpoint:

```text
https://api.aura-ia.es/api/v1/webhooks/stripe
```

Subscribed events:

```text
checkout.session.completed
customer.subscription.created
customer.subscription.updated
customer.subscription.deleted
invoice.paid
invoice.payment_failed
```

Save the live webhook signing secret:

```text
STRIPE_WEBHOOK_SECRET=whsec_live_endpoint_secret
```

## 2. Dokploy Backend Environment

Use real production values in Dokploy. Do not commit them.

The repository includes production helpers:

```text
docker-compose.dokploy.backend.yml
dokploy.backend.env.example
```

In Dokploy, create a Docker Compose project from `docker-compose.dokploy.backend.yml`, add the environment variables from `dokploy.backend.env.example`, then configure the service domain through the Domains tab:

```text
Host: api.aura-ia.es
Container port: 8080
HTTPS: enabled
```

```text
SPRING_PROFILES_ACTIVE=prod
FRONTEND_BASE_URL=https://aura-ia.es
CORS_ALLOWED_ORIGINS=https://aura-ia.es

STRIPE_SECRET_KEY=sk_live_xxx
STRIPE_WEBHOOK_SECRET=whsec_live_xxx
STRIPE_PRICE_ID_PERSONAL=price_live_personal
STRIPE_PRICE_ID_PREMIUM=price_live_premium
STRIPE_PORTAL_CONFIGURATION_ID=bpc_live_configuration
```

Temporary public test deployments can use `sk_test_...`, test prices, and a test webhook endpoint, but real customers cannot complete real payments in Stripe test mode.

## 3. Required Runtime Order

1. Deploy backend first and verify health:

```text
GET https://api.aura-ia.es/actuator/health
```

2. Create or update the Stripe webhook endpoint to the deployed backend URL.
3. Put the matching `STRIPE_WEBHOOK_SECRET` in Dokploy.
4. Restart the backend after changing Stripe secrets.
5. Deploy frontend with `VITE_API_BASE_URL` pointing to the production backend if the frontend build uses that variable.

## 4. Production Smoke Test

Before opening billing to users:

1. Register/login with a real test account in production.
2. Open `/#/dashboard/billing`.
3. Confirm the page shows `STRIPE_BILLING_LIVE_MODE` when live keys are configured.
4. Start Checkout for Personal.
5. Complete payment.
6. Confirm Stripe Dashboard shows the subscription as active.
7. Confirm the webhook delivery returned `2xx`.
8. Refresh AURA and confirm the user plan is `PERSONAL`.
9. Open the Customer Portal and confirm it returns to `/#/dashboard/billing`.
10. Cancel or downgrade the subscription in the portal and confirm the webhook updates AURA.

## 5. Local Webhook Testing

Stripe cannot call `localhost` directly. For local end-to-end subscription sync, run from the repository root:

```powershell
.\start-dev.ps1 -StripeWebhook
```

The local listener generates its own `whsec_...`. That secret is only for the local listener and is different from the Dashboard webhook secret used in Dokploy.

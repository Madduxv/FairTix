# Stripe Setup Guide

## Prerequisites

- A Stripe account at [dashboard.stripe.com](https://dashboard.stripe.com)
- Stripe CLI installed locally for webhook testing

## Keys

Stripe provides two sets of keys — test and live:

| Type | Prefix | When to use |
|------|--------|-------------|
| Publishable (test) | `pk_test_` | Frontend, local dev |
| Secret (test) | `sk_test_` | Backend, local dev |
| Publishable (live) | `pk_live_` | Frontend, production |
| Secret (live) | `sk_live_` | Backend, production |

Find your keys in **Stripe Dashboard → Developers → API keys**.

## Environment Variables

### Backend (`.env`)

```
STRIPE_ENABLED=true
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

### Frontend (`frontend/webpages/.env`)

```
REACT_APP_STRIPE_PUBLISHABLE_KEY=pk_test_...
```

## Local Webhook Testing

Stripe cannot reach `localhost` directly. Use the Stripe CLI to forward events:

```bash
stripe listen --forward-to localhost:8080/api/webhooks/stripe
```

The CLI prints a webhook signing secret on startup:

```
> Ready! Your webhook signing secret is whsec_abc123... (^C to quit)
```

Set `STRIPE_WEBHOOK_SECRET=whsec_abc123...` in your `.env` and restart the backend.

## Generating a Webhook Secret for Production

In **Stripe Dashboard → Developers → Webhooks**, add an endpoint pointing to your deployed URL:

```
https://your-domain.com/api/webhooks/stripe
```

Select the events to listen for (at minimum: `payment_intent.succeeded`, `payment_intent.payment_failed`, `charge.refunded`). After saving, reveal the signing secret and set it as `STRIPE_WEBHOOK_SECRET`.

## Switching to Live Keys

Replace `sk_test_` → `sk_live_` and `pk_test_` → `pk_live_` in your production environment. Never commit live keys to version control.

## Test Card Numbers

| Card number | Behavior |
|-------------|----------|
| `4242 4242 4242 4242` | Always succeeds |
| `4000 0000 0000 9995` | Always declined (insufficient funds) |
| `4000 0025 0000 3155` | Requires authentication (3DS) |
| `4000 0000 0000 0002` | Always declined (generic decline) |

Use any future expiry date, any 3-digit CVC, and any billing ZIP.

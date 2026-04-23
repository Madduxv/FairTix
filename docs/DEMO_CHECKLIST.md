# FairTix Demo Checklist

## Environment Setup

### 1. Start services

```bash
docker compose up --build
```

Services started:
- Backend (Spring Boot, dev profile): http://localhost:8080
- PostgreSQL 16: localhost:5432
- Redis 7: localhost:6379
- MailHog (email capture): http://localhost:8025

Flyway migrations run automatically on backend startup. Wait for the log line:
`Successfully applied N migrations to schema "public"` before proceeding.

### 2. Verify health

```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

### 3. Create admin user

Register via the UI or API, then promote:

```bash
# Register
curl -s -c cookies.txt -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@fairtix.dev","password":"Admin1234!","firstName":"Demo","lastName":"Admin"}'

# Get the user ID
USER_ID=$(curl -s -b cookies.txt http://localhost:8080/api/auth/me | jq -r '.id')

# Promote — requires an existing admin; for the first admin, run directly in Postgres:
# UPDATE users SET role = 'ADMIN' WHERE email = 'admin@fairtix.dev';
docker compose exec postgres psql -U fairtix -d fairtix \
  -c "UPDATE users SET role = 'ADMIN' WHERE email = 'admin@fairtix.dev';"
```

### 4. Create venue (required for seed data)

```bash
curl -s -b cookies.txt -X POST http://localhost:8080/api/venues \
  -H "Content-Type: application/json" \
  -d '{"name":"Demo Venue","address":"123 Main St","city":"San Francisco","state":"CA","country":"US","capacity":500,"latitude":37.7749,"longitude":-122.4194}'
```

Note the returned venue ID and update `seed_test_events.sql` if it differs from `3ed462ed-27c0-4cc9-8510-aaad0187034e`.

### 5. Load seed data

```bash
# Verify the admin UUID and venue UUID in seed_test_events.sql match your environment, then:
docker compose exec -T postgres psql -U fairtix -d fairtix < seed_test_events.sql
```

This creates:
- **Midnight Jazz Night** (2026-05-10) — no queue, purchase cap 4, 3 sections, $30–$85
- **Summer Rock Festival** (2026-07-04) — queue required (cap 200), 4 sections, $35–$150
- **Stand-Up Comedy Showcase** (2026-06-15) — no queue, no cap, 2 sections, $25–$45

### 6. Start the frontend

```bash
cd frontend/webpages && npm start
# Runs on http://localhost:3000
```

---

## Works-on-My-Machine Verification

- [ ] `docker compose ps` shows all 4 services as `running`
- [ ] `/actuator/health` returns `{"status":"UP"}`
- [ ] Flyway applied all 26 migrations (check backend logs)
- [ ] `SELECT count(*) FROM events;` returns ≥ 3
- [ ] `SELECT count(*) FROM seats;` returns ≥ 50
- [ ] MailHog UI loads at http://localhost:8025
- [ ] Frontend loads at http://localhost:3000 without console errors
- [ ] Admin user exists with `role = 'ADMIN'` in the database

---

## User Journey

### Register → Browse → Queue → Hold → Checkout → Ticket → ICS Export

1. **Register** — navigate to `/register`, create a new account
2. **Browse events** — navigate to `/events`, verify all 3 seeded events appear
3. **Event without queue (Midnight Jazz Night)**
   - Open event detail at `/events/:id`
   - Select a seat (Floor A-1, $85)
   - Confirm hold is created (10-min TTL); navigate to `/my-holds` to verify
4. **Event with queue (Summer Rock Festival)**
   - Open event detail
   - Join the queue; observe SSE-powered position updates in the UI
   - Wait for admission (or reduce queue in admin panel to trigger admission)
   - After admission, proceed to seat selection
5. **Checkout**
   - Navigate to `/checkout`
   - Use simulated payment (dev profile only): set outcome to `SUCCESS`
   - Complete order
6. **Ticket**
   - Navigate to `/my-tickets/:ticketId`
   - Verify ticket status is `VALID`
7. **ICS export**
   - Click "Add to Calendar" on the ticket detail page
   - Verify `.ics` file downloads and opens in a calendar app
   - File served from `GET /api/tickets/:id/calendar.ics`

---

## Admin Journey

### Publish Event, View Fraud Audit, Manage Refunds

1. **Admin dashboard** — navigate to `/admin`
2. **Event management**
   - Go to `/admin/events`
   - Create a new event or inspect a seeded event
   - Verify seat map at `/admin/events/:id/seats`
3. **View fraud audit** — navigate to `/admin/fraud`
   - Verify audit log entries appear for transfer, payment, and hold events
   - Check risk tier display (LOW / MEDIUM / HIGH / CRITICAL)
4. **Manage refunds** — navigate to `/admin/refunds`
   - Submit a refund request as a user (`/refunds`), then approve/deny it as admin
   - Verify that HIGH/CRITICAL risk score blocks auto-approval (manual review required)
5. **User management** — navigate to `/admin/users`
   - Verify user list; test promote endpoint if needed

---

## Edge Cases to Demo

### Transfer

1. Log in as User A, complete a purchase on Midnight Jazz Night
2. Go to ticket detail, initiate a transfer to User B's email
3. Log in as User B, accept the transfer at `/my-tickets` (incoming transfers)
4. Verify ticket ownership changed; check MailHog for transfer notification emails
5. **Velocity limit:** attempt 6 transfers from the same user within 24h — expect `429` on the 6th

### Rate limit hit

1. Rapidly call any rate-limited endpoint (e.g., seat hold) more than the configured limit
2. Observe `429 Too Many Requests` response
3. Wait for the window to reset; verify access resumes

### Fraud score block

1. In the database, set a user's risk tier to CRITICAL:
   ```sql
   UPDATE risk_scores SET tier = 'CRITICAL' WHERE user_id = '<uuid>';
   ```
2. Attempt to initiate a ticket transfer as that user
3. Verify the request is rejected with an appropriate error and an audit log entry is written

### Purchase cap enforcement

1. On Midnight Jazz Night (`max_tickets_per_user = 4`), attempt to purchase a 5th ticket
2. Verify the API rejects the request

---

## Known Limitations for Demo

- Payment simulation is only available in the `dev` Spring profile. In `prod`, simulated outcomes return `400 Bad Request`.
- Queue admission is triggered by a scheduler; during a live demo, lower the queue capacity or manually advance the queue via the database to speed it up.
- ICS calendar export requires a ticket in `VALID` status — cancelled or transferred tickets will not have an active export link.
- MailHog captures all outbound email in dev. No real emails are sent.

---

## Route Accessibility Quick Check

| Route | Auth required | Admin only |
|-------|--------------|-----------|
| `/events` | No | No |
| `/events/near-me` | No | No |
| `/events/:id` | No | No |
| `/my-holds` | Yes | No |
| `/checkout` | Yes | No |
| `/my-tickets/:id` | Yes | No |
| `/refunds` | Yes | No |
| `/support` | Yes | No |
| `/admin` | Yes | Yes |
| `/admin/events` | Yes | Yes |
| `/admin/fraud` | Yes | Yes |
| `/admin/refunds` | Yes | Yes |
| `/admin/users` | Yes | Yes |
| `/admin/venues` | Yes | Yes |
| `/admin/performers` | Yes | Yes |

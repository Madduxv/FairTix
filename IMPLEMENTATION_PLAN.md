# FairTix Implementation Plan — Issues #51, #58, #37, #70, #41

## Implementation Status

| Issue | Status | Notes |
|-------|--------|-------|
| #58 — Seat Uniqueness Constraint | **DONE** | Migration V3, app-level validation, 409 error handling |
| #41 — Rate Limiting | **DONE** | Configurable limits, explicit filter order, Redis fallback, payment endpoint fix |
| #51 — Admin Permissions | **DONE** | Event ownership, audit logging, ownership enforcement on update/delete |
| #37 — Event Listing Frontend | **DONE** | Search, filters, pagination, improved date formatting |
| #70 — Seat Hold Flow Frontend | **DONE** | Auto-polling, duration selection, checkout expiration warnings |

All 119 backend tests pass. Frontend builds cleanly.

---

## Issue #58 — Seat Uniqueness Constraint & Inventory Integrity (DONE)

### What Was Implemented

1. **Flyway migration `V3__add_seat_uniqueness_constraint.sql`**
   - Cleans up any existing duplicate seats
   - Adds `UNIQUE (event_id, section, row_label, seat_number)` constraint
   - Adds `CHECK (status IN ('AVAILABLE', 'HELD', 'BOOKED', 'SOLD'))` constraint

2. **Application-level validation**
   - `SeatRepository.existsByEvent_IdAndSectionAndRowLabelAndSeatNumber()` — query method
   - `SeatService.createSeat()` — checks for existing seat before insert, throws `DuplicateSeatException`
   - `DuplicateSeatException` — new exception class, mapped to HTTP 409 with code `DUPLICATE_SEAT`

3. **JPA entity annotation**
   - `Seat.java` `@Table` updated with `@UniqueConstraint` matching the DB constraint

4. **Test coverage**
   - `createSeat_duplicate_returns409()` — verifies duplicate seat creation is rejected

### Issues Encountered

- None. Clean implementation — no existing duplicates in test data.

### Remaining Considerations

- **AdminSeatsPage.js** doesn't validate uniqueness client-side. The 409 will surface as a server error. Consider adding a frontend toast for this error code in a follow-up.
- If a bulk-create endpoint is added in the future, it must handle partial failures gracefully.

---

## Issue #41 — Finish Rate Limiting & Verify No Flows Are Blocked (DONE)

### What Was Implemented

1. **Externalized configuration** (`application.properties`)
   ```properties
   ratelimit.auth=10
   ratelimit.orders=20
   ratelimit.seats=60
   ratelimit.default=100
   ```
   - `RateLimitService` now uses `@Value` injection instead of hardcoded limits

2. **Explicit filter ordering**
   - `@Order(Ordered.HIGHEST_PRECEDENCE + 10)` on `RateLimitFilter`

3. **Redis failure fallback**
   - `try/catch` around `rateLimitService.isAllowed()` — fails open with log warning

4. **Payment endpoint rate limit fix**
   - `/api/payments` now uses the `ordersLimit` (20/min) instead of default 100/min

5. **Auth endpoint fix**
   - `/auth/register` and `/auth/login` (without `/api` prefix) now correctly matched

6. **JSON 429 response body**
   - Filter now returns `{"status":429,"code":"RATE_LIMITED","message":"..."}` instead of empty body

7. **IP resolution fix**
   - `X-Forwarded-For` now takes the first IP (`split(",")[0].trim()`) for proxied requests

8. **Cleaner code**
   - Extracted `resolveKey()` and `resolveIp()` helper methods, eliminating 4x duplicated IP resolution blocks
   - Extracted `resolveLimit()` method, making limit resolution unit-testable

9. **Tests**
   - 12 unit tests covering all endpoint categories + new `testResolveLimitCoversAllEndpointCategories()` + `testPaymentsEndpointUsesOrdersLimit()` + `testAuthRegisterEndpointUsesAuthLimit()`
   - Uses `ReflectionTestUtils.setField()` to inject `@Value` fields in unit tests

### Flow Verification

| Flow | Endpoint | Limit | Calls/min | Blocked? |
|------|----------|-------|-----------|----------|
| Login | `/auth/login` | 10 | 1 | No |
| Browse events | `/api/events` | 100 | 1-5 | No |
| Seat polling (10s) | `/api/events/{id}/seats` | 60 | 6 | No |
| Create hold | `/api/events/{id}/holds` | 20 | 1-3 | No |
| Checkout | `/api/payments/checkout` | 20 | 1-2 | No |
| Admin: bulk seat create | `/api/events/{id}/seats` | 60 | 10-50 | **Risk at 60+** |

### Remaining Considerations

- **X-RateLimit-* headers** were planned but not implemented — Redisson's `RRateLimiter` API doesn't easily expose remaining permits/reset time. Would require a custom wrapper. Deferred.
- **Integration tests** for HTTP 429 responses would require a real or embedded Redis. Deferred in favor of thorough unit test coverage.

---

## Issue #51 — Implement Admin Permissions (DONE)

### What Was Implemented

1. **Event ownership model**
   - `Event.java` — added `organizerId` (UUID) field
   - `V4__add_event_organizer.sql` — migration adding `organizer_id` column with FK to `users(id)`, backfills from first admin (fallback: first user)
   - `EventResponse.java` — now includes `organizerId` in API responses
   - `EventController.createEvent()` — sets `organizerId` from JWT principal

2. **Ownership enforcement**
   - `EventService.update()` and `EventService.delete()` — verify `event.getOrganizerId().equals(callerId)`, throws `AccessDeniedException` if mismatch
   - `EventController` — passes `principal.getUserId()` to service on all mutating operations

3. **Audit logging**
   - `AuditLog.java` — entity with userId, action, resourceType, resourceId, details, createdAt
   - `AuditLogRepository.java` — JPA repository
   - `AuditService.java` — `log()` method with `REQUIRES_NEW` propagation (audit persists even if outer transaction rolls back)
   - `V5__create_audit_log.sql` — migration for `audit_logs` table with indexes
   - Audit calls in `EventController` for CREATE, UPDATE, DELETE actions

4. **Test updates**
   - All test files updated to use 4-arg `Event(...)` constructor and new service signatures
   - Both `EventControllerTest` classes updated to use `WithMockPrincipal` instead of `@WithMockUser` / `user().roles()` to provide `CustomUserPrincipal`

### Issues Encountered

- **NullPointerException on `principal.getUserId()`**: The existing tests used `user("admin@test.com").roles("ADMIN")` which creates a basic Spring `User`, not a `CustomUserPrincipal`. Fixed by switching all admin-route tests to `WithMockPrincipal.admin(...)`.
- **Two EventControllerTest classes**: Found tests in both `com.fairtix.events.api` and `com.fairtix.event.api` — both needed updates.
- **Constructor change cascade**: Changing `Event` from 3-arg to 4-arg constructor required updates in 8 test files.

### Remaining Considerations

- **`organizer_id` is nullable in DB** — the backfill migration doesn't add a NOT NULL constraint because if no users exist at all, the backfill would fail. Consider adding `ALTER TABLE events ALTER COLUMN organizer_id SET NOT NULL` as a separate migration after confirming prod data is clean.
- **Seat creation ownership** — `SeatController` doesn't yet verify the admin owns the event when creating seats. This is noted as a follow-up.
- **Analytics scoping** — `AnalyticsService` still returns global aggregates. Scoping by organizer would be a follow-up.

---

## Issue #37 — Build Event Listing Page in Frontend (DONE)

### What Was Implemented

1. **Search & filter controls**
   - Title search input — maps to `?title=` query param
   - Venue filter input — maps to `?venueName=` query param
   - "Show past events" checkbox — toggles `?upcoming=` param
   - 300ms debounce on all filter changes
   - Filters reset page to 0 on change

2. **Pagination**
   - Previous/Next buttons with disabled states
   - "Page X of Y" display
   - Page size selector (10 / 20 / 50)
   - "Showing X–Y of Z events" info line

3. **Improved date formatting**
   - Uses `toLocaleDateString()` with weekday, year, month, day, hour, minute

4. **Styling**
   - Filter bar with flexbox layout, responsive wrapping
   - Styled inputs with focus states
   - Pagination controls bar
   - Updated empty state message ("No events match your search")

### Issues Encountered

- None. The backend already supported all query params — this was purely frontend work.

### Remaining Considerations

- **Seat availability counts** on event cards would require a backend change (adding counts to `EventResponse`). Deferred — would add N+1 query overhead without a proper aggregate query.
- **URL-based filter state** (query params in browser URL) would enable shareable search links. Not implemented.

---

## Issue #70 — Seat Hold Flow in Frontend (DONE)

### What Was Implemented

1. **Auto-polling seat availability** (`EventDetail.js`)
   - `setInterval` every 10 seconds re-fetches seat data
   - Only polls when browser tab is visible (`document.hidden` check)
   - Auto-deselects seats that changed status during polling
   - Tracks previous seat state via `useRef` to detect changes

2. **Hold duration selection** (`EventDetail.js`)
   - Dropdown in selection bar: 5 / 10 (default) / 15 / 30 / 60 minutes
   - Passes `durationMinutes` to `POST /api/events/{eventId}/holds`
   - Success message reflects chosen duration

3. **Improved conflict error display** (`EventDetail.js`)
   - Shows the server's error message directly on 409 instead of generic text
   - Better error categorization (409 vs 401/403 vs other)

4. **Price column fix** (`EventDetail.js`)
   - Added missing `<td>` for the Price column that existed in `<th>` but not in row rendering

5. **Checkout expiration warnings** (`Checkout.js`)
   - 1-second countdown tick for hold expiration tracking
   - Warning banner when any hold has < 2 minutes remaining (amber background)
   - Error banner with "Back to My Holds" button when holds have expired
   - Only shows warnings during the `form` payment state (not during processing/success)

6. **Styling**
   - Hold duration select styling in `EventDetail.css`
   - Warning style (`.checkout-warning`) in `Checkout.css`

### Issues Encountered

- None. Backend already supports `durationMinutes` and `expiresAt` in responses.

### Remaining Considerations

- **Hold extension endpoint** — backend doesn't support extending holds. Would need `POST /api/holds/{id}/extend` endpoint.
- **WebSocket for real-time updates** — polling at 10s is good enough for now, but WebSocket/SSE would eliminate the delay. At 60 req/min seat limit, a single tab uses 6 req/min. Multiple tabs sharing the same user bucket could approach the limit.
- **"Confirm All" button** in MyHolds was planned but deferred — lower priority UX improvement.

---

## Cross-Issue Notes

### Migration Ordering

Migrations were numbered to avoid conflicts:
- `V3__add_seat_uniqueness_constraint.sql` (#58)
- `V4__add_event_organizer.sql` (#51)
- `V5__create_audit_log.sql` (#51)

### Files Changed Summary

**Backend (new files):**
- `V3__add_seat_uniqueness_constraint.sql`
- `V4__add_event_organizer.sql`
- `V5__create_audit_log.sql`
- `DuplicateSeatException.java`
- `AuditLog.java`
- `AuditLogRepository.java`
- `AuditService.java`

**Backend (modified):**
- `Seat.java` — unique constraint annotation
- `SeatRepository.java` — `existsBy...` method
- `SeatService.java` — duplicate check
- `GlobalExceptionHandler.java` — DuplicateSeatException handler
- `Event.java` — organizerId field
- `EventService.java` — ownership param on create/update/delete, ownership verification
- `EventController.java` — principal injection, audit logging
- `EventResponse.java` — organizerId field
- `RateLimitService.java` — configurable limits, payment endpoint, auth path fix
- `RateLimitFilter.java` — explicit order, Redis fallback, JSON response, code cleanup
- `application.properties` — rate limit config

**Backend (test updates):**
- `SeatControllerTest.java` — duplicate test, constructor update
- `EventControllerTest.java` (both) — WithMockPrincipal, constructor updates
- `EventServiceTest.java` — constructor and method signature updates
- `SeatHoldServiceTest.java` — constructor update
- `AnalyticsControllerTest.java` — constructor update
- `OrderControllerTest.java` — constructor update
- `PaymentControllerTest.java` — constructor update
- `TicketControllerTest.java` — constructor update
- `AccountDeletionTest.java` — constructor update
- `RateLimitServiceTest.java` — ReflectionTestUtils, new tests

**Frontend (modified):**
- `Events.js` — search, filters, pagination
- `Events.css` — filter bar, pagination styles
- `EventDetail.js` — polling, duration select, price column fix
- `EventDetail.css` — duration select styling
- `Checkout.js` — expiration warnings
- `Checkout.css` — warning style

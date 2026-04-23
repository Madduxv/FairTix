# FairTix Resale Policy

## Current Transfer Architecture

Ticket transfers are peer-to-peer and face-value only. The flow is:

1. Ticket owner initiates a transfer request to a recipient by email address.
2. The recipient receives an email notification and must explicitly accept within 7 days.
3. On acceptance, ticket ownership is atomically reassigned in the database.
4. The sender receives a confirmation email. Either party may cancel/reject before acceptance.
5. Expired requests (7-day TTL) are swept by a scheduled job every 60 seconds.

**Key implementation classes:**
- `TransferService` — orchestrates all transfer lifecycle operations
- `TicketTransferRequest` / `TicketTransferRequestRepository` — domain model and persistence
- `TransferVelocityExceededException` — thrown when velocity limit is breached

**Velocity limiting:** Redis sorted set keyed `transfer:velocity:{userId}`. Default: max 5 transfers per 24-hour rolling window, configurable via `fairtix.transfer.velocity.max-per-window` and `fairtix.transfer.velocity.window-hours`.

**Fraud gating:** Users at `RiskTier.CRITICAL` are blocked from initiating transfers entirely. The block is audit-logged.

**Audit trail:** Every transfer lifecycle event (`TRANSFER_REQUESTED`, `TRANSFER_ACCEPTED`, `TRANSFER_REJECTED`, `TRANSFER_CANCELLED`, `TRANSFER_EXPIRED`, `TRANSFER_BLOCKED_FRAUD_RISK`) is written to the audit log with request correlation.

---

## Policy Decision: Face-Value Only

Transfers carry no additional price — the recipient pays nothing beyond what the original purchaser paid. This is intentional and permanent for this project scope.

**Why face-value only:**
- Eliminates the primary scalping incentive: a ticket that cannot be resold above face value is not worth bulk-purchasing for resale.
- Removes the need for a payment escrow, payout processing, or tax compliance infrastructure.
- Keeps the trust model simple: both parties are registered FairTix users; no anonymous marketplace.

---

## Why the Current Design Prevents Scalping

| Mechanism | Effect |
|-----------|--------|
| Face-value-only enforcement (no price field on transfer) | Markup is structurally impossible |
| Velocity limit (5/24h default) | Bulk transfer operations are rate-blocked |
| CRITICAL-tier fraud block | High-risk accounts cannot transfer at all |
| Recipient must be a registered user | Anonymous resale is not possible |
| 7-day expiry | Requests cannot be held open indefinitely to game availability |
| Full audit trail | Suspicious transfer patterns are visible to admins |

---

## What a Future Resale Marketplace Would Require

This section documents scope that was intentionally deferred. It is not planned for this milestone.

| Capability | What it requires |
|------------|-----------------|
| Seller-set price above face value | Price field on transfer, price floor/ceiling rules, policy enforcement |
| Buyer payment to seller | Escrow service, payment split at checkout, payout scheduling |
| Seller payout | Bank account collection, payout batching, failed payout handling |
| Tax compliance (US) | 1099-K issuance for sellers above IRS threshold ($600/year as of 2024), TIN collection |
| Marketplace listing | Search/browse interface for available resale tickets, price sorting |
| Anti-fraud for marketplace | Secondary velocity limits, price anomaly detection, buyer/seller reputation |
| Refund policy on resale | Separate from primary refund policy; seller vs. platform liability |

---

## Decision: No Marketplace in This Milestone

The current milestone (M8) ships face-value-only peer-to-peer transfer only. The transfer architecture is complete and does not require modification to enforce this policy — the absence of a price field on `TicketTransferRequest` is the enforcement mechanism.

Any future marketplace work should begin with a new Flyway migration to add pricing fields and a new `ResaleListingService` rather than modifying `TransferService`.

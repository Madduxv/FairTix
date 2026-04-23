-- Extend the active-refund uniqueness constraint to also cover PENDING_MANUAL,
-- preventing concurrent duplicate refund requests that bypass the application-level check.
DROP INDEX IF EXISTS idx_refund_one_active_per_order;
CREATE UNIQUE INDEX idx_refund_one_active_per_order
    ON refund_requests (order_id)
    WHERE status IN ('REQUESTED', 'PENDING_MANUAL', 'APPROVED');

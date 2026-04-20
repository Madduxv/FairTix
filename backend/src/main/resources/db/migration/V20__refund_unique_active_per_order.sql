-- Prevent duplicate active refund requests per order at the DB level.
-- Only one REQUESTED or APPROVED refund can exist per order at a time.
CREATE UNIQUE INDEX idx_refund_one_active_per_order
    ON refund_requests (order_id)
    WHERE status IN ('REQUESTED', 'APPROVED');

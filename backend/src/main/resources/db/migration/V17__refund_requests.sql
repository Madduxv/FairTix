CREATE TABLE refund_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID NOT NULL REFERENCES orders(id),
    user_id         UUID NOT NULL REFERENCES users(id),
    amount          NUMERIC(10,2) NOT NULL,
    reason          TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    admin_notes     TEXT,
    reviewed_by     UUID REFERENCES users(id),
    reviewed_at     TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refund_order ON refund_requests(order_id);
CREATE INDEX idx_refund_user ON refund_requests(user_id);
CREATE INDEX idx_refund_status ON refund_requests(status);

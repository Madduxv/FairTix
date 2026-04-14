-- Payment records table (matches PaymentRecord.java entity)
CREATE TABLE payment_records (
    id              UUID PRIMARY KEY,
    order_id        UUID        NOT NULL REFERENCES orders(id) ON DELETE RESTRICT,
    user_id         UUID        NOT NULL REFERENCES users(id),
    amount          NUMERIC(10, 2) NOT NULL,
    currency        VARCHAR(3)  NOT NULL,
    status          VARCHAR(20) NOT NULL,
    transaction_id  VARCHAR(255) NOT NULL UNIQUE,
    failure_reason  VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_order_id ON payment_records(order_id);
CREATE INDEX idx_payment_user_id  ON payment_records(user_id);
CREATE INDEX idx_payment_status   ON payment_records(status);

CREATE TABLE payment_records (
    id             UUID        NOT NULL DEFAULT gen_random_uuid(),
    order_id       UUID        NOT NULL,
    user_id        UUID        NOT NULL,
    amount         NUMERIC(10, 2) NOT NULL,
    currency       VARCHAR(3)  NOT NULL,
    status         VARCHAR(20) NOT NULL,
    transaction_id VARCHAR(255) NOT NULL UNIQUE,
    failure_reason TEXT,
    created_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_payment_records PRIMARY KEY (id)
);

CREATE INDEX idx_payment_order_id ON payment_records (order_id);
CREATE INDEX idx_payment_user_id  ON payment_records (user_id);

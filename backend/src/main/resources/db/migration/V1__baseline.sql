-- Baseline migration: captures the existing FairTix schema.
-- Uses IF NOT EXISTS so it is safe to run against databases that already have these tables.

CREATE TABLE IF NOT EXISTS users (
    id       UUID PRIMARY KEY,
    email    VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS events (
    id         UUID PRIMARY KEY,
    title      VARCHAR(500) NOT NULL,
    venue      VARCHAR(255) NOT NULL,
    start_time TIMESTAMPTZ  NOT NULL
);

CREATE TABLE IF NOT EXISTS seats (
    id          UUID PRIMARY KEY,
    event_id    UUID         NOT NULL REFERENCES events(id),
    section     VARCHAR(255) NOT NULL,
    row_label   VARCHAR(255) NOT NULL,
    seat_number VARCHAR(255) NOT NULL,
    status      VARCHAR(255) NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_seat_event_id ON seats(event_id);
CREATE INDEX IF NOT EXISTS idx_seat_status   ON seats(status);

CREATE TABLE IF NOT EXISTS seat_holds (
    id         UUID PRIMARY KEY,
    seat_id    UUID        NOT NULL REFERENCES seats(id),
    owner_id   UUID        NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    status     VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_hold_status_expires ON seat_holds(status, expires_at);
CREATE INDEX IF NOT EXISTS idx_hold_owner_id       ON seat_holds(owner_id);

CREATE TABLE IF NOT EXISTS orders (
    id           UUID PRIMARY KEY,
    user_id      UUID           NOT NULL REFERENCES users(id),
    status       VARCHAR(255)   NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL,
    currency     VARCHAR(3)     NOT NULL,
    created_at   TIMESTAMPTZ    NOT NULL,
    updated_at   TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_order_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_order_status  ON orders(status);

CREATE TABLE IF NOT EXISTS order_holds (
    order_id UUID NOT NULL REFERENCES orders(id),
    hold_id  UUID NOT NULL
);

CREATE TABLE IF NOT EXISTS tickets (
    id        UUID PRIMARY KEY,
    order_id  UUID        NOT NULL REFERENCES orders(id),
    user_id   UUID        NOT NULL REFERENCES users(id),
    seat_id   UUID        NOT NULL REFERENCES seats(id),
    event_id  UUID        NOT NULL REFERENCES events(id),
    status    VARCHAR(255) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ticket_user_id  ON tickets(user_id);
CREATE INDEX IF NOT EXISTS idx_ticket_order_id ON tickets(order_id);
CREATE INDEX IF NOT EXISTS idx_ticket_event_id ON tickets(event_id);

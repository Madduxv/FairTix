CREATE TABLE support_tickets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    subject         VARCHAR(200) NOT NULL,
    category        VARCHAR(50) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    priority        VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    order_id        UUID REFERENCES orders(id),
    event_id        UUID REFERENCES events(id),
    assigned_to     UUID REFERENCES users(id),
    closed_at       TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ticket_messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id       UUID NOT NULL REFERENCES support_tickets(id) ON DELETE CASCADE,
    author_id       UUID NOT NULL REFERENCES users(id),
    message         TEXT NOT NULL,
    is_staff        BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_support_user ON support_tickets(user_id);
CREATE INDEX idx_support_status ON support_tickets(status);
CREATE INDEX idx_support_assigned ON support_tickets(assigned_to);
CREATE INDEX idx_ticket_msg_ticket ON ticket_messages(ticket_id);

-- Add emailSupport preference column
ALTER TABLE notification_preferences ADD COLUMN email_support BOOLEAN NOT NULL DEFAULT true;

ALTER TABLE events ADD COLUMN queue_required BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE events ADD COLUMN queue_capacity INTEGER;

CREATE TABLE queue_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    position INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    admitted_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_queue_event_user UNIQUE (event_id, user_id)
);

CREATE INDEX idx_qe_event_status ON queue_entries(event_id, status);
CREATE INDEX idx_qe_token ON queue_entries(token);
CREATE INDEX idx_qe_event_position ON queue_entries(event_id, position);

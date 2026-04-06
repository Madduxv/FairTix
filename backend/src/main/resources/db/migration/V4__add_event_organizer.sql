-- Add organizer ownership to events.
-- Nullable initially to allow backfill; then made NOT NULL.
ALTER TABLE events ADD COLUMN organizer_id UUID REFERENCES users(id);

-- Backfill: assign existing events to the first admin, or first user if no admin exists.
UPDATE events SET organizer_id = (
    SELECT id FROM users
    WHERE role = 'ADMIN'
    ORDER BY id
    LIMIT 1
)
WHERE organizer_id IS NULL;

-- Fallback: if no admin existed, assign to first user
UPDATE events SET organizer_id = (
    SELECT id FROM users ORDER BY id LIMIT 1
)
WHERE organizer_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_event_organizer_id ON events(organizer_id);

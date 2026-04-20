-- Add lifecycle status and transition timestamps to events
ALTER TABLE events ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';
ALTER TABLE events ADD COLUMN published_at TIMESTAMPTZ;
ALTER TABLE events ADD COLUMN cancelled_at TIMESTAMPTZ;
ALTER TABLE events ADD COLUMN completed_at TIMESTAMPTZ;
ALTER TABLE events ADD COLUMN archived_at TIMESTAMPTZ;
ALTER TABLE events ADD COLUMN cancellation_reason TEXT;

CREATE INDEX idx_events_status ON events(status);

-- Backfill: existing events are already visible, so treat them as PUBLISHED
UPDATE events SET status = 'PUBLISHED', published_at = NOW();

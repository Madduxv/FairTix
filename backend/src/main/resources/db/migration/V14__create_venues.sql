-- Create venues table
CREATE TABLE venues (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(255) NOT NULL UNIQUE,
  address VARCHAR(500),
  city VARCHAR(100),
  country VARCHAR(100),
  capacity INTEGER,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Migrate existing venue strings to venue rows
INSERT INTO venues (name)
SELECT DISTINCT venue FROM events WHERE venue IS NOT NULL
ON CONFLICT (name) DO NOTHING;

-- Add venue_id FK to events
ALTER TABLE events ADD COLUMN venue_id UUID REFERENCES venues(id);

-- Backfill venue_id from existing venue string
UPDATE events e SET venue_id = v.id FROM venues v WHERE e.venue = v.name;

-- Drop old venue string column
ALTER TABLE events DROP COLUMN venue;

CREATE INDEX idx_venues_name ON venues(name);

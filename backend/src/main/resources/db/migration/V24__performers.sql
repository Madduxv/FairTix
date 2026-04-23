CREATE TABLE performers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    genre VARCHAR(100),
    bio TEXT,
    image_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE event_performers (
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    performer_id UUID NOT NULL REFERENCES performers(id) ON DELETE CASCADE,
    PRIMARY KEY (event_id, performer_id)
);

CREATE INDEX idx_event_performers_performer ON event_performers(performer_id);

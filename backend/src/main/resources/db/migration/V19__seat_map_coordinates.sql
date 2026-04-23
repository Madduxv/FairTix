-- Add coordinate data to seats for visual seat map positioning
ALTER TABLE seats ADD COLUMN pos_x DOUBLE PRECISION;
ALTER TABLE seats ADD COLUMN pos_y DOUBLE PRECISION;
ALTER TABLE seats ADD COLUMN rotation DOUBLE PRECISION DEFAULT 0;

-- Add section geometry to venues for layout rendering (Phase 2 admin editor)
CREATE TABLE venue_sections (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id        UUID NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    section_type    VARCHAR(30) NOT NULL DEFAULT 'STANDARD',
    path_data       TEXT,
    pos_x           DOUBLE PRECISION NOT NULL DEFAULT 0,
    pos_y           DOUBLE PRECISION NOT NULL DEFAULT 0,
    width           DOUBLE PRECISION NOT NULL DEFAULT 100,
    height          DOUBLE PRECISION NOT NULL DEFAULT 100,
    color           VARCHAR(7) DEFAULT '#E0E0E0',
    sort_order      INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_venue_sections_venue ON venue_sections(venue_id);

-- Add per-user purchase cap to events table; NULL means no cap (backwards compatible)
ALTER TABLE events ADD COLUMN max_tickets_per_user INTEGER DEFAULT NULL;

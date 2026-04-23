-- Add soft-delete column to users table (matches User.java entity)
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

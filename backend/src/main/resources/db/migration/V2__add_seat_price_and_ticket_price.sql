-- Add price column to seats (backfill existing rows with 0.00 before adding NOT NULL).
ALTER TABLE seats ADD COLUMN IF NOT EXISTS price NUMERIC(10, 2);
UPDATE seats SET price = 0.00 WHERE price IS NULL;
ALTER TABLE seats ALTER COLUMN price SET NOT NULL;

-- Add price snapshot column to tickets (backfill existing rows with 0.00 before adding NOT NULL).
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS price NUMERIC(10, 2);
UPDATE tickets SET price = 0.00 WHERE price IS NULL;
ALTER TABLE tickets ALTER COLUMN price SET NOT NULL;

-- Remove any accidental duplicate seats (keeps the earliest-inserted row by UUID).
DELETE FROM seats a USING seats b
WHERE a.id > b.id
  AND a.event_id = b.event_id
  AND a.section  = b.section
  AND a.row_label = b.row_label
  AND a.seat_number = b.seat_number;

-- Prevent duplicate seats per event.
ALTER TABLE seats
ADD CONSTRAINT uk_seat_event_section_row_number
UNIQUE (event_id, section, row_label, seat_number);

-- Restrict status column to known values.
ALTER TABLE seats
ADD CONSTRAINT ck_seat_status
CHECK (status IN ('AVAILABLE', 'HELD', 'BOOKED', 'SOLD'));

-- Seed demo venues (with lat/lon) and events for local testing and map demo.
-- Uses DO block to resolve organizer_id at runtime without hard-coded UUIDs.

DO $$
DECLARE
  v_organizer_id UUID;
  v_venue1_id    UUID;
  v_venue2_id    UUID;
  v_venue3_id    UUID;
  v_event1_id    UUID;
  v_event2_id    UUID;
  v_event3_id    UUID;
BEGIN

  -- Use first admin user as organizer; fall back to first user if none exists
  SELECT id INTO v_organizer_id FROM users WHERE role = 'ADMIN' ORDER BY id LIMIT 1;
  IF v_organizer_id IS NULL THEN
    SELECT id INTO v_organizer_id FROM users ORDER BY id LIMIT 1;
  END IF;

  -- Venues
  INSERT INTO venues (id, name, address, city, country, capacity, latitude, longitude)
  VALUES (gen_random_uuid(), 'Smoothie King Center', '1501 Dave Dixon Dr', 'New Orleans', 'US', 19000, 29.948925, -90.082089)
  ON CONFLICT (name) DO UPDATE SET latitude = EXCLUDED.latitude, longitude = EXCLUDED.longitude
  RETURNING id INTO v_venue1_id;
  IF v_venue1_id IS NULL THEN
    SELECT id INTO v_venue1_id FROM venues WHERE name = 'Smoothie King Center';
  END IF;

  INSERT INTO venues (id, name, address, city, country, capacity, latitude, longitude)
  VALUES (gen_random_uuid(), 'Toyota Center', '1510 Polk St', 'Houston', 'US', 18300, 29.750771, -95.362179)
  ON CONFLICT (name) DO UPDATE SET latitude = EXCLUDED.latitude, longitude = EXCLUDED.longitude
  RETURNING id INTO v_venue2_id;
  IF v_venue2_id IS NULL THEN
    SELECT id INTO v_venue2_id FROM venues WHERE name = 'Toyota Center';
  END IF;

  INSERT INTO venues (id, name, address, city, country, capacity, latitude, longitude)
  VALUES (gen_random_uuid(), 'Caesars Superdome', '1500 Sugar Bowl Dr', 'New Orleans', 'US', 73208, 29.950931, -90.081364)
  ON CONFLICT (name) DO UPDATE SET latitude = EXCLUDED.latitude, longitude = EXCLUDED.longitude
  RETURNING id INTO v_venue3_id;
  IF v_venue3_id IS NULL THEN
    SELECT id INTO v_venue3_id FROM venues WHERE name = 'Caesars Superdome';
  END IF;

  -- Events
  v_event1_id := gen_random_uuid();
  INSERT INTO events (id, title, start_time, venue_id, organizer_id, status, published_at, max_tickets_per_user, queue_required, version)
  VALUES (
    v_event1_id,
    'Jazz Fest 2026',
    NOW() + INTERVAL '14 days',
    v_venue1_id,
    v_organizer_id,
    'ACTIVE',
    NOW(),
    4,
    false,
    0
  );

  v_event2_id := gen_random_uuid();
  INSERT INTO events (id, title, start_time, venue_id, organizer_id, status, published_at, max_tickets_per_user, queue_required, version)
  VALUES (
    v_event2_id,
    'Houston Rodeo Night',
    NOW() + INTERVAL '21 days',
    v_venue2_id,
    v_organizer_id,
    'ACTIVE',
    NOW(),
    2,
    false,
    0
  );

  v_event3_id := gen_random_uuid();
  INSERT INTO events (id, title, start_time, venue_id, organizer_id, status, published_at, max_tickets_per_user, queue_required, version)
  VALUES (
    v_event3_id,
    'Saints vs Panthers',
    NOW() + INTERVAL '7 days',
    v_venue3_id,
    v_organizer_id,
    'ACTIVE',
    NOW(),
    6,
    false,
    0
  );

  -- Seats for each event (3 sections × 5 seats = 15 per event)
  INSERT INTO seats (id, event_id, section, row_label, seat_number, status, price)
  SELECT
    gen_random_uuid(),
    v_event1_id,
    sec.section,
    row_label,
    seat_num::TEXT,
    'AVAILABLE',
    sec.price
  FROM (VALUES ('Floor', 49.00), ('Balcony', 29.00), ('VIP', 99.00)) AS sec(section, price),
       (VALUES ('A'), ('B'), ('C')) AS rows(row_label),
       generate_series(1, 5) AS seat_num;

  INSERT INTO seats (id, event_id, section, row_label, seat_number, status, price)
  SELECT
    gen_random_uuid(),
    v_event2_id,
    sec.section,
    row_label,
    seat_num::TEXT,
    'AVAILABLE',
    sec.price
  FROM (VALUES ('Floor', 55.00), ('Mezzanine', 35.00), ('VIP', 120.00)) AS sec(section, price),
       (VALUES ('A'), ('B'), ('C')) AS rows(row_label),
       generate_series(1, 5) AS seat_num;

  INSERT INTO seats (id, event_id, section, row_label, seat_number, status, price)
  SELECT
    gen_random_uuid(),
    v_event3_id,
    sec.section,
    row_label,
    seat_num::TEXT,
    'AVAILABLE',
    sec.price
  FROM (VALUES ('Lower Bowl', 75.00), ('Upper Deck', 40.00), ('Club', 150.00)) AS sec(section, price),
       (VALUES ('A'), ('B'), ('C')) AS rows(row_label),
       generate_series(1, 5) AS seat_num;

END $$;

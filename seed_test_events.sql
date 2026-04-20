-- Seed 3 test events with varied seats
-- Admin user and venue are pre-existing

DO $$
DECLARE
  admin_id UUID := '0d97e988-e920-4546-a9bb-65be8793ca2d';
  venue_id UUID := '3ed462ed-27c0-4cc9-8510-aaad0187034e';
  event1   UUID := gen_random_uuid();
  event2   UUID := gen_random_uuid();
  event3   UUID := gen_random_uuid();
BEGIN

-- Event 1: Midnight Jazz Night — small club, 3 sections, purchase cap of 4, no queue
INSERT INTO events (id, title, start_time, organizer_id, venue_id, queue_required, queue_capacity, max_tickets_per_user, status, published_at, version)
VALUES (event1, 'Midnight Jazz Night', '2026-05-10 20:00:00+00', admin_id, venue_id, false, NULL, 4, 'ACTIVE', now(), 0);

INSERT INTO seats (id, event_id, section, row_label, seat_number, status, price, version) VALUES
  (gen_random_uuid(), event1, 'Floor',     'A', '1', 'AVAILABLE', 85.00, 0),
  (gen_random_uuid(), event1, 'Floor',     'A', '2', 'AVAILABLE', 85.00, 0),
  (gen_random_uuid(), event1, 'Floor',     'A', '3', 'AVAILABLE', 85.00, 0),
  (gen_random_uuid(), event1, 'Floor',     'B', '1', 'AVAILABLE', 75.00, 0),
  (gen_random_uuid(), event1, 'Floor',     'B', '2', 'AVAILABLE', 75.00, 0),
  (gen_random_uuid(), event1, 'Floor',     'B', '3', 'AVAILABLE', 75.00, 0),
  (gen_random_uuid(), event1, 'Mezzanine', 'A', '1', 'AVAILABLE', 55.00, 0),
  (gen_random_uuid(), event1, 'Mezzanine', 'A', '2', 'AVAILABLE', 55.00, 0),
  (gen_random_uuid(), event1, 'Mezzanine', 'A', '3', 'AVAILABLE', 55.00, 0),
  (gen_random_uuid(), event1, 'Mezzanine', 'B', '1', 'AVAILABLE', 50.00, 0),
  (gen_random_uuid(), event1, 'Mezzanine', 'B', '2', 'AVAILABLE', 50.00, 0),
  (gen_random_uuid(), event1, 'Mezzanine', 'B', '3', 'AVAILABLE', 50.00, 0),
  (gen_random_uuid(), event1, 'Bar',       'A', '1', 'AVAILABLE', 30.00, 0),
  (gen_random_uuid(), event1, 'Bar',       'A', '2', 'AVAILABLE', 30.00, 0),
  (gen_random_uuid(), event1, 'Bar',       'A', '3', 'AVAILABLE', 30.00, 0),
  (gen_random_uuid(), event1, 'Bar',       'A', '4', 'AVAILABLE', 30.00, 0);

-- Event 2: Summer Rock Festival — large, 4 sections, wide price range, queue required (cap 200)
INSERT INTO events (id, title, start_time, organizer_id, venue_id, queue_required, queue_capacity, max_tickets_per_user, status, published_at, version)
VALUES (event2, 'Summer Rock Festival', '2026-07-04 18:00:00+00', admin_id, venue_id, true, 200, NULL, 'ACTIVE', now(), 0);

INSERT INTO seats (id, event_id, section, row_label, seat_number, status, price, version) VALUES
  (gen_random_uuid(), event2, 'Pit',        'GA', '1', 'AVAILABLE', 150.00, 0),
  (gen_random_uuid(), event2, 'Pit',        'GA', '2', 'AVAILABLE', 150.00, 0),
  (gen_random_uuid(), event2, 'Pit',        'GA', '3', 'AVAILABLE', 150.00, 0),
  (gen_random_uuid(), event2, 'Pit',        'GA', '4', 'AVAILABLE', 150.00, 0),
  (gen_random_uuid(), event2, 'Pit',        'GA', '5', 'AVAILABLE', 150.00, 0),
  (gen_random_uuid(), event2, 'Lower Bowl', 'A',  '1', 'AVAILABLE', 110.00, 0),
  (gen_random_uuid(), event2, 'Lower Bowl', 'A',  '2', 'AVAILABLE', 110.00, 0),
  (gen_random_uuid(), event2, 'Lower Bowl', 'A',  '3', 'AVAILABLE', 110.00, 0),
  (gen_random_uuid(), event2, 'Lower Bowl', 'B',  '1', 'AVAILABLE', 100.00, 0),
  (gen_random_uuid(), event2, 'Lower Bowl', 'B',  '2', 'AVAILABLE', 100.00, 0),
  (gen_random_uuid(), event2, 'Lower Bowl', 'B',  '3', 'AVAILABLE', 100.00, 0),
  (gen_random_uuid(), event2, 'Lower Bowl', 'B',  '4', 'AVAILABLE', 100.00, 0),
  (gen_random_uuid(), event2, 'Upper Bowl', 'A',  '1', 'AVAILABLE', 65.00, 0),
  (gen_random_uuid(), event2, 'Upper Bowl', 'A',  '2', 'AVAILABLE', 65.00, 0),
  (gen_random_uuid(), event2, 'Upper Bowl', 'A',  '3', 'AVAILABLE', 65.00, 0),
  (gen_random_uuid(), event2, 'Upper Bowl', 'B',  '1', 'AVAILABLE', 60.00, 0),
  (gen_random_uuid(), event2, 'Upper Bowl', 'B',  '2', 'AVAILABLE', 60.00, 0),
  (gen_random_uuid(), event2, 'Upper Bowl', 'B',  '3', 'AVAILABLE', 60.00, 0),
  (gen_random_uuid(), event2, 'Lawn GA',    'GA', '1', 'AVAILABLE', 35.00, 0),
  (gen_random_uuid(), event2, 'Lawn GA',    'GA', '2', 'AVAILABLE', 35.00, 0),
  (gen_random_uuid(), event2, 'Lawn GA',    'GA', '3', 'AVAILABLE', 35.00, 0),
  (gen_random_uuid(), event2, 'Lawn GA',    'GA', '4', 'AVAILABLE', 35.00, 0),
  (gen_random_uuid(), event2, 'Lawn GA',    'GA', '5', 'AVAILABLE', 35.00, 0),
  (gen_random_uuid(), event2, 'Lawn GA',    'GA', '6', 'AVAILABLE', 35.00, 0);

-- Event 3: Stand-Up Comedy Showcase — simple, 2 sections, flat pricing, no queue, no cap
INSERT INTO events (id, title, start_time, organizer_id, venue_id, queue_required, queue_capacity, max_tickets_per_user, status, published_at, version)
VALUES (event3, 'Stand-Up Comedy Showcase', '2026-06-15 19:30:00+00', admin_id, venue_id, false, NULL, NULL, 'ACTIVE', now(), 0);

INSERT INTO seats (id, event_id, section, row_label, seat_number, status, price, version) VALUES
  (gen_random_uuid(), event3, 'Orchestra', 'A', '1', 'AVAILABLE', 45.00, 0),
  (gen_random_uuid(), event3, 'Orchestra', 'A', '2', 'AVAILABLE', 45.00, 0),
  (gen_random_uuid(), event3, 'Orchestra', 'A', '3', 'AVAILABLE', 45.00, 0),
  (gen_random_uuid(), event3, 'Orchestra', 'A', '4', 'AVAILABLE', 45.00, 0),
  (gen_random_uuid(), event3, 'Orchestra', 'B', '1', 'AVAILABLE', 45.00, 0),
  (gen_random_uuid(), event3, 'Orchestra', 'B', '2', 'AVAILABLE', 45.00, 0),
  (gen_random_uuid(), event3, 'Orchestra', 'B', '3', 'AVAILABLE', 45.00, 0),
  (gen_random_uuid(), event3, 'Orchestra', 'B', '4', 'AVAILABLE', 45.00, 0),
  (gen_random_uuid(), event3, 'Orchestra', 'C', '1', 'AVAILABLE', 45.00, 0),
  (gen_random_uuid(), event3, 'Orchestra', 'C', '2', 'AVAILABLE', 45.00, 0),
  (gen_random_uuid(), event3, 'Balcony',   'A', '1', 'AVAILABLE', 25.00, 0),
  (gen_random_uuid(), event3, 'Balcony',   'A', '2', 'AVAILABLE', 25.00, 0),
  (gen_random_uuid(), event3, 'Balcony',   'A', '3', 'AVAILABLE', 25.00, 0),
  (gen_random_uuid(), event3, 'Balcony',   'A', '4', 'AVAILABLE', 25.00, 0),
  (gen_random_uuid(), event3, 'Balcony',   'B', '1', 'AVAILABLE', 25.00, 0),
  (gen_random_uuid(), event3, 'Balcony',   'B', '2', 'AVAILABLE', 25.00, 0),
  (gen_random_uuid(), event3, 'Balcony',   'B', '3', 'AVAILABLE', 25.00, 0),
  (gen_random_uuid(), event3, 'Balcony',   'B', '4', 'AVAILABLE', 25.00, 0);

END $$;

SELECT e.title, e.status, e.max_tickets_per_user, e.queue_required,
       count(s.id) AS seat_count,
       min(s.price) AS min_price,
       max(s.price) AS max_price
FROM events e
LEFT JOIN seats s ON s.event_id = e.id
WHERE e.title IN ('Midnight Jazz Night', 'Summer Rock Festival', 'Stand-Up Comedy Showcase')
GROUP BY e.id, e.title, e.status, e.max_tickets_per_user, e.queue_required;

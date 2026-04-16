CREATE TABLE ticket_transfer_requests (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  ticket_id UUID NOT NULL REFERENCES tickets(id),
  from_user_id UUID NOT NULL REFERENCES users(id),
  to_user_id UUID NOT NULL REFERENCES users(id),
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  expires_at TIMESTAMPTZ NOT NULL,
  resolved_at TIMESTAMPTZ
);

CREATE INDEX idx_transfer_ticket ON ticket_transfer_requests(ticket_id);
CREATE INDEX idx_transfer_to_user ON ticket_transfer_requests(to_user_id);
CREATE INDEX idx_transfer_from_user ON ticket_transfer_requests(from_user_id);
CREATE INDEX idx_transfer_expires ON ticket_transfer_requests(expires_at) WHERE status = 'PENDING';

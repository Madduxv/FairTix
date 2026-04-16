ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Treat all pre-existing active accounts as already verified to avoid locking them out.
UPDATE users SET email_verified = TRUE WHERE deleted_at IS NULL;

CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_evt_token ON email_verification_tokens(token);
CREATE INDEX idx_evt_user_id ON email_verification_tokens(user_id);

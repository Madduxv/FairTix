CREATE TABLE notification_preferences (
    user_id         UUID PRIMARY KEY REFERENCES users(id),
    email_order     BOOLEAN NOT NULL DEFAULT TRUE,
    email_ticket    BOOLEAN NOT NULL DEFAULT TRUE,
    email_hold      BOOLEAN NOT NULL DEFAULT FALSE,
    email_marketing BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Create default preferences for all existing users
INSERT INTO notification_preferences (user_id)
SELECT id FROM users WHERE deleted_at IS NULL;

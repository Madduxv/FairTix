ALTER TABLE users
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

CREATE TABLE user_risk_scores (
    user_id      UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    score        INTEGER NOT NULL DEFAULT 0,
    tier         VARCHAR(20) NOT NULL DEFAULT 'LOW',
    flag_count   INTEGER NOT NULL DEFAULT 0,
    notes        TEXT,
    last_calculated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

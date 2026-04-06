CREATE TABLE IF NOT EXISTS audit_logs (
    id            UUID PRIMARY KEY,
    user_id       UUID         NOT NULL,
    action        VARCHAR(50)  NOT NULL,
    resource_type VARCHAR(50)  NOT NULL,
    resource_id   UUID,
    details       TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_user_id    ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_logs(created_at);

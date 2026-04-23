CREATE TABLE suspicious_flags (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID        NOT NULL REFERENCES users(id),
    flag_type     VARCHAR(50) NOT NULL,
    details       TEXT,
    severity      VARCHAR(20) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at   TIMESTAMPTZ,
    resolved_by   UUID
);

CREATE INDEX idx_suspicious_flags_user ON suspicious_flags(user_id);
CREATE INDEX idx_suspicious_flags_user_type_created ON suspicious_flags(user_id, flag_type, created_at);

CREATE INDEX idx_audit_user_created ON audit_logs(user_id, created_at);

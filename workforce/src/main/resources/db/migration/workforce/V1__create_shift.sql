-- Workforce context. Lives in the "workforce" schema (set by Flyway).
-- society_id and staff_id belong to other contexts: plain columns, no foreign keys.

CREATE SEQUENCE shift_id_seq;

CREATE TABLE shift (
    id          BIGINT       PRIMARY KEY,
    society_id  BIGINT       NOT NULL,
    staff_id    BIGINT       NOT NULL,
    started_at  TIMESTAMPTZ  NOT NULL,
    ended_at    TIMESTAMPTZ,
    CONSTRAINT ck_shift_ends_after_start CHECK (ended_at IS NULL OR ended_at >= started_at)
);

-- At most one open shift per staff member, even if two clock-ins race.
CREATE UNIQUE INDEX uq_shift_open_per_staff ON shift (society_id, staff_id) WHERE ended_at IS NULL;
CREATE INDEX ix_shift_society_started ON shift (society_id, started_at);

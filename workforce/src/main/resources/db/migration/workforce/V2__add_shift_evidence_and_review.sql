-- Clock-in and clock-out evidence: a live selfie (object key in the private bucket) and the phone's location, with
-- the result of checking it against the society's site. Selfie keys and coordinates are personal data: they are
-- cleared 90 days after the shift started; placement and distance stay. Shifts recorded before this have none.
ALTER TABLE shift
    ADD COLUMN start_selfie_key     VARCHAR(200),
    ADD COLUMN start_latitude       DOUBLE PRECISION,
    ADD COLUMN start_longitude      DOUBLE PRECISION,
    ADD COLUMN start_accuracy_m     DOUBLE PRECISION,
    ADD COLUMN start_placement      VARCHAR(20),
    ADD COLUMN start_distance_m     BIGINT,
    ADD COLUMN end_selfie_key       VARCHAR(200),
    ADD COLUMN end_latitude         DOUBLE PRECISION,
    ADD COLUMN end_longitude        DOUBLE PRECISION,
    ADD COLUMN end_accuracy_m       DOUBLE PRECISION,
    ADD COLUMN end_placement        VARCHAR(20),
    ADD COLUMN end_distance_m       BIGINT,
    -- an admin's verdict after comparing the selfies with the staff member; reviewer_id belongs to Identity
    ADD COLUMN review_decision      VARCHAR(20),
    ADD COLUMN reviewer_id          BIGINT,
    ADD COLUMN reviewed_at          TIMESTAMPTZ,
    ADD COLUMN review_reason        VARCHAR(500),
    ADD CONSTRAINT ck_shift_start_placement CHECK (start_placement IN ('INSIDE', 'OUTSIDE', 'NO_LOCATION')),
    ADD CONSTRAINT ck_shift_end_placement CHECK (end_placement IN ('INSIDE', 'OUTSIDE', 'NO_LOCATION')),
    ADD CONSTRAINT ck_shift_review CHECK (
        (review_decision IS NULL AND reviewer_id IS NULL AND reviewed_at IS NULL)
        OR (review_decision IN ('CONFIRMED', 'DISPUTED') AND reviewer_id IS NOT NULL AND reviewed_at IS NOT NULL));

-- the daily clean-up looks for old shifts that still hold selfies or coordinates
CREATE INDEX ix_shift_evidence_to_forget ON shift (started_at)
    WHERE start_selfie_key IS NOT NULL OR end_selfie_key IS NOT NULL
       OR start_latitude IS NOT NULL OR end_latitude IS NOT NULL;

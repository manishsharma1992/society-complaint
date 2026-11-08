-- Towers that handle complaints their own way (e.g. with their own complaint app) while the society has the
-- COMPLAINTS add-on on. A row means complaints are off for that tower; deleting it brings the tower on board.
CREATE TABLE society_complaints_off_tower (
    society_id  BIGINT  NOT NULL REFERENCES society (id),
    tower_id    BIGINT  NOT NULL REFERENCES tower (id),
    PRIMARY KEY (society_id, tower_id)
);

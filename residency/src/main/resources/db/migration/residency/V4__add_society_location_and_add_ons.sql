-- Where each society is (for the feed's "nearby" posts), and its add-ons. A row in society_add_on means the platform
-- owner made the add-on available to the society; enabled says whether the society switched it on.
ALTER TABLE society
    ADD COLUMN latitude  DOUBLE PRECISION,
    ADD COLUMN longitude DOUBLE PRECISION,
    ADD CONSTRAINT ck_society_location CHECK ((latitude IS NULL) = (longitude IS NULL));

CREATE TABLE society_add_on (
    society_id  BIGINT       NOT NULL REFERENCES society (id),
    add_on      VARCHAR(20)  NOT NULL CHECK (add_on IN ('FEED', 'COMPLAINTS')),
    enabled     BOOLEAN      NOT NULL,
    PRIMARY KEY (society_id, add_on)
);

-- Runwal MyCity (Betwade, Dombivli; plus code 7JFM53PF+CW) keeps everything it has today
UPDATE society SET latitude = 19.186062, longitude = 73.074812 WHERE id = 1;
INSERT INTO society_add_on (society_id, add_on, enabled)
SELECT id, add_on, TRUE FROM society, (VALUES ('FEED'), ('COMPLAINTS')) AS a(add_on) WHERE id = 1;

-- Automatic assignment. auto_assignment: a society's choice (no row = off). staff_skills_category: the complaint
-- categories a staff member handles; staff_id belongs to the Identity context, so it has no foreign key.
CREATE TABLE auto_assignment (
    society_id  BIGINT   PRIMARY KEY,
    enabled     BOOLEAN  NOT NULL
);

CREATE TABLE staff_skills_category (
    society_id   BIGINT  NOT NULL,
    staff_id     BIGINT  NOT NULL,
    category_id  BIGINT  NOT NULL REFERENCES complaint_category (id),
    PRIMARY KEY (society_id, staff_id, category_id)
);

-- Runwal MyCity starts with automatic assignment on; it assigns nothing until staff have categories.
INSERT INTO auto_assignment (society_id, enabled) VALUES (1, TRUE);

-- Society 1 (the same ID the complaint schema already uses) with its 10 towers and 1,036 flats.
-- Floors count from the first residential floor (the 3 parking levels are not numbered), 4 flats per floor:
-- 27 residential floors everywhere except C4 Vega (19 floors - 3 parking = 16). Flat 503 = floor 5, unit 3.

INSERT INTO society (id, name) VALUES (1, 'Runwal MyCity');

INSERT INTO tower (id, society_id, code, name, residential_floors) VALUES
    (1, 1, 'A1', 'Astoria', 27),
    (2, 1, 'C1', 'Celestia', 27),
    (3, 1, 'A2', 'Aster', 27),
    (4, 1, 'C2', 'Cosmos', 27),
    (5, 1, 'A3', 'Orion', 27),
    (6, 1, 'C3', 'Sirius', 27),
    (7, 1, 'B2', 'Radiant', 27),
    (8, 1, 'C4', 'Vega', 16),
    (9, 1, 'B1', 'Alpha', 27),
    (10, 1, 'A4', 'Polaris', 27);

-- explicit IDs above: move the identity sequences past them
SELECT setval(pg_get_serial_sequence('society', 'id'), (SELECT max(id) FROM society));
SELECT setval(pg_get_serial_sequence('tower', 'id'), (SELECT max(id) FROM tower));

INSERT INTO flat (society_id, tower_id, floor, unit)
SELECT t.society_id, t.id, f.floor, u.unit
FROM tower t
CROSS JOIN LATERAL generate_series(1, t.residential_floors) AS f (floor)
CROSS JOIN generate_series(1, 4) AS u (unit)
ORDER BY t.id, f.floor, u.unit;

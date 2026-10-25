-- The demo resident (identity user 4) owns B2-503. Delete with the demo users before going live.
INSERT INTO flat_member (flat_id, user_id, type, since)
SELECT f.id, 4, 'OWNER', now()
FROM flat f JOIN tower t ON t.id = f.tower_id
WHERE t.society_id = 1 AND t.code = 'B2' AND f.floor = 5 AND f.unit = 3;

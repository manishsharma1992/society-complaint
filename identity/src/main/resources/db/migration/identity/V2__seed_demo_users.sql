-- Demo users for showing the app before real SMS works. The numbers are placeholders: no SMS is ever sent to them,
-- they log in with the fixed OTP_DEMO_CODE set on the server. Delete them (and the demo code) before going live.
-- IDs 1-4 are below the sequence start (100), so they never clash with real users.

INSERT INTO app_user (id, phone, name, status, created_at) VALUES
    (1, '+919000000001', 'Demo Society Admin', 'ACTIVE', now()),
    (2, '+919000000002', 'Demo Tower Admin B2', 'ACTIVE', now()),
    (3, '+919000000003', 'Demo Staff', 'ACTIVE', now()),
    (4, '+919000000004', 'Demo Resident B2-503', 'ACTIVE', now());

-- society 1 = Runwal MyCity, tower 7 = B2 Radiant (residency seed)
INSERT INTO user_role (user_id, role, society_id, tower_id) VALUES
    (1, 'SOCIETY_ADMIN', 1, NULL),
    (2, 'TOWER_ADMIN', 1, 7),
    (3, 'STAFF', 1, NULL),
    (4, 'RESIDENT', 1, NULL);

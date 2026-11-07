-- Whoever runs the platform itself, above any society: decides which add-ons (feed, complaints) a society may use.
-- Set here only; the app has no way to grant it. For now the demo society admin (user 1) holds it, for building and
-- testing; move it to the owner's own account before real residents join.
ALTER TABLE app_user ADD COLUMN platform_owner BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE app_user SET platform_owner = TRUE WHERE id = 1 AND phone = '+919000000001';

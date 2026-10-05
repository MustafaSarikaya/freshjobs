-- Admin accounts: remove the previous owner's @halitdincer.com accounts and
-- make mustafatayyard@gmail.com a superuser. The password is stored as a Django-compatible
-- PBKDF2-SHA256 hash; the plaintext is not in the repo.

DO $$
BEGIN
    -- Leftover Django tables reference auth_user without ON DELETE CASCADE.
    IF to_regclass('django_admin_log') IS NOT NULL THEN
        DELETE FROM django_admin_log WHERE user_id IN (
            SELECT id FROM auth_user WHERE lower(email) LIKE '%@halitdincer.com');
    END IF;
    IF to_regclass('auth_user_groups') IS NOT NULL THEN
        DELETE FROM auth_user_groups WHERE user_id IN (
            SELECT id FROM auth_user WHERE lower(email) LIKE '%@halitdincer.com');
    END IF;
    IF to_regclass('auth_user_user_permissions') IS NOT NULL THEN
        DELETE FROM auth_user_user_permissions WHERE user_id IN (
            SELECT id FROM auth_user WHERE lower(email) LIKE '%@halitdincer.com');
    END IF;
END $$;

DELETE FROM auth_user WHERE lower(email) LIKE '%@halitdincer.com';

UPDATE auth_user
SET password     = 'pbkdf2_sha256$870000$xXKYy6FcZIJ0mAQu1PiNrN$jIxlyiqCLutAWYBvunqS2gv03rFfc7uk75BcCAWjSgw=',
    is_staff     = TRUE,
    is_superuser = TRUE,
    is_active    = TRUE
WHERE lower(email) = 'mustafatayyard@gmail.com' OR username = 'mustafatayyard@gmail.com';

INSERT INTO auth_user (password, last_login, is_superuser, username, first_name,
                       last_name, email, is_staff, is_active, date_joined)
SELECT 'pbkdf2_sha256$870000$xXKYy6FcZIJ0mAQu1PiNrN$jIxlyiqCLutAWYBvunqS2gv03rFfc7uk75BcCAWjSgw=', NULL, TRUE, 'mustafatayyard@gmail.com', '', '', 'mustafatayyard@gmail.com', TRUE, TRUE, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM auth_user WHERE lower(email) = 'mustafatayyard@gmail.com' OR username = 'mustafatayyard@gmail.com');

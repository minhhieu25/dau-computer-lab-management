-- V2__seed.sql
-- Dev seed: exactly one admin account + default system settings (BR-09, BR-10).
-- The seed password is a dev-only value (Admin@123) and MUST be changed at first login.
-- Hash format: pbkdf2-sha256$<iterations>$<salt-b64>$<hash-b64>
--   algorithm PBKDF2WithHmacSHA256, 210000 iterations, 16-byte salt, 32-byte key.

INSERT INTO app_user (username, password_hash, full_name, role, status)
VALUES ('admin',
        'pbkdf2-sha256$210000$+EusXJioUVXKkKepIZMqIw==$f+PowvsAVLX1OVRtQ0Qeqe57dE2Q9lHdEzqO/LX24gI=',
        'System Administrator',
        'admin',
        'active');

INSERT INTO system_setting (system_setting_id, retention_days, updated_by)
VALUES (1, 30, (SELECT user_id FROM app_user WHERE username = 'admin'));

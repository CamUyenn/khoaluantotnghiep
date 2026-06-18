-- Dynamic key-value system settings for PostgreSQL
-- Run once on database KLTN before starting backend.

CREATE TABLE IF NOT EXISTS system_settings (
    setting_key VARCHAR(120) PRIMARY KEY,
    setting_value TEXT NOT NULL DEFAULT '',
    description TEXT NOT NULL DEFAULT ''
);

ALTER TABLE system_settings
    ADD COLUMN IF NOT EXISTS description TEXT NOT NULL DEFAULT '';

-- Optional baseline keys for admin settings screen.
INSERT INTO system_settings (setting_key, setting_value, description)
VALUES
    ('spring.mail.host', 'smtp.gmail.com', 'Địa chỉ máy chủ SMTP dùng để gửi email.'),
    ('spring.mail.port', '587', 'Cổng SMTP của máy chủ email (ví dụ: 587).'),
    ('spring.mail.username', '', 'Tài khoản email gửi đi của hệ thống.'),
    ('spring.mail.password', '', 'Mật khẩu ứng dụng của tài khoản email gửi đi.'),
    ('clinic.notification.receptionist.emails', '', 'Danh sách email lễ tân nhận thông báo, phân tách bằng dấu phẩy.'),
    ('APP_LOG_MAX_FILE_SIZE', '10MB', 'Kích thước tối đa cho mỗi file log (ví dụ: 10MB).'),
    ('APP_LOG_MAX_HISTORY_DAYS', '14', 'Số ngày tối đa giữ lại log file (ví dụ: 14).')
ON CONFLICT (setting_key) DO NOTHING;

UPDATE system_settings
SET description = 'Địa chỉ máy chủ SMTP dùng để gửi email.'
WHERE setting_key = 'spring.mail.host' AND COALESCE(description, '') = '';

UPDATE system_settings
SET description = 'Cổng SMTP của máy chủ email (ví dụ: 587).'
WHERE setting_key = 'spring.mail.port' AND COALESCE(description, '') = '';

UPDATE system_settings
SET description = 'Tài khoản email gửi đi của hệ thống.'
WHERE setting_key = 'spring.mail.username' AND COALESCE(description, '') = '';

UPDATE system_settings
SET description = 'Mật khẩu ứng dụng của tài khoản email gửi đi.'
WHERE setting_key = 'spring.mail.password' AND COALESCE(description, '') = '';

UPDATE system_settings
SET description = 'Danh sách email lễ tân nhận thông báo, phân tách bằng dấu phẩy.'
WHERE setting_key = 'clinic.notification.receptionist.emails' AND COALESCE(description, '') = '';

UPDATE system_settings
SET description = 'Kích thước tối đa cho mỗi file log (ví dụ: 10MB).'
WHERE setting_key = 'APP_LOG_MAX_FILE_SIZE' AND COALESCE(description, '') = '';

UPDATE system_settings
SET description = 'Số ngày tối đa giữ lại log file (ví dụ: 14).'
WHERE setting_key = 'APP_LOG_MAX_HISTORY_DAYS' AND COALESCE(description, '') = '';

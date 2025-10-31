-- Tạo user dev (nếu chưa có)
CREATE USER IF NOT EXISTS 'devuser'@'%' IDENTIFIED BY '123456';

-- Cấp toàn quyền cho user dev (phục vụ môi trường dev/test)
GRANT ALL PRIVILEGES ON *.* TO 'devuser'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;

-- Tạo database (nếu chưa tồn tại)
CREATE DATABASE IF NOT EXISTS social_media
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE social_media;

-- Tạo bảng persistent_logins (Spring Security Remember Me)
CREATE TABLE IF NOT EXISTS persistent_logins (
    username VARCHAR(64) NOT NULL,
    series VARCHAR(64) PRIMARY KEY,
    token VARCHAR(64) NOT NULL,
    last_used TIMESTAMP NOT NULL
);

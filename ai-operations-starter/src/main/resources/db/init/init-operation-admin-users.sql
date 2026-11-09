-- 人工执行：初始化两个系统运营管理员。
-- 请勿放入 db/migration 避免应用启动时自动创建管理员账号。
-- 密码仅保存 BCrypt 哈希；首次登录后应立即修改初始密码。

START TRANSACTION;

INSERT INTO `operation_user` (
    `id`, `user_id`, `mobile`, `username`, `password_hash`, `name`, `status`, `valid`, `version`
) VALUES
(2104843632644194305, 'ops_01a15e42-3763-7b67-9599-10f2172eecab', '13780527645', 'system-admin-01', '$2y$10$B7WJ10KBRyj6C1gfwC7pcO/Gd.gAP8u1a0RHWgn5VzQmOrSriGaXG', '系统管理员 01', 1, 1, 0),
(2104843632644194306, 'ops_01a15e42-5a32-7940-b925-41cf8d8b63bc', '13780527646', 'system-admin-02', '$2y$10$0GaBWTgXLCdggeicoR/ZYOc9kfekJrYuq3O9u2ER.aRzw55LFvmMy', '系统管理员 02', 1, 1, 0);

INSERT INTO `operation_user_identity` (
    `id`, `user_id`, `identity_type`, `identity_provider`, `identity_value`, `verified_at`, `valid`, `version`
) VALUES
(2104843632644194307, 'ops_01a15e42-3763-7b67-9599-10f2172eecab', 'ACCOUNT', 'LOCAL', 'system-admin-01', NOW(), 1, 0),
(2104843632644194308, 'ops_01a15e42-3763-7b67-9599-10f2172eecab', 'MOBILE', 'SMS', '13780527645', NOW(), 1, 0),
(2104843632644304307, 'ops_01a15e42-5a32-7940-b925-41cf8d8b63bc', 'ACCOUNT', 'LOCAL', 'system-admin-02', NOW(), 1, 0),
(2104843632644304308, 'ops_01a15e42-5a32-7940-b925-41cf8d8b63bc', 'MOBILE', 'SMS', '13780527646', NOW(), 1, 0);

INSERT INTO `operation_user_role` (
    `id`, `user_id`, `operation_role_id`, `effective_start_time`, `valid`, `version`
) VALUES
(2104843632644194309, 'ops_01a15e42-3763-7b67-9599-10f2172eecab', 1, NOW(), 1, 0),
(2104843632644304309, 'ops_01a15e42-5a32-7940-b925-41cf8d8b63bc', 1, NOW(), 1, 0);

COMMIT;


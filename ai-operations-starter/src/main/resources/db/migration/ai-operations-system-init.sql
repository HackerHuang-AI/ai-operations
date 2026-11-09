CREATE DATABASE IF NOT EXISTS `ai_operations` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `ai_operations`;

# 模块接口注册表 各模块维护自己的接口
CREATE TABLE `api_interface` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '接口注册主键 ID',
    `module_name` VARCHAR(128) NOT NULL COMMENT '模块应用名 取 spring.application.name',
    `http_method` VARCHAR(16) NOT NULL COMMENT 'HTTP 请求方法',
    `path` VARCHAR(256) COLLATE utf8mb4_bin NOT NULL COMMENT '服务内部接口路径或路径参数模板 不含网关模块前缀',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '接口业务说明',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识 1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_api_interface_module_method_path` (`module_name`, `http_method`, `path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块接口注册表';

# ai-operations 前端 HTTP 接口初始化
INSERT INTO `api_interface` (`id`, `module_name`, `http_method`, `path`, `description`) VALUES
(1, 'ai-operations', 'POST', '/api/access/login', '运营人员登录，返回运营会话令牌、会话 ID 及用户 ID'),
(2, 'ai-operations', 'GET', '/api/access/sessions', '查询当前运营用户登录会话列表'),
(3, 'ai-operations', 'POST', '/api/access/logout', '退出当前运营登录会话'),
(4, 'ai-operations', 'POST', '/api/access/sessions/logout', '下线当前运营用户的指定登录会话'),
(5, 'ai-operations', 'POST', '/api/access/logout-all', '退出当前运营用户全部登录会话'),
(6, 'ai-operations', 'POST', '/api/access/sms-code/send', '发送运营登录短信验证码'),
(7, 'ai-operations', 'POST', '/api/user/menu/tree', '查询当前运营用户角色授权的导航菜单树');

# 运营端用户表 登录字段与 ai-base.base_user 保持一致
CREATE TABLE `operation_user` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '本地物理主键',
    `user_id` VARCHAR(128) NOT NULL COMMENT '全局用户 ID',
    `mobile` VARCHAR(32) NOT NULL COMMENT '手机号',
    `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `username` VARCHAR(64) DEFAULT NULL COMMENT '账号名',
    `password_hash` VARCHAR(100) DEFAULT NULL COMMENT 'BCrypt 密码哈希',
    `name` VARCHAR(64) DEFAULT NULL COMMENT '用户姓名',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常 2-禁用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_operation_user_id` (`user_id`),
    UNIQUE KEY `uk_operation_user_mobile` (`mobile`),
    UNIQUE KEY `uk_operation_user_email` (`email`),
    UNIQUE KEY `uk_operation_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营端用户表';

# 运营端用户身份标识表 结构与 ai-base.user_identity 保持一致
CREATE TABLE `operation_user_identity` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键 ID',
    `user_id` VARCHAR(128) NOT NULL COMMENT '全局用户 ID',
    `identity_type` VARCHAR(32) NOT NULL COMMENT '身份类型',
    `identity_provider` VARCHAR(64) NOT NULL DEFAULT 'LOCAL' COMMENT '身份提供方',
    `identity_value` VARCHAR(256) NOT NULL COMMENT '身份标识值',
    `verified_at` DATETIME DEFAULT NULL COMMENT '认证通过时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_operation_identity_type_provider_value` (`identity_type`, `identity_provider`, `identity_value`),
    KEY `idx_operation_identity_user_id` (`user_id`),
    CONSTRAINT `fk_operation_user_identity_user` FOREIGN KEY (`user_id`) REFERENCES `operation_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营端用户身份标识表';

# 运营端登录审计表 字段与 ai-base.login_audit 保持一致
CREATE TABLE `operation_login_audit` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键 ID',
    `user_id` VARCHAR(128) DEFAULT NULL COMMENT '全局用户 ID',
    `tenant_id` VARCHAR(128) DEFAULT NULL COMMENT '全局租户 ID 运营端登录时为空',
    `identity_type` VARCHAR(32) NOT NULL COMMENT '身份类型',
    `identity_value_hash` CHAR(64) NOT NULL COMMENT '身份标识值 SHA-256 摘要',
    `login_result` TINYINT NOT NULL COMMENT '登录结果：1-成功 2-失败',
    `failure_code` VARCHAR(64) DEFAULT NULL COMMENT '失败码',
    `login_ip` VARCHAR(64) DEFAULT NULL COMMENT '登录 IP',
    `user_agent` VARCHAR(512) DEFAULT NULL COMMENT '用户代理',
    `login_at` DATETIME NOT NULL COMMENT '登录时间',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营端登录审计表';

# 运营端菜单表 系统级
CREATE TABLE `operation_menu` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '运营菜单 ID',
    `menu_code` VARCHAR(128) NOT NULL COMMENT '菜单编码 同时作为权限标识',
    `menu_name` VARCHAR(128) NOT NULL COMMENT '菜单名称',
    `desc` VARCHAR(256) DEFAULT NULL COMMENT '菜单描述',
    `resource_type` TINYINT NOT NULL COMMENT '资源类型：1-目录 2-页面 3-按钮',
    `route_path` VARCHAR(256) DEFAULT NULL COMMENT '前端路由路径 仅页面资源使用',
    `parent_menu_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '父菜单 ID NULL 表示根菜单',
    `men_path` VARCHAR(512) NOT NULL COMMENT '菜单编码路径 使用分号分隔',
    `menu_level` TINYINT UNSIGNED NOT NULL COMMENT '菜单层级：根目录为 0 依次递增',
    `icon` VARCHAR(128) DEFAULT NULL COMMENT '菜单图标标识',
    `sort_no` INT NOT NULL DEFAULT 0 COMMENT '同级菜单排序值 升序排列',
    `deletable` TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许删除：1-允许 0-不允许',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用 2-停用',
    `creator_user_id` VARCHAR(128) DEFAULT NULL COMMENT '创建人全局用户 ID 系统初始化为空',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater_user_id` VARCHAR(128) DEFAULT NULL COMMENT '最后修改人全局用户 ID 系统初始化为空',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_menu_code` (`menu_code`),
    KEY `idx_parent_menu_sort` (`parent_menu_id`, `sort_no`, `id`),
    CONSTRAINT `fk_operation_menu_parent` FOREIGN KEY (`parent_menu_id`) REFERENCES `operation_menu` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营端菜单表';

# 运营端菜单初始化 menu_code 是稳定业务标识 parent_menu_id 表示树关系 men_path 记录当前路径
INSERT INTO `operation_menu` (
    `id`, `menu_code`, `menu_name`, `desc`, `resource_type`, `route_path`, `parent_menu_id`, `men_path`, `menu_level`, `sort_no`, `deletable`, `status`, `valid`, `version`
) VALUES
(1, 'ROOT', '根目录', '运营端导航菜单的根节点。', 1, NULL, NULL, 'ROOT', 0, 0, 0, 1, 1, 0),
(2, 'OPS_HOME', '首页', '运营端首页入口。', 2, '/home', 1, 'ROOT;OPS_HOME', 1, 1, 0, 1, 1, 0),
(3, 'OPS_TASK', '任务中心', '查看和管理运营任务。', 2, '/tasks', 1, 'ROOT;OPS_TASK', 1, 2, 0, 1, 1, 0),
(4, 'OPS_ADMIN', '运营管理', '运营用户、角色与菜单管理入口。', 1, NULL, 1, 'ROOT;OPS_ADMIN', 1, 3, 0, 1, 1, 0),
(5, 'OPS_USER_MANAGE', '运营用户管理', '管理运营端用户。', 2, '/operations/users', 4, 'ROOT;OPS_ADMIN;OPS_USER_MANAGE', 2, 1, 0, 1, 1, 0),
(6, 'OPS_ROLE_MANAGE', '运营角色管理', '管理运营端角色。', 2, '/operations/roles', 4, 'ROOT;OPS_ADMIN;OPS_ROLE_MANAGE', 2, 2, 0, 1, 1, 0),
(7, 'OPS_MENU_MANAGE', '运营菜单管理', '管理运营端菜单。', 2, '/operations/menus', 4, 'ROOT;OPS_ADMIN;OPS_MENU_MANAGE', 2, 3, 0, 1, 1, 0),
(8, 'TREASURY_OPS', '司库运营', '用户菜单与租户管理入口。', 1, NULL, 1, 'ROOT;TREASURY_OPS', 1, 4, 0, 1, 1, 0),
(9, 'TREASURY_MENU', '用户菜单管理', '管理用户端菜单。', 2, '/treasury/user-menus', 8, 'ROOT;TREASURY_OPS;TREASURY_MENU', 2, 1, 0, 1, 1, 0),
(10, 'TREASURY_TENANT', '租户管理', '管理租户信息。', 2, '/treasury/tenants', 8, 'ROOT;TREASURY_OPS;TREASURY_TENANT', 2, 2, 0, 1, 1, 0),
(11, 'KNOWLEDGE_OPS', '智库运营', '智库运营功能入口。', 1, NULL, 1, 'ROOT;KNOWLEDGE_OPS', 1, 5, 0, 1, 1, 0);

# 运营角色表 系统级
CREATE TABLE `operation_role` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '运营角色 ID',
    `role_code` VARCHAR(64) NOT NULL COMMENT '运营角色编码',
    `role_name` VARCHAR(64) NOT NULL COMMENT '运营角色名称',
    `description` VARCHAR(256) DEFAULT NULL COMMENT '角色说明',
    `deletable` TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许删除：1-允许 0-不允许',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用 2-停用',
    `creator_user_id` VARCHAR(128) DEFAULT NULL COMMENT '创建人全局用户 ID 系统初始化为空',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater_user_id` VARCHAR(128) DEFAULT NULL COMMENT '最后修改人全局用户 ID 系统初始化为空',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营角色表';

# 系统内置运营超级管理员角色
INSERT INTO `operation_role` (
    `id`, `role_code`, `role_name`, `description`, `deletable`, `status`, `valid`, `version`
) VALUES
(1, 'OPERATION_SUPER_ADMIN', '运营超级管理员', '拥有运营端全部菜单及功能权限', 0, 1, 1, 0);

# 运营用户角色关联表
CREATE TABLE `operation_user_role` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键 ID',
    `user_id` VARCHAR(128) NOT NULL COMMENT '全局唯一用户 ID 按 ops_ 前缀加 UUID v7 生成',
    `operation_role_id` BIGINT UNSIGNED NOT NULL COMMENT '运营角色 ID',
    `effective_start_time` DATETIME NOT NULL COMMENT '授权生效时间',
    `effective_end_time` DATETIME DEFAULT NULL COMMENT '授权失效时间 NULL 表示当前有效',
    `creator_user_id` VARCHAR(128) DEFAULT NULL COMMENT '创建人全局用户 ID 系统初始化为空',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater_user_id` VARCHAR(128) DEFAULT NULL COMMENT '最后修改人全局用户 ID 系统初始化为空',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_operation_user_role_valid` (`user_id`, `operation_role_id`, `valid`),
    KEY `idx_operation_role_id` (`operation_role_id`),
    KEY `idx_operation_user_role_user` (`user_id`),
    CONSTRAINT `fk_operation_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `operation_user` (`user_id`),
    CONSTRAINT `fk_operation_user_role_role` FOREIGN KEY (`operation_role_id`) REFERENCES `operation_role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营用户角色关联表';

# 运营角色菜单关联表
CREATE TABLE `operation_role_menu` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键 ID',
    `operation_role_id` BIGINT UNSIGNED NOT NULL COMMENT '运营角色 ID',
    `operation_menu_id` BIGINT UNSIGNED NOT NULL COMMENT '运营菜单 ID',
    `include_descendants` TINYINT NOT NULL DEFAULT 0 COMMENT '是否继承子菜单权限：1-是 0-否',
    `deletable` TINYINT NOT NULL DEFAULT 1 COMMENT '该角色菜单关联是否允许删除：1-允许 0-不允许',
    `creator_user_id` VARCHAR(128) DEFAULT NULL COMMENT '创建人全局用户 ID 系统初始化为空',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater_user_id` VARCHAR(128) DEFAULT NULL COMMENT '最后修改人全局用户 ID 系统初始化为空',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_operation_role_menu_valid` (`operation_role_id`, `operation_menu_id`, `valid`),
    KEY `idx_operation_menu_id` (`operation_menu_id`),
    CONSTRAINT `fk_operation_role_menu_role` FOREIGN KEY (`operation_role_id`) REFERENCES `operation_role` (`id`),
    CONSTRAINT `fk_operation_role_menu_menu` FOREIGN KEY (`operation_menu_id`) REFERENCES `operation_menu` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营角色菜单关联表';

# 运营超级管理员默认关联根目录并继承全部子菜单 该关系不可删除
INSERT INTO `operation_role_menu` (
    `id`, `operation_role_id`, `operation_menu_id`, `include_descendants`, `deletable`, `valid`, `version`
) VALUES
(1, 1, 1, 1, 0, 1, 0);


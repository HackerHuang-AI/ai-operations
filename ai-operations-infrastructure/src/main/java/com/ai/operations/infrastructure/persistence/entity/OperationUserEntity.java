package com.ai.operations.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("operation_user")
public class OperationUserEntity extends VersionedEntity {
    /** 运营用户全局 ID，使用 ops_ 前缀加 UUID v7。 */
    private String userId;
    /** 手机号。 */
    private String mobile;
    /** 邮箱。 */
    private String email;
    /** 登录账号名。 */
    private String username;
    /** BCrypt 密码哈希。 */
    private String passwordHash;
    /** 用户姓名。 */
    private String name;
    /** 用户状态：1-正常，2-禁用。 */
    private Integer status;
}


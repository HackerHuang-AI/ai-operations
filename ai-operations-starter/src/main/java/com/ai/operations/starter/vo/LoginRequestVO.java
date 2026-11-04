package com.ai.operations.starter.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestVO {
    /** 登录方式：ACCOUNT_PASSWORD-账号密码，MOBILE_SMS-短信验证码。 */
    @NotBlank(message = "3101101")
    private String loginType;
    /** 登录账号名，账号密码登录时填写。 */
    private String account;
    /** 登录密码，账号密码登录时填写。 */
    private String password;
    /** 手机号，短信验证码登录时填写。 */
    private String mobile;
    /** 短信验证码，短信验证码登录时填写。 */
    private String smsCode;
    /** 客户端设备标识，16～128 位字母、数字、下划线或连字符。 */
    @NotBlank(message = "3101107")
    @Pattern(regexp = "^[A-Za-z0-9_-]{16,128}$", message = "3101108")
    private String deviceId;
}


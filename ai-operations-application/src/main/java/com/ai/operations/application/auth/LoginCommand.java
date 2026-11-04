package com.ai.operations.application.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginCommand {
    /** 登录方式：ACCOUNT_PASSWORD-账号密码，MOBILE_SMS-短信验证码。 */
    private String loginType;
    /** 登录账号名，账号密码登录时使用。 */
    private String account;
    /** 登录密码，账号密码登录时使用。 */
    private String password;
    /** 手机号，短信验证码登录时使用。 */
    private String mobile;
    /** 短信验证码。 */
    private String smsCode;
    /** 客户端设备标识，用于同设备会话替换和登录设备数限制。 */
    private String deviceId;
    /** 登录来源 IP，由接口层从 HTTP 请求中获取。 */
    private String loginIp;
    /** 客户端 User-Agent，由接口层从 HTTP 请求头中获取。 */
    private String userAgent;
}


package com.ai.operations.starter.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendSmsCodeRequestVO {
    /** 接收登录短信验证码的手机号。 */
    @NotBlank(message = "3101109")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "3001004")
    private String mobile;
}


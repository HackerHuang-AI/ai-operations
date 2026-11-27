package com.ai.operations.starter.controller;

import com.ai.operations.application.auth.LoginCommand;
import com.ai.operations.application.auth.LoginResult;
import com.ai.operations.application.auth.SessionInfo;
import com.ai.operations.application.service.LoginService;
import com.ai.operations.application.service.SmsCodeService;
import com.ai.operations.starter.common.Result;
import com.ai.operations.starter.vo.LoginRequestVO;
import com.ai.operations.starter.vo.LogoutSessionRequestVO;
import com.ai.operations.starter.vo.SendSmsCodeRequestVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/access")
public class MockLoginController {
    private final LoginService loginService;
    private final SmsCodeService smsCodeService;

    public MockLoginController(LoginService loginService, SmsCodeService smsCodeService) {
        this.loginService = loginService;
        this.smsCodeService = smsCodeService;
    }

    @PostMapping("/login")
    public Result<LoginResult> login(@Valid @RequestBody LoginRequestVO request, HttpServletRequest servletRequest) {
        LoginCommand command = new LoginCommand();
        command.setLoginType(request.getLoginType());
        command.setAccount(request.getAccount());
        command.setPassword(request.getPassword());
        command.setMobile(request.getMobile());
        command.setSmsCode(request.getSmsCode());
        command.setDeviceId(request.getDeviceId());
        command.setLoginIp(resolveClientIp(servletRequest));
        command.setUserAgent(servletRequest.getHeader("User-Agent"));
        return Result.success(loginService.login(command));
    }

    @GetMapping("/sessions")
    public Result<List<SessionInfo>> sessions() {
        return Result.success(loginService.listSessions());
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        loginService.logout();
        return Result.success(null);
    }

    @PostMapping("/sessions/logout")
    public Result<Void> logoutSession(@Valid @RequestBody LogoutSessionRequestVO request) {
        loginService.logout(request.getSessionId());
        return Result.success(null);
    }

    @PostMapping("/logout-all")
    public Result<Void> logoutAll() {
        loginService.logoutAll();
        return Result.success(null);
    }

    @PostMapping("/sms-code/send")
    public Result<Void> sendSmsCode(@Valid @RequestBody SendSmsCodeRequestVO request) {
        smsCodeService.sendLoginCode(request.getMobile());
        return Result.success(null);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            String clientIp = forwardedFor.split(",", 2)[0].trim();
            if (StringUtils.hasText(clientIp)) {
                return clientIp;
            }
        }
        return request.getRemoteAddr();
    }
}


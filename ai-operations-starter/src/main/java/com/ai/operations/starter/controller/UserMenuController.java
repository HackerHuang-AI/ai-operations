package com.ai.operations.starter.controller;

import com.ai.operations.application.bo.OperationMenuBO;
import com.ai.operations.application.common.BizException;
import com.ai.operations.application.enums.ErrorCodeEnum;
import com.ai.operations.application.model.request.RequestContext;
import com.ai.operations.application.model.request.RequestContextHolder;
import com.ai.operations.application.service.OperationMenuService;
import com.ai.operations.starter.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/user/menu")
public class UserMenuController {
    private final OperationMenuService operationMenuService;

    public UserMenuController(OperationMenuService operationMenuService) {
        this.operationMenuService = operationMenuService;
    }

    @PostMapping("/tree")
    public Result<List<OperationMenuBO>> tree() {
        try {
            String userId = RequestContextHolder.current().map(RequestContext::userId)
                    .filter(StringUtils::hasText)
                    .orElseThrow(() -> new IllegalStateException("current user is unavailable"));
            return Result.success(operationMenuService.tree(userId));
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("查询当前运营用户可见导航菜单失败", exception);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }
}


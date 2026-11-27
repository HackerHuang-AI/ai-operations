package com.ai.operations.starter.controller;

import com.ai.operations.application.bo.OperationMenuBO;
import com.ai.operations.application.model.request.RequestContext;
import com.ai.operations.application.model.request.RequestContextHolder;
import com.ai.operations.application.service.OperationMenuService;
import com.ai.operations.starter.common.Result;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/menu")
public class UserMenuController {
    private final OperationMenuService operationMenuService;

    public UserMenuController(OperationMenuService operationMenuService) {
        this.operationMenuService = operationMenuService;
    }

    @PostMapping("/tree")
    public Result<List<OperationMenuBO>> tree() {
        String userId = RequestContextHolder.current().map(RequestContext::userId)
                .filter(StringUtils::hasText)
                .orElseThrow(() -> new IllegalStateException("current user is unavailable"));
        return Result.success(operationMenuService.tree(userId));
    }
}


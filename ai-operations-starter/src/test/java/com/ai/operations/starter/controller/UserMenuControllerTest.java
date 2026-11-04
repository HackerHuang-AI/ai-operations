package com.ai.operations.starter.controller;

import com.ai.operations.application.bo.OperationMenuBO;
import com.ai.operations.application.common.BizException;
import com.ai.operations.application.enums.ErrorCodeEnum;
import com.ai.operations.application.model.request.RequestContext;
import com.ai.operations.application.model.request.RequestContextHolder;
import com.ai.operations.application.service.OperationMenuService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserMenuControllerTest {
    private static final String USER_ID = "ops-user-1";
    private OperationMenuService service;
    private UserMenuController controller;

    @BeforeEach
    void setUp() {
        service = mock(OperationMenuService.class);
        controller = new UserMenuController(service);
        context(USER_ID);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.clear();
    }

    @Test
    void postWithoutBodyReturnsExactlyEightNavigationFields() throws Exception {
        OperationMenuBO node = new OperationMenuBO();
        node.setMenuCode("OPS_HOME");
        node.setResourceName("首页");
        node.setDesc("运营端首页入口。");
        node.setResourceType(2);
        node.setRoutePath("/home");
        node.setIcon("home");
        node.setSortNo(1);
        when(service.tree(USER_ID)).thenReturn(List.of(node));

        var response = MockMvcBuilders.standaloneSetup(controller).build()
                .perform(post("/api/user/menu/tree")).andExpect(status().isOk()).andReturn();
        var json = new ObjectMapper().readTree(response.getResponse().getContentAsByteArray());
        var menu = json.get("data").get(0);

        assertThat(menu.size()).isEqualTo(8);
        assertThat(menu.get("menuCode").asText()).isEqualTo("OPS_HOME");
        assertThat(menu.get("resourceName").asText()).isEqualTo("首页");
        assertThat(menu.get("desc").asText()).isEqualTo("运营端首页入口。");
        assertThat(menu.get("resourceType").asInt()).isEqualTo(2);
        assertThat(menu.get("routePath").asText()).isEqualTo("/home");
        assertThat(menu.get("icon").asText()).isEqualTo("home");
        assertThat(menu.get("sortNo").asInt()).isEqualTo(1);
        assertThat(menu.get("children").isArray()).isTrue();
        assertThat(menu.get("children").size()).isZero();
        verify(service).tree(USER_ID);
        verifyNoMoreInteractions(service);
    }

    @Test
    void frontendUserIdCannotOverrideRequestContext() throws Exception {
        when(service.tree(USER_ID)).thenReturn(List.of());

        MockMvcBuilders.standaloneSetup(controller).build()
                .perform(post("/api/user/menu/tree").param("userId", "other-user")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"other-user\"}"))
                .andExpect(status().isOk());

        verify(service).tree(USER_ID);
        verifyNoMoreInteractions(service);
    }

    @Test
    void noMenusReturnsEmptyList() {
        when(service.tree(USER_ID)).thenReturn(List.of());

        assertThat(controller.tree().getData()).isEmpty();
    }

    @Test
    void missingContextOrUserIsRejectedBeforeQuery() {
        RequestContextHolder.clear();
        rejected();
        context(null);
        rejected();
        context("  ");
        rejected();
        verifyNoInteractions(service);
    }

    @Test
    void businessFailureIsPreserved() {
        BizException failure = new BizException(ErrorCodeEnum.LOGIN_FAILED);
        when(service.tree(USER_ID)).thenThrow(failure);

        assertThatThrownBy(controller::tree).isSameAs(failure);
    }

    @Test
    void unexpectedFailureIsMappedToSystemError() {
        when(service.tree(USER_ID)).thenThrow(new IllegalStateException("query failed"));

        rejected();
    }

    private void rejected() {
        assertThatThrownBy(controller::tree).isInstanceOfSatisfying(BizException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.SYSTEM_ERROR));
    }

    private void context(String userId) {
        RequestContextHolder.set(new RequestContext("trace-1", "traceparent", userId, null, "session-1", "ai-operations"));
    }
}


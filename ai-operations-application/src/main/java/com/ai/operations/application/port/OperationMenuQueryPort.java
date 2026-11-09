package com.ai.operations.application.port;

import com.ai.operations.application.model.OperationMenuRowBO;

import java.util.List;

/**
 * 运营后台用户菜单查询端口。
 */
public interface OperationMenuQueryPort {

    /** 查询用户可见的菜单/按钮资源行（授权标记见 authorized 字段） */
    List<OperationMenuRowBO> selectUserMenus(String userId);
}

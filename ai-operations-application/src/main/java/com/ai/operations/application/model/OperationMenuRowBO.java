package com.ai.operations.application.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 用户菜单资源行（业务模型）。
 *
 * <p>与资源表/角色关联表的映射由 infrastructure 完成（Row → BO）。
 */
@Getter
@Setter
public class OperationMenuRowBO {
    private Long id;
    private Long parentId;
    private String menuCode;
    private String resourceName;
    private String desc;
    private Integer resourceType;
    private String routePath;
    private String icon;
    private Integer sortNo;
    private boolean authorized;
}

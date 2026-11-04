package com.ai.operations.infrastructure.persistence.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OperationMenuRow {
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


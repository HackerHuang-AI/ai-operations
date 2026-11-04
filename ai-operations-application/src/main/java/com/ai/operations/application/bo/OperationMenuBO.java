package com.ai.operations.application.bo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OperationMenuBO {
    private String menuCode;
    private String resourceName;
    private String desc;
    private Integer resourceType;
    private String routePath;
    private String icon;
    private Integer sortNo;
    private List<OperationMenuBO> children = List.of();
}


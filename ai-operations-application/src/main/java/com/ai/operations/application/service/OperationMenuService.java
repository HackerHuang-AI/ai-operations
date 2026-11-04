package com.ai.operations.application.service;

import com.ai.operations.application.bo.OperationMenuBO;

import java.util.List;

public interface OperationMenuService {
    List<OperationMenuBO> tree(String userId);
}


package com.ai.operations.application.service.impl;

import com.ai.operations.application.bo.OperationMenuBO;
import com.ai.operations.application.service.OperationMenuService;
import com.ai.operations.infrastructure.persistence.dto.OperationMenuRow;
import com.ai.operations.infrastructure.persistence.mapper.OperationMenuMapper;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OperationMenuServiceImpl implements OperationMenuService {
    private final OperationMenuMapper operationMenuMapper;

    public OperationMenuServiceImpl(OperationMenuMapper operationMenuMapper) {
        this.operationMenuMapper = operationMenuMapper;
    }

    @Override
    public List<OperationMenuBO> tree(String userId) {
        Map<Long, List<OperationMenuRow>> byParent = new HashMap<>();
        operationMenuMapper.selectUserMenus(userId).stream()
                .filter(row -> row.getResourceType() == 1 || row.getResourceType() == 2)
                .sorted(Comparator.comparing(OperationMenuRow::getSortNo).thenComparing(OperationMenuRow::getId))
                .forEach(row -> byParent.computeIfAbsent(row.getParentId(), key -> new ArrayList<>()).add(row));
        List<OperationMenuBO> result = new ArrayList<>();
        for (OperationMenuBO node : buildChildren(null, byParent)) {
            if ("ROOT".equals(node.getMenuCode()) && node.getResourceType() == 1) {
                result.addAll(node.getChildren());
            } else {
                result.add(node);
            }
        }
        return result;
    }

    private List<OperationMenuBO> buildChildren(Long parentId, Map<Long, List<OperationMenuRow>> byParent) {
        List<OperationMenuBO> result = new ArrayList<>();
        for (OperationMenuRow row : byParent.getOrDefault(parentId, List.of())) {
            if (row.getResourceType() == 2 && !row.isAuthorized()) {
                continue;
            }
            List<OperationMenuBO> children = buildChildren(row.getId(), byParent);
            if (row.getResourceType() == 1 && children.isEmpty()) {
                continue;
            }
            OperationMenuBO node = new OperationMenuBO();
            node.setMenuCode(row.getMenuCode());
            node.setResourceName(row.getResourceName());
            node.setDesc(row.getDesc());
            node.setResourceType(row.getResourceType());
            node.setRoutePath(row.getRoutePath());
            node.setIcon(row.getIcon());
            node.setSortNo(row.getSortNo());
            node.setChildren(children);
            result.add(node);
        }
        return result;
    }
}


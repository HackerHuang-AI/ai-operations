package com.ai.operations.application.service.impl;

import com.ai.operations.application.bo.OperationMenuBO;
import com.ai.operations.application.model.OperationMenuRowBO;
import com.ai.operations.application.port.OperationMenuQueryPort;
import com.ai.operations.application.service.OperationMenuService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OperationMenuServiceImpl implements OperationMenuService {
    private final OperationMenuQueryPort menuQueryPort;

    public OperationMenuServiceImpl(OperationMenuQueryPort menuQueryPort) {
        this.menuQueryPort = menuQueryPort;
    }

    @Override
    public List<OperationMenuBO> tree(String userId) {
        Map<Long, List<OperationMenuRowBO>> byParent = new HashMap<>();
        menuQueryPort.selectUserMenus(userId).stream()
                .filter(row -> row.getResourceType() == 1 || row.getResourceType() == 2)
                .sorted(Comparator.comparing(OperationMenuRowBO::getSortNo).thenComparing(OperationMenuRowBO::getId))
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

    private List<OperationMenuBO> buildChildren(Long parentId, Map<Long, List<OperationMenuRowBO>> byParent) {
        List<OperationMenuBO> result = new ArrayList<>();
        for (OperationMenuRowBO row : byParent.getOrDefault(parentId, List.of())) {
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

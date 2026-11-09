package com.ai.operations.infrastructure.repository;

import com.ai.operations.application.model.OperationMenuRowBO;
import com.ai.operations.application.port.OperationMenuQueryPort;
import com.ai.operations.infrastructure.persistence.dto.OperationMenuRow;
import com.ai.operations.infrastructure.persistence.mapper.OperationMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OperationMenuQueryPort 实现：SQL Row → BO 透传转换下沉。
 */
@Component
@RequiredArgsConstructor
public class OperationMenuRepository implements OperationMenuQueryPort {

    private final OperationMenuMapper operationMenuMapper;

    @Override
    public List<OperationMenuRowBO> selectUserMenus(String userId) {
        return operationMenuMapper.selectUserMenus(userId).stream()
                .map(this::toBo)
                .toList();
    }

    private OperationMenuRowBO toBo(OperationMenuRow row) {
        OperationMenuRowBO bo = new OperationMenuRowBO();
        bo.setId(row.getId());
        bo.setParentId(row.getParentId());
        bo.setMenuCode(row.getMenuCode());
        bo.setResourceName(row.getResourceName());
        bo.setDesc(row.getDesc());
        bo.setResourceType(row.getResourceType());
        bo.setRoutePath(row.getRoutePath());
        bo.setIcon(row.getIcon());
        bo.setSortNo(row.getSortNo());
        bo.setAuthorized(row.isAuthorized());
        return bo;
    }
}

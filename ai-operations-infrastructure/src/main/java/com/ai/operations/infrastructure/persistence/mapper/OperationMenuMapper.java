package com.ai.operations.infrastructure.persistence.mapper;

import com.ai.operations.infrastructure.persistence.dto.OperationMenuRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OperationMenuMapper {
    List<OperationMenuRow> selectUserMenus(@Param("userId") String userId);
}


package com.ai.operations.infrastructure.persistence.mapper;

import com.ai.operations.infrastructure.persistence.dto.OperationMenuRow;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OperationMenuMapperTest {
    private Configuration configuration;
    private BoundSql boundSql;
    private String sql;

    @BeforeEach
    void setUp() throws Exception {
        configuration = new Configuration();
        String resource = "mapper/OperationMenuMapper.xml";
        try (var input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        boundSql = configuration.getMappedStatement(OperationMenuMapper.class.getName() + ".selectUserMenus")
                .getBoundSql(Map.of("userId", "ops-user-1"));
        sql = boundSql.getSql().replaceAll("\\s+", " ").trim();
    }

    @Test
    void mapperUsesRealSchemaAndBoundCurrentUserWithoutTenant() {
        assertThat(configuration.hasMapper(OperationMenuMapper.class)).isTrue();
        assertThat(configuration.getMappedStatement(OperationMenuMapper.class.getName() + ".selectUserMenus")
                .getResultMaps().get(0).getType()).isEqualTo(OperationMenuRow.class);
        assertThat(sql).contains("m.parent_menu_id AS parentId", "m.menu_name AS resourceName, m.`desc`,",
                "ur.user_id = u.user_id", "r.id = ur.operation_role_id",
                "rm.operation_role_id = r.id", "granted.id = rm.operation_menu_id", "u.user_id = ?")
                .doesNotContain("tenant", "ops-user-1");
        assertThat(boundSql.getParameterMappings()).extracting(mapping -> mapping.getProperty()).containsExactly("userId");
    }

    @Test
    void inactiveUsersRolesAndGrantsCannotAuthorizeMenus() {
        assertThat(sql).contains("u.status = 1 AND u.valid = 1", "ur.valid = 1",
                "r.status = 1 AND r.valid = 1 AND rm.valid = 1",
                "granted.status = 1 AND granted.valid = 1",
                "WHERE m.status = 1 AND m.valid = 1");
    }

    @Test
    void authorizationTimeIncludesStartButExcludesEndAndAllowsOpenEnd() {
        assertThat(sql).contains("ur.effective_start_time <= CURRENT_TIMESTAMP",
                "(ur.effective_end_time IS NULL OR ur.effective_end_time > CURRENT_TIMESTAMP)")
                .doesNotContain("effective_start_time < CURRENT_TIMESTAMP", "effective_end_time >= CURRENT_TIMESTAMP");
    }

    @Test
    void onlyDirectoryAndPageGrantsCanAuthorizeSelfOrExplicitDescendants() {
        assertThat(sql).contains("granted.resource_type IN (1, 2)", "m.resource_type IN (1, 2)",
                "granted.id = m.id OR (rm.include_descendants = 1",
                "LEFT(m.men_path, CHAR_LENGTH(granted.men_path) + 1) = CONCAT(granted.men_path, ';')");
    }

    @Test
    void multipleRolesUseExistenceNotRowMultiplicationAndOrderIsStable() {
        assertThat(sql).startsWith("SELECT m.id,").contains("EXISTS ( SELECT 1", ") AS authorized FROM operation_menu m")
                .endsWith("ORDER BY m.sort_no, m.id");
    }
}


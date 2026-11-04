package com.ai.operations.application.service.impl;

import com.ai.operations.application.bo.OperationMenuBO;
import com.ai.operations.infrastructure.persistence.dto.OperationMenuRow;
import com.ai.operations.infrastructure.persistence.mapper.OperationMenuMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OperationMenuServiceImplTest {
    private static final String USER_ID = "ops-user-1";
    private OperationMenuMapper mapper;
    private OperationMenuServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(OperationMenuMapper.class);
        service = new OperationMenuServiceImpl(mapper);
    }

    @Test
    void noRolesReturnsEmptyTree() {
        menus(row(1, null, "ROOT", 1, false, 0), row(2, 1L, "HOME", 2, false, 1));

        assertThat(service.tree(USER_ID)).isEmpty();
        verify(mapper).selectUserMenus(USER_ID);
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void directLeafGrantKeepsAncestorDirectoriesWithoutSiblingLeakage() {
        menus(row(1, null, "ROOT", 1, false, 0), row(2, 1L, "ADMIN", 1, false, 1),
                row(3, 2L, "USERS", 2, true, 1), row(4, 2L, "ROLES", 2, false, 2),
                row(5, 1L, "EMPTY_GRANTED", 1, true, 2));

        List<OperationMenuBO> tree = service.tree(USER_ID);

        assertThat(tree).extracting(OperationMenuBO::getMenuCode).containsExactly("ADMIN");
        assertThat(tree.get(0).getDesc()).isEqualTo("ADMIN描述");
        assertThat(tree.get(0).getChildren()).extracting(OperationMenuBO::getMenuCode).containsExactly("USERS");
        OperationMenuBO leaf = tree.get(0).getChildren().get(0);
        assertThat(leaf.getChildren()).isEmpty();
        assertThat(leaf.getResourceName()).isEqualTo("USERS菜单");
        assertThat(leaf.getDesc()).isEqualTo("USERS描述");
        assertThat(leaf.getRoutePath()).isEqualTo("/USERS");
        assertThat(leaf.getIcon()).isEqualTo("icon-USERS");
    }

    @Test
    void rootDescendantGrantsExpandRootAndRemoveEmptyDirectories() {
        OperationMenuRow profile = row(3, 2L, "PROFILE", 2, true, 1);
        profile.setDesc(null);
        menus(row(1, null, "ROOT", 1, true, 0), row(2, 1L, "HOME", 2, true, 1),
                profile, row(4, 1L, "ADMIN", 1, true, 2),
                row(5, 4L, "USERS", 2, true, 1), row(6, 1L, "EMPTY", 1, true, 3));

        List<OperationMenuBO> tree = service.tree(USER_ID);

        assertThat(tree).extracting(OperationMenuBO::getMenuCode).containsExactly("HOME", "ADMIN");
        assertThat(tree.get(0).getChildren()).extracting(OperationMenuBO::getMenuCode).containsExactly("PROFILE");
        assertThat(tree.get(0).getChildren().get(0).getDesc()).isNull();
        assertThat(tree.get(1).getChildren()).extracting(OperationMenuBO::getMenuCode).containsExactly("USERS");
    }

    @Test
    void directPageGrantDoesNotAuthorizeItsChildPages() {
        menus(row(1, null, "ROOT", 1, false, 0), row(2, 1L, "HOME", 2, true, 1),
                row(3, 2L, "PROFILE", 2, false, 1));

        List<OperationMenuBO> tree = service.tree(USER_ID);

        assertThat(tree).extracting(OperationMenuBO::getMenuCode).containsExactly("HOME");
        assertThat(tree.get(0).getChildren()).isEmpty();
    }

    @Test
    void buttonOnlyGrantDoesNotPromoteParentPageOrDirectory() {
        menus(row(1, null, "ROOT", 1, false, 0), row(2, 1L, "ADMIN", 1, false, 1),
                row(3, 2L, "USERS", 2, false, 1), row(4, 3L, "CREATE", 3, true, 1));

        assertThat(service.tree(USER_ID)).isEmpty();
    }

    @Test
    void buttonsNeverAppearUnderAuthorizedPages() {
        menus(row(1, null, "ROOT", 1, true, 0), row(2, 1L, "HOME", 2, true, 1),
                row(3, 2L, "EDIT", 3, true, 1), row(4, 3L, "HIDDEN", 2, true, 1));

        assertThat(service.tree(USER_ID).get(0).getChildren()).isEmpty();
    }

    @Test
    void disabledOrDeletedParentBranchIsNotPromotedToRoot() {
        menus(row(1, null, "ROOT", 1, true, 0), row(3, 2L, "HIDDEN", 2, true, 1),
                row(4, 1L, "HOME", 2, true, 2));

        assertThat(service.tree(USER_ID)).extracting(OperationMenuBO::getMenuCode).containsExactly("HOME");
        menus(row(4, 1L, "HOME", 2, true, 2));
        assertThat(service.tree(USER_ID)).isEmpty();
    }

    @Test
    void authorizedChildDoesNotPromoteUnauthorizedParentPage() {
        menus(row(1, null, "ROOT", 1, false, 0), row(2, 1L, "HOME", 2, false, 1),
                row(3, 2L, "PROFILE", 2, true, 1));

        assertThat(service.tree(USER_ID)).isEmpty();
    }

    @Test
    void sortNumberThenIdOrdersEveryLevel() {
        menus(row(8, 5L, "THIRD", 2, true, 2), row(4, 1L, "SECOND_ROOT", 2, true, 2),
                row(7, 5L, "SECOND", 2, true, 1), row(6, 5L, "FIRST", 2, true, 1),
                row(1, null, "ROOT", 1, true, 0), row(5, 1L, "FIRST_ROOT", 1, false, 1));

        List<OperationMenuBO> tree = service.tree(USER_ID);

        assertThat(tree).extracting(OperationMenuBO::getMenuCode).containsExactly("FIRST_ROOT", "SECOND_ROOT");
        assertThat(tree.get(0).getChildren()).extracting(OperationMenuBO::getMenuCode)
                .containsExactly("FIRST", "SECOND", "THIRD");
    }

    private void menus(OperationMenuRow... rows) {
        when(mapper.selectUserMenus(USER_ID)).thenReturn(List.of(rows));
    }

    private OperationMenuRow row(long id, Long parentId, String code, int type, boolean authorized, int sortNo) {
        OperationMenuRow row = new OperationMenuRow();
        row.setId(id);
        row.setParentId(parentId);
        row.setMenuCode(code);
        row.setResourceName(code + "菜单");
        row.setDesc(code + "描述");
        row.setResourceType(type);
        row.setRoutePath(type == 2 ? "/" + code : null);
        row.setIcon("icon-" + code);
        row.setSortNo(sortNo);
        row.setAuthorized(authorized);
        return row;
    }
}


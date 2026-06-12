package org.alloy.security;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllowedUserActionsAuthorityMapperTest {

    @Test
    void mapsEnterpriseAdminUserManagementActions() {
        Set<String> authorities = AllowedUserActionsAuthorityMapper.permissionAuthorities(Set.of(
                "create_edit_enterprise_users",
                "visibility_edit_enterprises"
        ));
        assertTrue(authorities.contains("PERMISSION_CREATE_EDIT_USER_ENTERPRISE"));
        assertTrue(authorities.contains("PERMISSION_VISIBILITY_EDIT_ENTERPRISES"));
    }

    @Test
    void ignoresUnknownActions() {
        Set<String> authorities = AllowedUserActionsAuthorityMapper.permissionAuthorities(Set.of(
                "view_only_users",
                "unknown_action"
        ));
        assertTrue(authorities.isEmpty());
    }

    @Test
    void mapsWorkWithReports() {
        Set<String> authorities = AllowedUserActionsAuthorityMapper.permissionAuthorities(Set.of("work_with_reports"));
        assertTrue(authorities.contains("PERMISSION_WORK_WITH_REPORTS"));
        assertFalse(authorities.contains("PERMISSION_CREATE_EDIT_USER_ENTERPRISE"));
    }
}

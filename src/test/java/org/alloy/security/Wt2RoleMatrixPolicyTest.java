package org.alloy.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Политика «прямых» прав по матрице: «+» в ячейке → роль имеет authority {@link Wt2Permission#getAuthority()}
 * без дополнительных грантов. «наст N» и «+*5» проверяются как {@link Wt2RoleMatrix.CellKind#CONFIGURABLE}.
 */
class Wt2RoleMatrixPolicyTest {

    @Test
    void matrixRowCountMatchesPermissions() {
        assertEquals(32, Wt2Permission.values().length);
        assertEquals(32, Wt2RoleMatrix.directPermissions(Wt2RoleMatrix.Wt2RoleColumn.ADMIN_ALLOY).size());
    }

    @Test
    void adminAlloyHasAllDirectPermissions() {
        assertEquals(EnumSet.allOf(Wt2Permission.class),
                Wt2RoleMatrix.directPermissions(Wt2RoleMatrix.Wt2RoleColumn.ADMIN_ALLOY));
    }

    @Test
    void recoverPasswordRowIsAllowForAllRoles() {
        for (Wt2RoleMatrix.Wt2RoleColumn col : Wt2RoleMatrix.Wt2RoleColumn.values()) {
            assertEquals(Wt2RoleMatrix.CellKind.ALLOW,
                    Wt2RoleMatrix.cell(Wt2Permission.RECOVER_OWN_LOGIN_PASSWORD, col).getKind(),
                    col.name());
        }
    }

    @ParameterizedTest
    @EnumSource(Wt2RoleMatrix.Wt2RoleColumn.class)
    void configurableCellsHaveLevelOneToSix(Wt2RoleMatrix.Wt2RoleColumn col) {
        Wt2RoleMatrix.configurableBy(col).forEach((perm, level) -> {
            assertTrue(level >= 1 && level <= 6, perm + " -> " + level);
        });
    }

    static Stream<Arguments> expectedDirectByRole() {
        return Stream.of(
                Arguments.of(Wt2RoleMatrix.Wt2RoleColumn.USER_ALLOY,
                        EnumSet.of(Wt2Permission.RECOVER_OWN_LOGIN_PASSWORD)),
                Arguments.of(Wt2RoleMatrix.Wt2RoleColumn.ADMIN_DEALER,
                        EnumSet.of(
                                Wt2Permission.CREATE_EDIT_USER_DEALER,
                                Wt2Permission.RECOVER_OWN_LOGIN_PASSWORD,
                                Wt2Permission.RESET_PASSWORD_USER_DEALER,
                                Wt2Permission.RESET_PASSWORD_ADMIN_ENTERPRISE,
                                Wt2Permission.RESET_PASSWORD_USER_ENTERPRISE,
                                Wt2Permission.VISIBILITY_EDIT_DEALERS,
                                Wt2Permission.ADD_EQUIPMENT,
                                Wt2Permission.MOVE_EQUIPMENT_CHANGE_INFO,
                                Wt2Permission.DELETE_EQUIPMENT,
                                Wt2Permission.VIEW_EQUIPMENT_HISTORY_GRAPHS,
                                Wt2Permission.WORK_WITH_REPORTS,
                                Wt2Permission.WORK_WITH_NOTIFICATIONS,
                                Wt2Permission.ADD_DELETE_EDIT_WELDING_MATERIALS,
                                Wt2Permission.ADD_DELETE_EDIT_WPS_CARDS)),
                Arguments.of(Wt2RoleMatrix.Wt2RoleColumn.USER_DEALER,
                        EnumSet.of(Wt2Permission.RECOVER_OWN_LOGIN_PASSWORD)),
                Arguments.of(Wt2RoleMatrix.Wt2RoleColumn.ADMIN_ENTERPRISE,
                        EnumSet.of(
                                Wt2Permission.CREATE_EDIT_USER_ENTERPRISE,
                                Wt2Permission.RECOVER_OWN_LOGIN_PASSWORD,
                                Wt2Permission.RESET_PASSWORD_USER_ENTERPRISE,
                                Wt2Permission.VISIBILITY_EDIT_ENTERPRISES,
                                Wt2Permission.ADD_EQUIPMENT,
                                Wt2Permission.MOVE_EQUIPMENT_CHANGE_INFO,
                                Wt2Permission.DELETE_EQUIPMENT,
                                Wt2Permission.BIND_WELDERS_TO_EQUIPMENT,
                                Wt2Permission.MAINTENANCE_RECORD,
                                Wt2Permission.EQUIPMENT_CONTROL_ACCESS,
                                Wt2Permission.VIEW_EQUIPMENT_HISTORY_GRAPHS,
                                Wt2Permission.ADD_DELETE_EDIT_WELDERS,
                                Wt2Permission.WELDER_CERTIFICATION_DATA,
                                Wt2Permission.ADD_DELETE_RFID_PASSES,
                                Wt2Permission.WORK_WITH_REPORTS,
                                Wt2Permission.WORK_WITH_NOTIFICATIONS,
                                Wt2Permission.ADD_DELETE_EDIT_WELDING_MATERIALS,
                                Wt2Permission.ADD_DELETE_EDIT_WPS_CARDS)),
                Arguments.of(Wt2RoleMatrix.Wt2RoleColumn.USER_ENTERPRISE,
                        EnumSet.of(Wt2Permission.RECOVER_OWN_LOGIN_PASSWORD))
        );
    }

    @ParameterizedTest
    @MethodSource("expectedDirectByRole")
    void directPermissionsMatchMatrix(Wt2RoleMatrix.Wt2RoleColumn col, Set<Wt2Permission> expected) {
        assertEquals(expected, Wt2RoleMatrix.directPermissions(col));
    }
}

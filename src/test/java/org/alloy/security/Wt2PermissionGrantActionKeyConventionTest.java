package org.alloy.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Соглашение: строки в {@code UserAccount.allowedUserActions} и ключи в
 * {@link org.springframework.security.access.prepost.PreAuthorize} для
 * {@link org.alloy.services.UserAccountService#hasAllowedUserAction(String, String)}
 * совпадают с {@link Wt2Permission#name()} в нижнем регистре (snake_case).
 */
class Wt2PermissionGrantActionKeyConventionTest {

    @Test
    void createDeleteEnterprisesKeyMatchesOrganizationController() {
        assertEquals("create_delete_enterprises",
                Wt2Permission.CREATE_DELETE_ENTERPRISES.name().toLowerCase(Locale.ROOT));
    }

    @Test
    void allPermissionNamesAreUpperSnakeCase() {
        for (Wt2Permission p : Wt2Permission.values()) {
            String n = p.name();
            assertEquals(n, n.toUpperCase(Locale.ROOT), () -> "Use UPPER_SNAKE_CASE in enum: " + p);
        }
    }

    /**
     * Ячейки «наст N» в {@link Wt2RoleMatrix}: ключ гранта в {@code allowedUserActions} =
     * {@code PERMISSION.name().toLowerCase()}.
     */
    @ParameterizedTest
    @EnumSource(Wt2RoleMatrix.Wt2RoleColumn.class)
    void matrixConfigurableCellsMatchLowercaseEnumGrantKey(Wt2RoleMatrix.Wt2RoleColumn col) {
        Wt2RoleMatrix.configurableBy(col).forEach((perm, level) -> {
            String expected = perm.name().toLowerCase(Locale.ROOT);
            assertEquals(expected, grantActionKey(perm), col.name() + " / " + perm);
        });
    }

    private static String grantActionKey(Wt2Permission p) {
        return p.name().toLowerCase(Locale.ROOT);
    }
}

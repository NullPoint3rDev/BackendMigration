package org.alloy.security;

import org.alloy.models.entities.UserAccount;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ключи {@code UserAccount.allowedUserActions} (как на форме AddUserPage) и правила view-only.
 */
public final class AllowedUserActionsHelper {

    public static final String VIEW_ONLY_EQUIPMENT = "view_only_equipment";
    public static final String VIEW_ONLY_WELDERS = "view_only_welders";
    public static final String VIEW_ONLY_USERS = "view_only_users";
    public static final String VIEW_ONLY_ORGANIZATIONS = "view_only_organizations";

    public static final String WORK_WITH_REPORTS = "work_with_reports";
    public static final String WIFI_MODULES_WT2 = "wifi_modules_wt2";

    private static final Set<String> EQUIPMENT_DOMAIN = Set.of(
            VIEW_ONLY_EQUIPMENT,
            "wifi_modules_wt2",
            "add_equipment_core_pulse",
            "move_equipment_change_info",
            "delete_equipment",
            "view_ip_history",
            "ip_management_functions",
            "fix_maintenance",
            "assign_welders_to_equipment"
    );

    private static final Set<String> EQUIPMENT_WRITE = Set.of(
            "wifi_modules_wt2",
            "add_equipment_core_pulse",
            "move_equipment_change_info",
            "delete_equipment",
            "ip_management_functions",
            "fix_maintenance",
            "assign_welders_to_equipment"
    );

    private static final Set<String> WELDERS_DOMAIN = Set.of(
            VIEW_ONLY_WELDERS,
            "add_delete_edit_welders",
            "manage_welder_certification",
            "add_delete_rfid_passes"
    );

    private static final Set<String> WELDERS_WRITE = Set.of(
            "add_delete_edit_welders",
            "manage_welder_certification",
            "add_delete_rfid_passes"
    );

    private static final Set<String> USERS_DOMAIN = Set.of(
            VIEW_ONLY_USERS,
            "recovery_account",
            "create_edit_enterprise_admins",
            "create_edit_enterprise_users",
            "reset_enterprise_admin_passwords",
            "reset_enterprise_user_passwords",
            "create_edit_dealer_admins",
            "create_edit_dealer_users",
            "reset_dealer_admin_passwords",
            "reset_dealer_user_passwords",
            "create_alloy_admins",
            "create_edit_alloy_users",
            "reset_alloy_user_passwords"
    );

    private static final Set<String> USERS_WRITE = Set.of(
            "recovery_account",
            "create_edit_enterprise_admins",
            "create_edit_enterprise_users",
            "reset_enterprise_admin_passwords",
            "reset_enterprise_user_passwords",
            "create_edit_dealer_admins",
            "create_edit_dealer_users",
            "reset_dealer_admin_passwords",
            "reset_dealer_user_passwords",
            "create_alloy_admins",
            "create_edit_alloy_users",
            "reset_alloy_user_passwords"
    );

    private static final Set<String> ORGANIZATIONS_DOMAIN = Set.of(
            VIEW_ONLY_ORGANIZATIONS,
            "visibility_edit_dealers",
            "create_delete_dealers",
            "visibility_edit_enterprises",
            "create_delete_enterprises",
            "visibility_edit_alloy"
    );

    private static final Set<String> ORGANIZATIONS_WRITE = Set.of(
            "visibility_edit_dealers",
            "create_delete_dealers",
            "visibility_edit_enterprises",
            "create_delete_enterprises",
            "visibility_edit_alloy"
    );

    private AllowedUserActionsHelper() {
    }

    public static Set<String> parseActions(UserAccount account) {
        if (account == null || account.getAllowedUserActions() == null
                || account.getAllowedUserActions().trim().isEmpty()) {
            return Collections.emptySet();
        }
        return Arrays.stream(account.getAllowedUserActions().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(HashSet::new));
    }

    public static boolean hasAction(Set<String> actions, String actionId) {
        if (actions == null || actionId == null) {
            return false;
        }
        return actions.contains(actionId.trim().toLowerCase(Locale.ROOT));
    }

    public static boolean canReadEquipment(Set<String> actions, boolean adminAlloy) {
        if (adminAlloy) {
            return true;
        }
        if (hasAction(actions, VIEW_ONLY_EQUIPMENT)) {
            return true;
        }
        return actions != null && actions.stream().anyMatch(EQUIPMENT_DOMAIN::contains);
    }

    public static boolean canWriteEquipment(Set<String> actions, boolean adminAlloy) {
        if (adminAlloy) {
            return true;
        }
        if (hasAction(actions, VIEW_ONLY_EQUIPMENT)) {
            return false;
        }
        return actions != null && actions.stream().anyMatch(EQUIPMENT_WRITE::contains);
    }

    public static boolean canReadWelders(Set<String> actions, boolean adminAlloy) {
        if (adminAlloy) {
            return true;
        }
        if (hasAction(actions, VIEW_ONLY_WELDERS)) {
            return true;
        }
        return actions != null && actions.stream().anyMatch(WELDERS_DOMAIN::contains);
    }

    public static boolean canWriteWelders(Set<String> actions, boolean adminAlloy) {
        if (adminAlloy) {
            return true;
        }
        if (hasAction(actions, VIEW_ONLY_WELDERS)) {
            return false;
        }
        return actions != null && actions.stream().anyMatch(WELDERS_WRITE::contains);
    }

    public static boolean canReadUsers(Set<String> actions, boolean adminAlloy) {
        if (adminAlloy) {
            return true;
        }
        if (hasAction(actions, VIEW_ONLY_USERS)) {
            return true;
        }
        return actions != null && actions.stream().anyMatch(USERS_DOMAIN::contains);
    }

    public static boolean canWriteUsers(Set<String> actions, boolean adminAlloy) {
        if (adminAlloy) {
            return true;
        }
        if (hasAction(actions, VIEW_ONLY_USERS)) {
            return false;
        }
        return actions != null && actions.stream().anyMatch(USERS_WRITE::contains);
    }

    public static boolean canReadOrganizations(Set<String> actions, boolean adminAlloy) {
        if (adminAlloy) {
            return true;
        }
        if (hasAction(actions, VIEW_ONLY_ORGANIZATIONS)) {
            return true;
        }
        return actions != null && actions.stream().anyMatch(ORGANIZATIONS_DOMAIN::contains);
    }

    public static boolean canWriteOrganizations(Set<String> actions, boolean adminAlloy) {
        if (adminAlloy) {
            return true;
        }
        if (hasAction(actions, VIEW_ONLY_ORGANIZATIONS)) {
            return false;
        }
        return actions != null && actions.stream().anyMatch(ORGANIZATIONS_WRITE::contains);
    }

    public static boolean canWorkWithReports(Set<String> actions, boolean adminAlloy) {
        if (adminAlloy) {
            return true;
        }
        return hasAction(actions, WORK_WITH_REPORTS);
    }

    public static boolean canReadMacRegistry(Set<String> actions, boolean adminAlloy) {
        if (adminAlloy) {
            return true;
        }
        return hasAction(actions, WIFI_MODULES_WT2);
    }

    public static boolean canAddMacRegistry(Set<String> actions, boolean adminAlloy) {
        if (adminAlloy) {
            return true;
        }
        if (hasAction(actions, VIEW_ONLY_EQUIPMENT)) {
            return false;
        }
        return hasAction(actions, WIFI_MODULES_WT2);
    }

    /** Удаляет write-ключи домена, если включён соответствующий view_only. */
    public static String sanitizeAllowedUserActionsCsv(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return raw;
        }
        Set<String> actions = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(HashSet::new));

        if (hasAction(actions, VIEW_ONLY_EQUIPMENT)) {
            actions.removeAll(EQUIPMENT_WRITE);
        }
        if (hasAction(actions, VIEW_ONLY_WELDERS)) {
            actions.removeAll(WELDERS_WRITE);
        }
        if (hasAction(actions, VIEW_ONLY_USERS)) {
            actions.removeAll(USERS_WRITE);
        }
        if (hasAction(actions, VIEW_ONLY_ORGANIZATIONS)) {
            actions.removeAll(ORGANIZATIONS_WRITE);
        }

        return String.join(",", actions);
    }
}

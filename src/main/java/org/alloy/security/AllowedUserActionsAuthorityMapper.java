package org.alloy.security;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Ключи allowedUserActions (форма AddUserPage) → authority PERMISSION_* (Wt2Permission).
 */
public final class AllowedUserActionsAuthorityMapper {

    private static final Map<String, Wt2Permission> ACTION_TO_PERMISSION;

    static {
        Map<String, Wt2Permission> map = new HashMap<>();
        map.put("recovery_account", Wt2Permission.RECOVER_OWN_LOGIN_PASSWORD);
        map.put("create_edit_enterprise_admins", Wt2Permission.CREATE_EDIT_ADMIN_ENTERPRISE);
        map.put("create_edit_enterprise_users", Wt2Permission.CREATE_EDIT_USER_ENTERPRISE);
        map.put("reset_enterprise_admin_passwords", Wt2Permission.RESET_PASSWORD_ADMIN_ENTERPRISE);
        map.put("reset_enterprise_user_passwords", Wt2Permission.RESET_PASSWORD_USER_ENTERPRISE);
        map.put("create_edit_dealer_admins", Wt2Permission.CREATE_EDIT_ADMIN_DEALER);
        map.put("create_edit_dealer_users", Wt2Permission.CREATE_EDIT_USER_DEALER);
        map.put("reset_dealer_admin_passwords", Wt2Permission.RESET_PASSWORD_ADMIN_DEALER);
        map.put("reset_dealer_user_passwords", Wt2Permission.RESET_PASSWORD_USER_DEALER);
        map.put("create_alloy_admins", Wt2Permission.CREATE_ADMIN_ALLOY);
        map.put("create_edit_alloy_users", Wt2Permission.CREATE_EDIT_USER_ALLOY);
        map.put("reset_alloy_user_passwords", Wt2Permission.RESET_PASSWORD_USER_ALLOY);
        map.put("visibility_edit_dealers", Wt2Permission.VISIBILITY_EDIT_DEALERS);
        map.put("create_delete_dealers", Wt2Permission.CREATE_DELETE_DEALERS);
        map.put("visibility_edit_enterprises", Wt2Permission.VISIBILITY_EDIT_ENTERPRISES);
        map.put("create_delete_enterprises", Wt2Permission.CREATE_DELETE_ENTERPRISES);
        map.put("visibility_edit_alloy", Wt2Permission.VISIBILITY_EDIT_ALLOY);
        map.put("wifi_modules_wt2", Wt2Permission.WIFI_MODULES_FORM_ACCESS);
        map.put("add_equipment_core_pulse", Wt2Permission.ADD_EQUIPMENT);
        map.put("move_equipment_change_info", Wt2Permission.MOVE_EQUIPMENT_CHANGE_INFO);
        map.put("delete_equipment", Wt2Permission.DELETE_EQUIPMENT);
        map.put("view_ip_history", Wt2Permission.VIEW_EQUIPMENT_HISTORY_GRAPHS);
        map.put("ip_management_functions", Wt2Permission.EQUIPMENT_CONTROL_ACCESS);
        map.put("fix_maintenance", Wt2Permission.MAINTENANCE_RECORD);
        map.put("assign_welders_to_equipment", Wt2Permission.BIND_WELDERS_TO_EQUIPMENT);
        map.put("add_delete_edit_welders", Wt2Permission.ADD_DELETE_EDIT_WELDERS);
        map.put("manage_welder_certification", Wt2Permission.WELDER_CERTIFICATION_DATA);
        map.put("add_delete_rfid_passes", Wt2Permission.ADD_DELETE_RFID_PASSES);
        map.put("work_with_reports", Wt2Permission.WORK_WITH_REPORTS);
        map.put("work_with_notifications", Wt2Permission.WORK_WITH_NOTIFICATIONS);
        map.put("welding_materials", Wt2Permission.ADD_DELETE_EDIT_WELDING_MATERIALS);
        map.put("wps_cards", Wt2Permission.ADD_DELETE_EDIT_WPS_CARDS);
        ACTION_TO_PERMISSION = Collections.unmodifiableMap(map);
    }

    private AllowedUserActionsAuthorityMapper() {
    }

    public static Set<String> permissionAuthorities(Set<String> allowedActions) {
        if (allowedActions == null || allowedActions.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> out = new HashSet<>();
        for (String action : allowedActions) {
            if (action == null || action.isEmpty()) {
                continue;
            }
            Wt2Permission permission = ACTION_TO_PERMISSION.get(action.trim().toLowerCase(Locale.ROOT));
            if (permission != null) {
                out.add(permission.getAuthority());
            }
        }
        return out;
    }
}

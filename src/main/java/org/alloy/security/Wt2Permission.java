package org.alloy.security;

/**
 * Коды прав по матрице «Функции пользователей WT2» (32 пункта).
 * Используются как authority: PERMISSION
 */
public enum Wt2Permission {
    CREATE_ADMIN_ALLOY,              // 1
    CREATE_EDIT_USER_ALLOY,          // 2
    CREATE_EDIT_ADMIN_DEALER,        // 3
    CREATE_EDIT_USER_DEALER,         // 4
    CREATE_EDIT_ADMIN_ENTERPRISE,    // 5
    CREATE_EDIT_USER_ENTERPRISE,     // 6
    RECOVER_OWN_LOGIN_PASSWORD,      // 7
    RESET_PASSWORD_USER_ALLOY,       // 8
    RESET_PASSWORD_ADMIN_DEALER,     // 9
    RESET_PASSWORD_USER_DEALER,      // 10
    RESET_PASSWORD_ADMIN_ENTERPRISE, // 11
    RESET_PASSWORD_USER_ENTERPRISE, // 12
    CREATE_DELETE_DEALERS,           // 13
    CREATE_DELETE_ENTERPRISES,       // 14
    VISIBILITY_EDIT_ALLOY,           // 15
    VISIBILITY_EDIT_DEALERS,         // 16
    VISIBILITY_EDIT_ENTERPRISES,     // 17
    WIFI_MODULES_FORM_ACCESS,        // 18
    ADD_EQUIPMENT,                   // 19
    MOVE_EQUIPMENT_CHANGE_INFO,      // 20
    DELETE_EQUIPMENT,                // 21
    BIND_WELDERS_TO_EQUIPMENT,       // 22
    MAINTENANCE_RECORD,              // 23
    EQUIPMENT_CONTROL_ACCESS,        // 24
    VIEW_EQUIPMENT_HISTORY_GRAPHS,   // 25
    ADD_DELETE_EDIT_WELDERS,         // 26
    WELDER_CERTIFICATION_DATA,        // 27
    ADD_DELETE_RFID_PASSES,          // 28
    WORK_WITH_REPORTS,               // 29
    WORK_WITH_NOTIFICATIONS,         // 30
    ADD_DELETE_EDIT_WELDING_MATERIALS, // 31
    ADD_DELETE_EDIT_WPS_CARDS;       // 32

    /** Authority для Spring Security */
    public String getAuthority() {
        return "PERMISSION_" + name();
    }
}

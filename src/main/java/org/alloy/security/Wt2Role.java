package org.alloy.security;

/**
 * Роли системы WT2 (по матрице «Функции пользователей»).
 * Уровень (1–6) используется для логики «наст N»: право может выдаваться пользователем с ролью уровня N.
 */
public enum Wt2Role {
    /** 1 — Администратор Эллой */
    ADMIN_ALLOY(1, "Администратор Эллой"),
    /** 2 — Пользователь Эллой */
    USER_ALLOY(2, "Пользователь Эллой"),
    /** 3 — Администратор диллера */
    ADMIN_DEALER(3, "Администратор диллера"),
    /** 4 — Пользователь диллера */
    USER_DEALER(4, "Пользователь диллера"),
    /** 5 — Администратор предприятия */
    ADMIN_ENTERPRISE(5, "Администратор предприятия"),
    /** 6 — Пользователь предприятия */
    USER_ENTERPRISE(6, "Пользователь предприятия");

    private final int level;
    private final String displayName;

    Wt2Role(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Имя для Spring Security (hasRole использует это без префикса ROLE_) */
    public String getAuthority() {
        return "ROLE_" + name();
    }
}

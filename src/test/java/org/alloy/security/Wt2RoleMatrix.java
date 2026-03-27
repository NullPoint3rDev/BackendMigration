package org.alloy.security;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Матрица «Функции пользователей WT2» (32 строки × 6 ролей).
 * Ячейки: «+» — прямое право; «-» — нет; «наст N» — только через выдачу прав при создании пользователя (настраивает роль уровня N).
 */
public final class Wt2RoleMatrix {

    private Wt2RoleMatrix() {}

    public enum Wt2RoleColumn {
        ADMIN_ALLOY(1),
        USER_ALLOY(2),
        ADMIN_DEALER(3),
        USER_DEALER(4),
        ADMIN_ENTERPRISE(5),
        USER_ENTERPRISE(6);

        private final int level;

        Wt2RoleColumn(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    public enum CellKind {
        /** Прямое разрешение (плюс в матрице) */
        ALLOW,
        /** Запрет по умолчанию */
        DENY,
        /** Не прямое право: может быть выдано пользователю ролью уровня {@link #configuratorLevel} */
        CONFIGURABLE
    }

    public static final class Cell {
        private final CellKind kind;
        /** Для {@link CellKind#CONFIGURABLE}: кто может настраивать (уровень роли 1–6) */
        private final int configuratorLevel;

        private Cell(CellKind kind, int configuratorLevel) {
            this.kind = kind;
            this.configuratorLevel = configuratorLevel;
        }

        public static Cell allow() {
            return new Cell(CellKind.ALLOW, 0);
        }

        public static Cell deny() {
            return new Cell(CellKind.DENY, 0);
        }

        public static Cell configurable(int configuratorLevel) {
            if (configuratorLevel < 1 || configuratorLevel > 6) {
                throw new IllegalArgumentException("configuratorLevel must be 1..6");
            }
            return new Cell(CellKind.CONFIGURABLE, configuratorLevel);
        }

        public CellKind getKind() {
            return kind;
        }

        public int getConfiguratorLevel() {
            return configuratorLevel;
        }
    }

    /**
     * Строки в порядке {@link Wt2Permission#ordinal()} (1..32).
     * Колонки: {@link Wt2RoleColumn} по порядку enum.
     */
    private static final String[][] RAW = new String[][] {
            {"+", "-", "-", "-", "-", "-"},
            {"+", "-", "-", "-", "-", "-"},
            {"+", "наст 1", "-", "-", "-", "-"},
            {"+", "наст 1", "+", "-", "-", "-"},
            {"+", "наст 1", "-", "-", "-", "-"},
            {"+", "наст 1", "-", "-", "+", "-"},
            {"+", "+", "+", "+", "+", "+"},
            {"+", "-", "-", "-", "-", "-"},
            {"+", "наст 1", "-", "-", "-", "-"},
            {"+", "наст 1", "+", "-", "-", "-"},
            {"+", "наст 1", "+", "наст 3", "-", "-"},
            {"+", "наст 1", "+", "наст 3", "+", "-"},
            {"+", "наст 1", "-", "-", "-", "-"},
            {"+", "наст 1", "+*5", "-", "-", "-"},
            {"+", "-", "-", "-", "-", "-"},
            {"+", "наст 1", "+", "наст 3", "-", "-"},
            {"+", "наст 1", "наст 1", "наст 3", "+", "наст 5"},
            {"+", "наст 1", "-", "-", "-", "-"},
            {"+", "наст 1", "+", "наст 3", "+", "наст 5"},
            {"+", "наст 1", "+", "наст 3", "+", "наст 5"},
            {"+", "наст 1", "+", "наст 3", "+", "наст 5"},
            {"+", "-", "-", "-", "+", "наст 5"},
            {"+", "-", "-", "-", "+", "наст 5"},
            {"+", "наст 1", "-", "-", "+", "наст 5"},
            {"+", "наст 1", "+", "наст 3", "+", "наст 5"},
            {"+", "-", "-", "-", "+", "наст 5"},
            {"+", "-", "-", "-", "+", "наст 5"},
            {"+", "-", "-", "-", "+", "наст 5"},
            {"+", "наст 1", "+", "наст 3", "+", "наст 5"},
            {"+", "наст 1", "+", "наст 3", "+", "наст 5"},
            {"+", "наст 1", "+", "наст 3", "+", "наст 5"},
            {"+", "наст 1", "+", "наст 3", "+", "наст 5"},
    };

    static {
        if (RAW.length != Wt2Permission.values().length) {
            throw new IllegalStateException("Matrix rows must match Wt2Permission count");
        }
        for (String[] row : RAW) {
            if (row.length != Wt2RoleColumn.values().length) {
                throw new IllegalStateException("Each matrix row must have 6 columns");
            }
        }
    }

    public static Cell parseCell(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("null cell");
        }
        String s = raw.trim();
        if ("+".equals(s)) {
            return Cell.allow();
        }
        if ("-".equals(s)) {
            return Cell.deny();
        }
        String lower = s.toLowerCase(Locale.ROOT).replace(" ", "");
        if (lower.startsWith("наст")) {
            String num = lower.substring("наст".length());
            int level = Integer.parseInt(num);
            return Cell.configurable(level);
        }
        if ("+*5".equals(s.trim())) {
            return Cell.configurable(5);
        }
        throw new IllegalArgumentException("Unknown matrix cell: " + raw);
    }

    public static Cell cell(Wt2Permission row, Wt2RoleColumn col) {
        return parseCell(RAW[row.ordinal()][col.ordinal()]);
    }

    /** Права с прямым «+» для роли (без учёта выдачи «наст»). */
    public static Set<Wt2Permission> directPermissions(Wt2RoleColumn role) {
        EnumSet<Wt2Permission> set = EnumSet.noneOf(Wt2Permission.class);
        for (Wt2Permission p : Wt2Permission.values()) {
            if (cell(p, role).getKind() == CellKind.ALLOW) {
                set.add(p);
            }
        }
        return Collections.unmodifiableSet(set);
    }

    /** Все пары (право → уровень настройщика) для ячеек «наст N». */
    public static Map<Wt2Permission, Integer> configurableBy(Wt2RoleColumn role) {
        EnumMap<Wt2Permission, Integer> map = new EnumMap<>(Wt2Permission.class);
        for (Wt2Permission p : Wt2Permission.values()) {
            Cell c = cell(p, role);
            if (c.getKind() == CellKind.CONFIGURABLE) {
                map.put(p, c.getConfiguratorLevel());
            }
        }
        return Collections.unmodifiableMap(map);
    }
}

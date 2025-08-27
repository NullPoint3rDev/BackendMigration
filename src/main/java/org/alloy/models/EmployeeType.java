package org.alloy.models;

public enum EmployeeType {
    ADMIN("Администратор"),
    MANAGER("Менеджер"),
    REGULATOR("Регулировщик"),
    WELDER("Сварщик"),
    QC("ОТК"),
    PROGRAMMER("Программист");

    private final String displayName;

    EmployeeType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

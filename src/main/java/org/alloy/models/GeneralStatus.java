package org.alloy.models;

public enum GeneralStatus {
    Active,
    Inactive,
    Deleted,
    Pending,
    Blocked,
    /** Аппарат скрыт из UI, MAC освобождён; данные удаляются фоновым job по id. */
    Purging
}

package org.alloy.models.notifications;

public abstract class NotificationTypeBase {
    public abstract String getType();
    public abstract String generateKey();
    public abstract String generateJson();
    public abstract String buildContent();
}

package org.alloy.models.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NotificationTypeMessage extends NotificationTypeBase {
    private NotificationMessageParameters parameters;

    public NotificationTypeMessage(String message) {
        this.parameters = new NotificationMessageParameters();
        this.parameters.setMessage(message);
    }

    @Override
    public String getType() {
        return "NotificationMessage";
    }

    @Override
    public String generateKey() {
        return null;
    }

    @Override
    public String generateJson() {
        if (parameters == null) {
            return "";
        }
        try {
            return new ObjectMapper().writeValueAsString(parameters);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public String buildContent() {
        return parameters == null ? null : parameters.getMessage();
    }

    @Data
    @NoArgsConstructor
    public static class NotificationMessageParameters {
        private String message;
    }
}

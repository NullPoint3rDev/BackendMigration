package org.alloy.models.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class WeldingMachineTypeConfiguration {
    private Settings settings;
    private PropertyLimits propertyLimits;
    private MessageConfig outbound;
    private MessageConfig inbound;

    @Data
    public static class Settings {
        private double efficiency = 0.95;
        private double standbyPower = 20;
    }

    @Data
    public static class PropertyLimits {
        private PropertyLimit[] limits;
    }

    @Data
    public static class PropertyLimit {
        private String propertyCode;
        private String description;
        private double minValue;
        private double maxValue;
        private double step;
    }

    @Data
    public static class MessageConfig {
        private MessageProperty[] body;
    }

    @Data
    public static class MessageProperty {
        private String propertyCode;
        private String description;
        private String propertyType;
        private String rangeSource;
        private PropertyEnum[] enums;
    }

    @Data
    public static class PropertyEnum {
        private String value;
        private String description;
    }
}

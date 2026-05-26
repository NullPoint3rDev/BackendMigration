package org.alloy.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class ReportGenerationJobStartDTO {
    /**
     * WIRE_CONSUMPTION | WELDER_WORK | EQUIPMENT_WORK | EQUIPMENT_MALFUNCTION
     */
    private String reportType;
    /** Тело запроса как у соответствующего POST .../generate */
    private Map<String, Object> payload;
}

package org.alloy.models.weldingmachine;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StateSummaryPropertyValue {
    private String propertyCode;
    private String value;
    private String propertyType;
    private String rawValue;
    private boolean limitsExceeded;
    private Float limitMin;
    private Float limitMax;
} 
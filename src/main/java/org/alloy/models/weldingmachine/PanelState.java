package org.alloy.models.weldingmachine;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class PanelState {
    private LocalDateTime lastDatetimeUpdate;
    private List<PanelLedItem> leds = new ArrayList<>();
    private List<PanelTextItem> texts = new ArrayList<>();
    private List<SummaryProperty> summaryProperties = new ArrayList<>();
    private PanelWorkerInfo workerInfo;
    private double iReal;
    private double uReal;
}

@Data
@NoArgsConstructor
class PanelWorkerInfo {
    private int userAccountId;
    private String name;
    private String photo;
}

@Data
@NoArgsConstructor
class PanelItem {
    private double x;
    private double y;
    private String color;
}

@Data
@NoArgsConstructor
class PanelLedItem extends PanelItem {
    private double width;
    private double height;
    private double radius;
}

@Data
@NoArgsConstructor
class PanelTextItem extends PanelItem {
    private String text;
    private double fontSize;
    private String fontFamily;
    private String fontStyle;
}

@Data
@NoArgsConstructor
class SummaryProperty {
    private String propertyCode;
    private String title;
    private String value;
    private String unit;
    private String propertyType;
}

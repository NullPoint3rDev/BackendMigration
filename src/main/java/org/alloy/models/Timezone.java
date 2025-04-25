package org.alloy.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
public class Timezone {
    private int id;
    private String name;
    private int offset;
    private String countryCode;
    private Double offsetDouble;
    private String abbr;
    private int offsetSeconds;

    public Timezone(Map<String, String> dict) {
        this.id = Integer.parseInt(dict.get("id"));
        this.name = dict.get("name");
        this.offset = Integer.parseInt(dict.get("offset"));
        this.countryCode = dict.get("country_code");
        String offsetDoubleStr = dict.get("offset_double");
        if (offsetDoubleStr != null && !offsetDoubleStr.isEmpty()) {
            this.offsetDouble = Double.parseDouble(offsetDoubleStr);
        }
        this.offsetSeconds = Integer.parseInt(dict.get("offset_seconds"));
        this.abbr = dict.get("abbr");
    }

    public String getGmtOffsetDescription() {
        return String.format("GMT %s%02d:%02d",
                offsetDouble < 0 ? "-" : "+",
                Math.floor(Math.abs(offsetDouble)),
                Math.round((Math.abs(offsetDouble) - Math.floor(Math.abs(offsetDouble))) * 60)
        );
    }

    public String getPublicGmtName() {
        return String.format("%s (%s)", name, getGmtOffsetDescription());
    }

    public int getId() {
        return 0;
    }
}

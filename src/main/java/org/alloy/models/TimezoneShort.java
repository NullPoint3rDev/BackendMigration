package org.alloy.models;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TimezoneShort {
    private int id;
    private String name;

    public TimezoneShort(Timezone tz) {
        this.id = tz.getId();
        this.name = tz.getPublicGmtName();
    }
}

package org.alloy.models.bulks;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class BulkWeldingRawData {
    private long packetId;
    private long durationMs;
    private String dateTime = "";
    private List<BulkWeldingRawField> fields = new ArrayList<>();
}

@Data
@NoArgsConstructor
class BulkWeldingRawField {
    private String code = "";
    private String value = "";
}

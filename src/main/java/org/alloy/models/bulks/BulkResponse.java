package org.alloy.models.bulks;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class BulkResponse<T> {
    private BulkMeta meta = new BulkMeta();
    private List<T> data = new ArrayList<>();
}

@Data
@NoArgsConstructor
class BulkMeta {
    private int total = 0;
    private int page = 1;
    private int pageSize = 0;
}

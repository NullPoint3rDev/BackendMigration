package org.alloy.models.dto;

import lombok.Data;

import java.util.List;

@Data
public class MacAddressRegistryPageDTO {
    private List<MacAddressRegistryDTO> items;
    private long total;
    private int page;
    private int pageSize;
}

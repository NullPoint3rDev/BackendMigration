package org.alloy.models.dto.mapper;

import org.alloy.models.entities.Dump;
import org.alloy.models.dto.DumpDTO;

public class DumpMapper {
    public static DumpDTO toDTO(Dump dump) {
        if (dump == null) return null;
        DumpDTO dto = new DumpDTO();
        dto.setId(dump.getId());
        dto.setDateCreated(dump.getDateCreated());
        dto.setMac(dump.getMac());
        dto.setIp(dump.getIp());
        dto.setData(dump.getData());
        return dto;
    }

    public static Dump toEntity(DumpDTO dto) {
        if (dto == null) return null;
        Dump dump = new Dump();
        dump.setId(dto.getId());
        dump.setDateCreated(dto.getDateCreated());
        dump.setMac(dto.getMac());
        dump.setIp(dto.getIp());
        dump.setData(dto.getData());
        return dump;
    }
} 
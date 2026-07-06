package org.alloy.models.dto.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtcLocalDateTimeAsMoscowOffsetSerializerTest {

    static class Probe {
        @JsonSerialize(using = UtcLocalDateTimeAsMoscowOffsetSerializer.class)
        public LocalDateTime lastWeldAt;

        Probe(LocalDateTime lastWeldAt) {
            this.lastWeldAt = lastWeldAt;
        }
    }

    @Test
    void serializesUtcNaiveAsMoscowOffset() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        String json = mapper.writeValueAsString(new Probe(LocalDateTime.of(2026, 6, 25, 11, 39, 5)));

        assertEquals("{\"lastWeldAt\":\"2026-06-25T14:39:05+03:00\"}", json);
    }
}

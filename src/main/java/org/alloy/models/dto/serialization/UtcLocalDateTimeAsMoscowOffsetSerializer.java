package org.alloy.models.dto.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Сериализация naive UTC из БД в ISO с московским offset (+03:00). */
public class UtcLocalDateTimeAsMoscowOffsetSerializer extends JsonSerializer<LocalDateTime> {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        String formatted = value.atZone(DisplayTimeZones.STORAGE)
                .withZoneSameInstant(DisplayTimeZones.DISPLAY)
                .toOffsetDateTime()
                .format(ISO_OFFSET);
        gen.writeString(formatted);
    }
}

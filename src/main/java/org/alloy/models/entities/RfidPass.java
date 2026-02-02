package org.alloy.models.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "rfid_passes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"code", "welder_id"})
})
@Data
@NoArgsConstructor
@Schema(description = "RFID пропуск сварщика")
public class RfidPass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Уникальный идентификатор пропуска")
    private Long id;

    @Column(name = "code", nullable = false, length = 100)
    @Schema(description = "Код RFID пропуска")
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "welder_id", nullable = false)
    @JsonIgnore
    @Schema(description = "Сварщик, которому принадлежит пропуск")
    private Welder welder;
}


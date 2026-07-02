package org.alloy.models.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Certifications")
@Data
@NoArgsConstructor
@Schema(description = "Аттестация сварщика")
public class Certification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Уникальный идентификатор аттестации")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "welder_id", nullable = false, foreignKey = @ForeignKey(name = "fk_certification_welder"))
    @JsonIgnore
    @Schema(description = "Сварщик")
    private Welder welder;

    @Column(name = "certificate_number", length = 255)
    @Schema(description = "Номер удостоверения")
    private String certificateNumber;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "certification_date")
    @Schema(description = "Дата аттестации")
    private LocalDate certificationDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "expiry_date")
    @Schema(description = "Дата окончания действия аттестации")
    private LocalDate expiryDate;

    @ElementCollection
    @CollectionTable(name = "certification_welding_methods", joinColumns = @JoinColumn(name = "certification_id"))
    @Column(name = "method")
    @Schema(description = "Способы сварки")
    private List<String> weldingMethods = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "certification_tech_groups", joinColumns = @JoinColumn(name = "certification_id"))
    @Column(name = "group_name")
    @Schema(description = "Группы технических устройств")
    private List<String> techGroups = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "certification_positions", joinColumns = @JoinColumn(name = "certification_id"))
    @Column(name = "position")
    @Schema(description = "Пространственные положения швов")
    private List<String> positions = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "certification_connections", joinColumns = @JoinColumn(name = "certification_id"))
    @Column(name = "connection")
    @Schema(description = "Виды сварных соединений")
    private List<String> connections = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "certification_materials", joinColumns = @JoinColumn(name = "certification_id"))
    @Column(name = "material")
    @Schema(description = "Группы сварочных материалов")
    private List<String> materials = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "certification_parts", joinColumns = @JoinColumn(name = "certification_id"))
    @Column(name = "part")
    @Schema(description = "Виды свариваемых деталей")
    private List<String> parts = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "certification_weld_types", joinColumns = @JoinColumn(name = "certification_id"))
    @Column(name = "weld_type")
    @Schema(description = "Типы сварных швов")
    private List<String> weldTypes = new ArrayList<>();

    @Column(name = "thickness_from")
    @Schema(description = "Толщина деталей от, мм")
    private Double thicknessFrom;

    @Column(name = "thickness_to")
    @Schema(description = "Толщина деталей до, мм")
    private Double thicknessTo;

    @Column(name = "diameter_from")
    @Schema(description = "Наружный диаметр от, мм")
    private Double diameterFrom;

    @Column(name = "diameter_to")
    @Schema(description = "Наружный диаметр до, мм")
    private Double diameterTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Schema(description = "Статус аттестации")
    private CertificationStatus status = CertificationStatus.ACTIVE;

    public enum CertificationStatus {
        ACTIVE("Действует"),
        EXPIRED("Истекла"),
        REVOKED("Аннулирована");

        private final String displayName;

        CertificationStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}


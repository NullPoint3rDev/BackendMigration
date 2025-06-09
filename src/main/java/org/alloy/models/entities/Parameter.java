package org.alloy.models.entities;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "Parameter")
public class Parameter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer measureUnitId;

    @Column(nullable = false)
    private String propertyCode;

    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "measureUnitId", insertable = false, updatable = false)
    private MeasureUnit measureUnit;
} 
package org.alloy.models.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "TRANSLATION")
public class Translation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Lang")
    private String lang;

    @Column(name = "TableName")
    private String tableName;

    @Column(name = "ColumnName")
    private String columnName;

    @Column(name = "IdName")
    private String idName;

    @Column(name = "IdValue")
    private String idValue;

    @Column(name = "Value")
    private String value;
}

package org.alloy.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "v2_session_index")
@IdClass(V2SessionIndex.PK.class)
@Data
@NoArgsConstructor
public class V2SessionIndex {

    @Id
    @Column(name = "mac", length = 12, nullable = false)
    private String mac;

    @Id
    @Column(name = "session_number", nullable = false)
    private Integer sessionNumber;

    @Id
    @Column(name = "channel", length = 16, nullable = false)
    private String channel;

    @Column(name = "last_index", nullable = false)
    private Integer lastIndex;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    public static class PK implements Serializable {
        private String mac;
        private Integer sessionNumber;
        private String channel;
    }
}

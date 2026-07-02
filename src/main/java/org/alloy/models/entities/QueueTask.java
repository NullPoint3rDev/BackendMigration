package org.alloy.models.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "QueueTask")
public class QueueTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreated;

    private Integer status;
    private Integer userAccountId;
    private LocalDateTime dateStarted;
    private LocalDateTime dateFinished;
    private LocalDateTime scheduledOn;
    private String taskName;
    private String taskParametersJson;
    private String statusResult;
    private String statusMessage;
    private Integer priority;
    private String taskResultJson;
    private Integer scheduleTaskId;
} 
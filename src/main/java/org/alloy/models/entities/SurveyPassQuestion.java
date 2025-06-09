package org.alloy.models.entities;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "SurveyPassQuestion")
public class SurveyPassQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer surveyPassId;

    @Column(nullable = false)
    private Integer surveyQuestionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "surveyPassId", insertable = false, updatable = false)
    private SurveyPass surveyPass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "surveyQuestionId", insertable = false, updatable = false)
    private SurveyQuestion surveyQuestion;
} 
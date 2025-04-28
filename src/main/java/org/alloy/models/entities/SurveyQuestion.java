package org.alloy.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "SurveyQuestion")
public class SurveyQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer surveyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "surveyId", insertable = false, updatable = false)
    private Survey survey;

    @OneToMany(mappedBy = "surveyQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SurveyPassQuestion> surveyPassQuestions;
} 
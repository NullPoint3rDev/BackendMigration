package org.alloy.repositories;

import org.alloy.models.entities.SurveyPassQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SurveyPassQuestionRepository extends JpaRepository<SurveyPassQuestion, Integer> {
} 
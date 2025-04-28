package org.alloy.services;

import org.alloy.models.entities.SurveyQuestion;
import org.alloy.repositories.SurveyQuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SurveyQuestionService {
    @Autowired
    private SurveyQuestionRepository surveyQuestionRepository;

    public List<SurveyQuestion> findAll() {
        return surveyQuestionRepository.findAll();
    }

    public Optional<SurveyQuestion> findById(Integer id) {
        return surveyQuestionRepository.findById(id);
    }

    public SurveyQuestion save(SurveyQuestion surveyQuestion) {
        return surveyQuestionRepository.save(surveyQuestion);
    }

    public void deleteById(Integer id) {
        surveyQuestionRepository.deleteById(id);
    }
} 
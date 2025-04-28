package org.alloy.services;

import org.alloy.models.entities.SurveyPassQuestion;
import org.alloy.repositories.SurveyPassQuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SurveyPassQuestionService {
    @Autowired
    private SurveyPassQuestionRepository surveyPassQuestionRepository;

    public List<SurveyPassQuestion> findAll() {
        return surveyPassQuestionRepository.findAll();
    }

    public Optional<SurveyPassQuestion> findById(Integer id) {
        return surveyPassQuestionRepository.findById(id);
    }

    public SurveyPassQuestion save(SurveyPassQuestion surveyPassQuestion) {
        return surveyPassQuestionRepository.save(surveyPassQuestion);
    }

    public void deleteById(Integer id) {
        surveyPassQuestionRepository.deleteById(id);
    }
} 
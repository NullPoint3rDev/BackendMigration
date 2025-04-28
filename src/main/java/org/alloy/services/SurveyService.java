package org.alloy.services;

import org.alloy.models.entities.Survey;
import org.alloy.repositories.SurveyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SurveyService {
    @Autowired
    private SurveyRepository surveyRepository;

    public List<Survey> findAll() {
        return surveyRepository.findAll();
    }

    public Optional<Survey> findById(Integer id) {
        return surveyRepository.findById(id);
    }

    public Survey save(Survey survey) {
        return surveyRepository.save(survey);
    }

    public void deleteById(Integer id) {
        surveyRepository.deleteById(id);
    }
} 
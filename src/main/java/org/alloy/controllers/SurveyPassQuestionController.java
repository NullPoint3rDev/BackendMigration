package org.alloy.controllers;

import org.alloy.models.entities.SurveyPassQuestion;
import org.alloy.services.SurveyPassQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/survey-pass-questions")
public class SurveyPassQuestionController {
    @Autowired
    private SurveyPassQuestionService surveyPassQuestionService;

    @GetMapping
    public List<SurveyPassQuestion> getAll() {
        return surveyPassQuestionService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SurveyPassQuestion> getById(@PathVariable Integer id) {
        return surveyPassQuestionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public SurveyPassQuestion create(@RequestBody SurveyPassQuestion surveyPassQuestion) {
        return surveyPassQuestionService.save(surveyPassQuestion);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SurveyPassQuestion> update(@PathVariable Integer id, @RequestBody SurveyPassQuestion surveyPassQuestion) {
        if (!surveyPassQuestionService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        surveyPassQuestion.setId(id);
        return ResponseEntity.ok(surveyPassQuestionService.save(surveyPassQuestion));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!surveyPassQuestionService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        surveyPassQuestionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
} 
package org.alloy.controllers;

import org.alloy.models.entities.SurveyQuestion;
import org.alloy.services.SurveyQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/survey-questions")
public class SurveyQuestionController {
    @Autowired
    private SurveyQuestionService surveyQuestionService;

    @GetMapping
    public List<SurveyQuestion> getAll() {
        return surveyQuestionService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SurveyQuestion> getById(@PathVariable Integer id) {
        return surveyQuestionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public SurveyQuestion create(@RequestBody SurveyQuestion surveyQuestion) {
        return surveyQuestionService.save(surveyQuestion);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SurveyQuestion> update(@PathVariable Integer id, @RequestBody SurveyQuestion surveyQuestion) {
        if (!surveyQuestionService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        surveyQuestion.setId(id);
        return ResponseEntity.ok(surveyQuestionService.save(surveyQuestion));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!surveyQuestionService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        surveyQuestionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
} 
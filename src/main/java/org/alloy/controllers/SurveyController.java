package org.alloy.controllers;

import org.alloy.models.entities.Survey;
import org.alloy.services.SurveyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/surveys")
public class SurveyController {
    @Autowired
    private SurveyService surveyService;

    @GetMapping
    public List<Survey> getAll() {
        return surveyService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Survey> getById(@PathVariable Integer id) {
        return surveyService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Survey create(@RequestBody Survey survey) {
        return surveyService.save(survey);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Survey> update(@PathVariable Integer id, @RequestBody Survey survey) {
        if (!surveyService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        survey.setId(id);
        return ResponseEntity.ok(surveyService.save(survey));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!surveyService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        surveyService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
} 
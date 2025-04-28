package org.alloy.controllers;

import org.alloy.models.entities.Translation;
import org.alloy.services.TranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/translations")
public class TranslationController {
    @Autowired
    private TranslationService translationService;

    @GetMapping
    public List<Translation> getAll() {
        return translationService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Translation> getById(@PathVariable Integer id) {
        return translationService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Translation create(@RequestBody Translation translation) {
        return translationService.save(translation);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Translation> update(@PathVariable Integer id, @RequestBody Translation translation) {
        return translationService.findById(id)
                .map(existingTranslation -> {
                    translation.setId(id);
                    return ResponseEntity.ok(translationService.save(translation));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return translationService.findById(id)
                .map(translation -> {
                    translationService.deleteById(id);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
} 
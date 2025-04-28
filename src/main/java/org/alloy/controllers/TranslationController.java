package org.alloy.controllers;

import org.alloy.models.entities.Translation;
import org.alloy.services.TranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/translations")
public class TranslationController {
    @Autowired
    private TranslationService translationService;

    @GetMapping
    public Page<Translation> getAll(Pageable pageable) {
        return translationService.findAll(pageable);
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
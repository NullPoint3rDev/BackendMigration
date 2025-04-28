package org.alloy.controllers;

import org.alloy.models.entities.EmailTemplate;
import org.alloy.services.EmailTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/email-templates")
public class EmailTemplateController {
    @Autowired
    private EmailTemplateService emailTemplateService;

    @GetMapping
    public List<EmailTemplate> getAll() {
        return emailTemplateService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailTemplate> getById(@PathVariable Integer id) {
        return emailTemplateService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public EmailTemplate create(@RequestBody EmailTemplate emailTemplate) {
        return emailTemplateService.save(emailTemplate);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmailTemplate> update(@PathVariable Integer id, @RequestBody EmailTemplate emailTemplate) {
        if (!emailTemplateService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        emailTemplate.setId(id);
        return ResponseEntity.ok(emailTemplateService.save(emailTemplate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!emailTemplateService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        emailTemplateService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
} 
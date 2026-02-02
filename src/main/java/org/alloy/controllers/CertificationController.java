package org.alloy.controllers;

import org.alloy.models.entities.Certification;
import org.alloy.services.CertificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/certifications")
public class CertificationController {

    @Autowired
    private CertificationService certificationService;

    @PreAuthorize("hasRole('Администратор') or hasRole('Менеджер') or hasRole('Технолог')")
    @GetMapping
    public ResponseEntity<List<Certification>> getAllCertifications() {
        List<Certification> certifications = certificationService.getAllCertifications();
        return ResponseEntity.ok(certifications);
    }

    @PreAuthorize("hasRole('Администратор') or hasRole('Менеджер') or hasRole('Технолог')")
    @GetMapping("/{id}")
    public ResponseEntity<Certification> getCertificationById(@PathVariable Long id) {
        Optional<Certification> certification = certificationService.getCertificationById(id);
        return certification.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('Администратор') or hasRole('Менеджер') or hasRole('Технолог')")
    @GetMapping("/welder/{welderId}")
    public ResponseEntity<List<Certification>> getCertificationsByWelderId(@PathVariable Long welderId) {
        List<Certification> certifications = certificationService.getCertificationsByWelderId(welderId);
        return ResponseEntity.ok(certifications);
    }

    @PreAuthorize("hasRole('Администратор') or hasRole('Технолог')")
    @PostMapping("/welder/{welderId}")
    public ResponseEntity<Certification> createCertification(
            @PathVariable Long welderId,
            @RequestBody Certification certification) {
        try {
            Certification createdCertification = certificationService.createCertification(welderId, certification);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCertification);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PreAuthorize("hasRole('Администратор') or hasRole('Технолог')")
    @PutMapping("/{id}")
    public ResponseEntity<Certification> updateCertification(
            @PathVariable Long id,
            @RequestBody Certification certificationDetails) {
        Certification updatedCertification = certificationService.updateCertification(id, certificationDetails);
        if (updatedCertification != null) {
            return ResponseEntity.ok(updatedCertification);
        }
        return ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasRole('Администратор')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCertification(@PathVariable Long id) {
        boolean deleted = certificationService.deleteCertification(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}


package org.alloy.controllers;

import org.alloy.models.dto.WelderDTO;
import org.alloy.models.entities.RfidPass;
import org.alloy.models.entities.Welder;
import org.alloy.services.WelderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/welders")
public class WelderController {

    @Autowired
    private WelderService welderService;

    @PreAuthorize("hasRole('Администратор') or hasRole('Менеджер') or hasRole('Технолог')")
    @GetMapping
    public ResponseEntity<List<Welder>> getAllWelders() {
        List<Welder> welders = welderService.getAllWelders();
        return ResponseEntity.ok(welders);
    }

    @PreAuthorize("hasRole('Администратор') or hasRole('Менеджер') or hasRole('Технолог')")
    @GetMapping("/{id}")
    public ResponseEntity<Welder> getWelderById(@PathVariable Long id) {
        Optional<Welder> welder = welderService.getWelderById(id);
        return welder.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('Администратор') or hasRole('Технолог')")
    @PostMapping
    public ResponseEntity<Welder> createWelder(@RequestBody WelderDTO welderDTO) {
        Welder createdWelder = welderService.createWelder(welderDTO);
        // Загружаем пропуска для возврата
        if (createdWelder != null) {
            createdWelder = welderService.getWelderById(createdWelder.getId()).orElse(createdWelder);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(createdWelder);
    }

    @PreAuthorize("hasRole('Администратор') or hasRole('Технолог')")
    @PutMapping("/{id}")
    public ResponseEntity<Welder> updateWelder(@PathVariable Long id, @RequestBody WelderDTO welderDTO) {
        Welder updatedWelder = welderService.updateWelder(id, welderDTO);
        if (updatedWelder != null) {
            // Загружаем пропуска для возврата
            updatedWelder = welderService.getWelderById(updatedWelder.getId()).orElse(updatedWelder);
            return ResponseEntity.ok(updatedWelder);
        }
        return ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasRole('Администратор') or hasRole('Менеджер') or hasRole('Технолог')")
    @GetMapping("/{id}/rfid-availability")
    public ResponseEntity<Boolean> checkRfidCodeAvailability(
            @PathVariable Long id,
            @RequestParam String rfidCode,
            @RequestParam String department) {
        boolean isAvailable = welderService.isRfidCodeAvailable(rfidCode, department, id);
        return ResponseEntity.ok(isAvailable);
    }

    @PreAuthorize("hasRole('Администратор')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWelder(@PathVariable Long id) {
        boolean deleted = welderService.deleteWelder(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasRole('Администратор') or hasRole('Менеджер') or hasRole('Технолог')")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Welder>> getWeldersByStatus(@PathVariable Welder.WelderStatus status) {
        List<Welder> welders = welderService.getWeldersByStatus(status);
        return ResponseEntity.ok(welders);
    }

    @PreAuthorize("hasRole('Администратор') or hasRole('Менеджер') or hasRole('Технолог')")
    @GetMapping("/department/{department}")
    public ResponseEntity<List<Welder>> getWeldersByDepartment(@PathVariable String department) {
        List<Welder> welders = welderService.getWeldersByDepartment(department);
        return ResponseEntity.ok(welders);
    }

    @PreAuthorize("hasRole('Администратор') or hasRole('Менеджер') or hasRole('Технолог')")
    @GetMapping("/name/{name}")
    public ResponseEntity<List<Welder>> getWeldersByName(@PathVariable String name) {
        List<Welder> welders = welderService.getWeldersByName(name);
        return ResponseEntity.ok(welders);
    }

    @PreAuthorize("hasRole('Администратор') or hasRole('Менеджер') or hasRole('Технолог')")
    @GetMapping("/grade/{grade}")
    public ResponseEntity<List<Welder>> getWeldersByGrade(@PathVariable String grade) {
        List<Welder> welders = welderService.getWeldersByGrade(grade);
        return ResponseEntity.ok(welders);
    }

    @PreAuthorize("hasRole('Администратор') or hasRole('Менеджер') or hasRole('Технолог')")
    @GetMapping("/rfid/{rfidCode}")
    public ResponseEntity<Welder> getWelderByRfidCode(@PathVariable String rfidCode) {
        Welder welder = welderService.getWelderByRfidCode(rfidCode);
        if (welder != null) {
            return ResponseEntity.ok(welder);
        }
        return ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasRole('Администратор') or hasRole('Менеджер') or hasRole('Технолог')")
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Welder> getWelderByEmployeeId(@PathVariable String employeeId) {
        Welder welder = welderService.getWelderByEmployeeId(employeeId);
        if (welder != null) {
            return ResponseEntity.ok(welder);
        }
        return ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasRole('Администратор') or hasRole('Менеджер') or hasRole('Технолог')")
    @GetMapping("/search")
    public ResponseEntity<List<Welder>> searchWelders(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Welder.WelderStatus status,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String grade) {
        List<Welder> welders = welderService.getWeldersByFilters(name, status, department, grade);
        return ResponseEntity.ok(welders);
    }

    @PreAuthorize("hasRole('Администратор') or hasRole('Технолог')")
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadWelderPhoto(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file) {
        try {
            String photoPath = welderService.uploadWelderPhoto(id, file);
            return ResponseEntity.ok(photoPath);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasRole('Администратор') or hasRole('Менеджер') or hasRole('Технолог')")
    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> getWelderPhoto(@PathVariable Long id) {
        try {
            Optional<Welder> welder = welderService.getWelderById(id);
            if (!welder.isPresent() || welder.get().getPhoto() == null) {
                return ResponseEntity.notFound().build();
            }
            byte[] photoData = welderService.getWelderPhoto(welder.get().getPhoto());
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(photoData);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

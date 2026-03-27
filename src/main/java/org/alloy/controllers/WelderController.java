package org.alloy.controllers;

import org.alloy.models.dto.WelderDTO;
import org.alloy.models.entities.Welder;
import org.alloy.services.WelderService;
import org.alloy.services.Wt2AccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/welders")
public class WelderController {

    @Autowired
    private WelderService welderService;

    @Autowired
    private Wt2AccessService wt2AccessService;

    @PreAuthorize("hasAnyAuthority('PERMISSION_ADD_DELETE_EDIT_WELDERS','PERMISSION_VIEW_EQUIPMENT_HISTORY_GRAPHS','PERMISSION_BIND_WELDERS_TO_EQUIPMENT')")
    @GetMapping
    public ResponseEntity<List<Welder>> getAllWelders() {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Welder> welders = wt2AccessService.filterWelders(welderService.getAllWelders(), principal);
        return ResponseEntity.ok(welders);
    }

    @PreAuthorize("hasAnyAuthority('PERMISSION_ADD_DELETE_EDIT_WELDERS','PERMISSION_VIEW_EQUIPMENT_HISTORY_GRAPHS','PERMISSION_BIND_WELDERS_TO_EQUIPMENT')")
    @GetMapping("/{id}")
    public ResponseEntity<Welder> getWelderById(@PathVariable Long id) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        wt2AccessService.assertCanViewWelder(id, principal);
        Optional<Welder> welder = welderService.getWelderById(id);
        return welder.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('PERMISSION_ADD_DELETE_EDIT_WELDERS')")
    @PostMapping
    public ResponseEntity<Welder> createWelder(@RequestBody WelderDTO welderDTO) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        wt2AccessService.assertEnterpriseWelderDepartmentAllowed(welderDTO != null ? welderDTO.getDepartment() : null, principal);
        wt2AccessService.assertEnterpriseCanAssignWelderMachines(welderDTO != null ? welderDTO.getMachineIds() : null, principal);
        Welder createdWelder = welderService.createWelder(welderDTO);
        // Загружаем пропуска для возврата
        if (createdWelder != null) {
            createdWelder = welderService.getWelderById(createdWelder.getId()).orElse(createdWelder);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(createdWelder);
    }

    @PreAuthorize("hasAuthority('PERMISSION_ADD_DELETE_EDIT_WELDERS')")
    @PutMapping("/{id}")
    public ResponseEntity<Welder> updateWelder(@PathVariable Long id, @RequestBody WelderDTO welderDTO) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        wt2AccessService.assertCanViewWelder(id, principal);
        wt2AccessService.assertEnterpriseWelderDepartmentAllowed(welderDTO != null ? welderDTO.getDepartment() : null, principal);
        wt2AccessService.assertEnterpriseCanAssignWelderMachines(welderDTO != null ? welderDTO.getMachineIds() : null, principal);
        Welder updatedWelder = welderService.updateWelder(id, welderDTO);
        if (updatedWelder != null) {
            // Загружаем пропуска для возврата
            updatedWelder = welderService.getWelderById(updatedWelder.getId()).orElse(updatedWelder);
            return ResponseEntity.ok(updatedWelder);
        }
        return ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasAnyAuthority('PERMISSION_ADD_DELETE_EDIT_WELDERS','PERMISSION_VIEW_EQUIPMENT_HISTORY_GRAPHS','PERMISSION_BIND_WELDERS_TO_EQUIPMENT')")
    @GetMapping("/{id}/rfid-availability")
    public ResponseEntity<Boolean> checkRfidCodeAvailability(
            @PathVariable Long id,
            @RequestParam String rfidCode,
            @RequestParam String department) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        wt2AccessService.assertCanViewWelder(id, principal);
        boolean isAvailable = welderService.isRfidCodeAvailable(rfidCode, department, id);
        return ResponseEntity.ok(isAvailable);
    }

    @PreAuthorize("hasAuthority('PERMISSION_ADD_DELETE_EDIT_WELDERS')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWelder(@PathVariable Long id) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        wt2AccessService.assertCanViewWelder(id, principal);
        boolean deleted = welderService.deleteWelder(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasAnyAuthority('PERMISSION_ADD_DELETE_EDIT_WELDERS','PERMISSION_VIEW_EQUIPMENT_HISTORY_GRAPHS','PERMISSION_BIND_WELDERS_TO_EQUIPMENT')")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Welder>> getWeldersByStatus(@PathVariable Welder.WelderStatus status) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Welder> welders = wt2AccessService.filterWelders(welderService.getWeldersByStatus(status), principal);
        return ResponseEntity.ok(welders);
    }

    @PreAuthorize("hasAnyAuthority('PERMISSION_ADD_DELETE_EDIT_WELDERS','PERMISSION_VIEW_EQUIPMENT_HISTORY_GRAPHS','PERMISSION_BIND_WELDERS_TO_EQUIPMENT')")
    @GetMapping("/department/{department}")
    public ResponseEntity<List<Welder>> getWeldersByDepartment(@PathVariable String department) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Welder> welders = wt2AccessService.filterWelders(welderService.getWeldersByDepartment(department), principal);
        return ResponseEntity.ok(welders);
    }

    @PreAuthorize("hasAnyAuthority('PERMISSION_ADD_DELETE_EDIT_WELDERS','PERMISSION_VIEW_EQUIPMENT_HISTORY_GRAPHS','PERMISSION_BIND_WELDERS_TO_EQUIPMENT')")
    @GetMapping("/name/{name}")
    public ResponseEntity<List<Welder>> getWeldersByName(@PathVariable String name) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Welder> welders = wt2AccessService.filterWelders(welderService.getWeldersByName(name), principal);
        return ResponseEntity.ok(welders);
    }

    @PreAuthorize("hasAnyAuthority('PERMISSION_ADD_DELETE_EDIT_WELDERS','PERMISSION_VIEW_EQUIPMENT_HISTORY_GRAPHS','PERMISSION_BIND_WELDERS_TO_EQUIPMENT')")
    @GetMapping("/grade/{grade}")
    public ResponseEntity<List<Welder>> getWeldersByGrade(@PathVariable String grade) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Welder> welders = wt2AccessService.filterWelders(welderService.getWeldersByGrade(grade), principal);
        return ResponseEntity.ok(welders);
    }

    @PreAuthorize("hasAnyAuthority('PERMISSION_ADD_DELETE_EDIT_WELDERS','PERMISSION_VIEW_EQUIPMENT_HISTORY_GRAPHS','PERMISSION_BIND_WELDERS_TO_EQUIPMENT')")
    @GetMapping("/rfid/{rfidCode}")
    public ResponseEntity<Welder> getWelderByRfidCode(@PathVariable String rfidCode) {
        Welder welder = welderService.getWelderByRfidCode(rfidCode);
        if (welder != null) {
            String principal = SecurityContextHolder.getContext().getAuthentication().getName();
            wt2AccessService.assertCanViewWelder(welder.getId(), principal);
            return ResponseEntity.ok(welder);
        }
        return ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasAnyAuthority('PERMISSION_ADD_DELETE_EDIT_WELDERS','PERMISSION_VIEW_EQUIPMENT_HISTORY_GRAPHS','PERMISSION_BIND_WELDERS_TO_EQUIPMENT')")
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Welder> getWelderByEmployeeId(@PathVariable String employeeId) {
        Welder welder = welderService.getWelderByEmployeeId(employeeId);
        if (welder != null) {
            String principal = SecurityContextHolder.getContext().getAuthentication().getName();
            wt2AccessService.assertCanViewWelder(welder.getId(), principal);
            return ResponseEntity.ok(welder);
        }
        return ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasAnyAuthority('PERMISSION_ADD_DELETE_EDIT_WELDERS','PERMISSION_VIEW_EQUIPMENT_HISTORY_GRAPHS','PERMISSION_BIND_WELDERS_TO_EQUIPMENT')")
    @GetMapping("/search")
    public ResponseEntity<List<Welder>> searchWelders(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Welder.WelderStatus status,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String grade) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Welder> welders = wt2AccessService.filterWelders(
                welderService.getWeldersByFilters(name, status, department, grade), principal);
        return ResponseEntity.ok(welders);
    }

    @PreAuthorize("hasAuthority('PERMISSION_ADD_DELETE_EDIT_WELDERS') or hasAuthority('PERMISSION_WELDER_CERTIFICATION_DATA')")
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadWelderPhoto(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file) {
        try {
            String principal = SecurityContextHolder.getContext().getAuthentication().getName();
            wt2AccessService.assertCanViewWelder(id, principal);
            String photoPath = welderService.uploadWelderPhoto(id, file);
            return ResponseEntity.ok(photoPath);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAnyAuthority('PERMISSION_ADD_DELETE_EDIT_WELDERS','PERMISSION_VIEW_EQUIPMENT_HISTORY_GRAPHS','PERMISSION_BIND_WELDERS_TO_EQUIPMENT')")
    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> getWelderPhoto(@PathVariable Long id) {
        try {
            String principal = SecurityContextHolder.getContext().getAuthentication().getName();
            wt2AccessService.assertCanViewWelder(id, principal);
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

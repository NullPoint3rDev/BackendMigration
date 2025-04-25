package org.alloy.controllers;

import org.alloy.models.entities.OrganizationUnit;
import org.alloy.services.OrganizationUnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organization-units")
public class OrganizationUnitController {

    private final OrganizationUnitService organizationUnitService;

    @Autowired
    public OrganizationUnitController(OrganizationUnitService organizationUnitService) {
        this.organizationUnitService = organizationUnitService;
    }

    @GetMapping
    public ResponseEntity<List<OrganizationUnit>> getAllOrganizationUnits() {
        List<OrganizationUnit> organizationUnits = organizationUnitService.getAllOrganizationUnits();
        return ResponseEntity.ok(organizationUnits);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationUnit> getOrganizationUnitById(@PathVariable Integer id) {
        return organizationUnitService.getOrganizationUnitById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<List<OrganizationUnit>> getOrganizationUnitsByOrganizationId(@PathVariable Integer organizationId) {
        List<OrganizationUnit> organizationUnits = organizationUnitService.getOrganizationUnitsByOrganizationId(organizationId);
        return ResponseEntity.ok(organizationUnits);
    }

    @GetMapping("/parent/{parentId}")
    public ResponseEntity<List<OrganizationUnit>> getOrganizationUnitsByParentId(@PathVariable Integer parentId) {
        List<OrganizationUnit> organizationUnits = organizationUnitService.getOrganizationUnitsByParentId(parentId);
        return ResponseEntity.ok(organizationUnits);
    }

    @GetMapping("/search")
    public ResponseEntity<List<OrganizationUnit>> searchOrganizationUnits(
            @RequestParam Integer organizationId,
            @RequestParam String searchTerm) {
        List<OrganizationUnit> organizationUnits = organizationUnitService.searchOrganizationUnits(organizationId, searchTerm);
        return ResponseEntity.ok(organizationUnits);
    }

    @PostMapping
    public ResponseEntity<OrganizationUnit> createOrganizationUnit(@RequestBody OrganizationUnit organizationUnit) {
        try {
            OrganizationUnit createdOrganizationUnit = organizationUnitService.createOrganizationUnit(organizationUnit);
            return new ResponseEntity<>(createdOrganizationUnit, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrganizationUnit> updateOrganizationUnit(@PathVariable Integer id, @RequestBody OrganizationUnit organizationUnit) {
        try {
            organizationUnit.setId(id);
            OrganizationUnit updatedOrganizationUnit = organizationUnitService.updateOrganizationUnit(organizationUnit);
            return ResponseEntity.ok(updatedOrganizationUnit);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrganizationUnit(@PathVariable Integer id) {
        try {
            organizationUnitService.deleteOrganizationUnit(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteOrganizationUnit(@PathVariable Integer id) {
        try {
            organizationUnitService.hardDeleteOrganizationUnit(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

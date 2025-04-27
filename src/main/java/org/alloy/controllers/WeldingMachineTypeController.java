package org.alloy.controllers;

import org.alloy.models.entities.WeldingMachineType;
import org.alloy.models.GeneralStatus;
import org.alloy.services.WeldingMachineTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/welding-machine-types")
public class WeldingMachineTypeController {

    private final WeldingMachineTypeService weldingMachineTypeService;

    @Autowired
    public WeldingMachineTypeController(WeldingMachineTypeService weldingMachineTypeService) {
        this.weldingMachineTypeService = weldingMachineTypeService;
    }

    @GetMapping
    public ResponseEntity<List<WeldingMachineType>> getAllWeldingMachineTypes() {
        List<WeldingMachineType> types = weldingMachineTypeService.getAllWeldingMachineTypes();
        return ResponseEntity.ok(types);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WeldingMachineType> getWeldingMachineTypeById(@PathVariable Integer id) {
        return weldingMachineTypeService.getWeldingMachineTypeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<WeldingMachineType> getWeldingMachineTypeByName(@PathVariable String name) {
        return weldingMachineTypeService.getWeldingMachineTypeByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<WeldingMachineType>> getWeldingMachineTypesByStatus(@PathVariable String status) {
        try {
            GeneralStatus generalStatus = GeneralStatus.valueOf(status);
            List<WeldingMachineType> types = weldingMachineTypeService.getWeldingMachineTypesByStatus(generalStatus);
            return ResponseEntity.ok(types);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<WeldingMachineType>> searchWeldingMachineTypes(@RequestParam String searchTerm) {
        List<WeldingMachineType> types = weldingMachineTypeService.searchWeldingMachineTypes(searchTerm);
        return ResponseEntity.ok(types);
    }

    @PostMapping
    public ResponseEntity<WeldingMachineType> createWeldingMachineType(@RequestBody WeldingMachineType type) {
        try {
            WeldingMachineType createdType = weldingMachineTypeService.createWeldingMachineType(type);
            return new ResponseEntity<>(createdType, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<WeldingMachineType> updateWeldingMachineType(@PathVariable Integer id, @RequestBody WeldingMachineType type) {
        try {
            type.setId(id);
            WeldingMachineType updatedType = weldingMachineTypeService.updateWeldingMachineType(type);
            return ResponseEntity.ok(updatedType);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWeldingMachineType(@PathVariable Integer id) {
        try {
            weldingMachineTypeService.deleteWeldingMachineType(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteWeldingMachineType(@PathVariable Integer id) {
        try {
            weldingMachineTypeService.hardDeleteWeldingMachineType(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

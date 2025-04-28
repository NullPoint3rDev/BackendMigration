package org.alloy.controllers;

import org.alloy.models.entities.Dump;
import org.alloy.services.DumpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dumps")
public class DumpController {
    @Autowired
    private DumpService dumpService;

    @GetMapping
    public List<Dump> getAll() {
        return dumpService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Dump> getById(@PathVariable Integer id) {
        return dumpService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Dump create(@RequestBody Dump dump) {
        return dumpService.save(dump);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Dump> update(@PathVariable Integer id, @RequestBody Dump dump) {
        if (!dumpService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        dump.setId(id);
        return ResponseEntity.ok(dumpService.save(dump));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!dumpService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        dumpService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
} 
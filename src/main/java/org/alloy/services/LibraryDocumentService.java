package org.alloy.services;

import org.alloy.models.entities.LibraryDocument;
import org.alloy.repositories.LibraryDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LibraryDocumentService {
    private final LibraryDocumentRepository repository;

    @Autowired
    public LibraryDocumentService(LibraryDocumentRepository repository) {
        this.repository = repository;
    }

    public LibraryDocument save(LibraryDocument doc) {
        return repository.save(doc);
    }

    public List<LibraryDocument> findAll() {
        return repository.findAll();
    }

    public Optional<LibraryDocument> findById(Long id) {
        return repository.findById(id);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
} 
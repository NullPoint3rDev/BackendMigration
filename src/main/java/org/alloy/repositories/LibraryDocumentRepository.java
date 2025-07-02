package org.alloy.repositories;

import org.alloy.models.entities.LibraryDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryDocumentRepository extends JpaRepository<LibraryDocument, Long> {
} 
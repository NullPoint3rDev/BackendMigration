package org.alloy.controllers;

import org.alloy.models.entities.LibraryDocument;
import org.alloy.services.LibraryDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/library-documents")
public class LibraryDocumentController {
    private final LibraryDocumentService service;
    private final Path libraryDir = Paths.get("library");

    @Autowired
    public LibraryDocumentController(LibraryDocumentService service) throws IOException {
        this.service = service;
        if (!Files.exists(libraryDir)) {
            Files.createDirectories(libraryDir);
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LibraryDocumentDTO> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("comment") String comment,
            @RequestParam("uploader") String uploader
    ) throws IOException {
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = libraryDir.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        LibraryDocument doc = new LibraryDocument();
        doc.setFileName(file.getOriginalFilename());
        doc.setComment(comment);
        doc.setUploader(uploader);
        doc.setUploadDate(LocalDateTime.now());
        doc.setFilePath(fileName);
        LibraryDocument saved = service.save(doc);
        return ResponseEntity.ok(LibraryDocumentDTO.fromEntity(saved));
    }

    @GetMapping
    public List<LibraryDocumentDTO> getAllDocuments() {
        return service.findAll().stream().map(LibraryDocumentDTO::fromEntity).collect(Collectors.toList());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id, HttpServletRequest request) throws MalformedURLException {
        LibraryDocument doc = service.findById(id).orElseThrow();
        Path filePath = libraryDir.resolve(doc.getFilePath());
        Resource resource = new UrlResource(filePath.toUri());
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // fallback
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                .body(resource);
    }

    // DTO для передачи данных на фронт
    public static class LibraryDocumentDTO {
        public Long id;
        public String fileName;
        public String comment;
        public String uploader;
        public LocalDateTime uploadDate;
        public String filePath;

        public static LibraryDocumentDTO fromEntity(LibraryDocument doc) {
            LibraryDocumentDTO dto = new LibraryDocumentDTO();
            dto.id = doc.getId();
            dto.fileName = doc.getFileName();
            dto.comment = doc.getComment();
            dto.uploader = doc.getUploader();
            dto.uploadDate = doc.getUploadDate();
            dto.filePath = doc.getFilePath();
            return dto;
        }
    }
} 
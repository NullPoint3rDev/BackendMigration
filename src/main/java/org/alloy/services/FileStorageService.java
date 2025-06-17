package org.alloy.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public UUID saveFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Создаем директорию, если она не существует
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            logger.info("Creating upload directory: {}", uploadPath);
            Files.createDirectories(uploadPath);
        }

        // Генерируем уникальное имя файла
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("Original filename is null");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        UUID fileId = UUID.randomUUID();
        String filename = fileId.toString() + extension;

        // Сохраняем файл
        Path filePath = uploadPath.resolve(filename);
        logger.debug("Saving file to: {}", filePath);
        Files.copy(file.getInputStream(), filePath);

        return fileId;
    }

    public byte[] getFile(UUID fileId) throws IOException {
        if (fileId == null) {
            throw new IllegalArgumentException("File ID is null");
        }

        Path uploadPath = Paths.get(uploadDir);
        logger.debug("Looking for file with ID: {} in directory: {}", fileId, uploadPath);

        // Ищем файл по UUID (без расширения)
        return Files.walk(uploadPath)
                .filter(path -> path.getFileName().toString().startsWith(fileId.toString()))
                .findFirst()
                .map(path -> {
                    try {
                        logger.debug("Found file at: {}", path);
                        return Files.readAllBytes(path);
                    } catch (IOException e) {
                        logger.error("Error reading file: {}", path, e);
                        throw new RuntimeException("Error reading file", e);
                    }
                })
                .orElseThrow(() -> {
                    logger.error("File not found with ID: {}", fileId);
                    return new IOException("File not found");
                });
    }
} 
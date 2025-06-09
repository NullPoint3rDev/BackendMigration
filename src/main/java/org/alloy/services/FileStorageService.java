package org.alloy.services;

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

    @Value("${file.upload-dir}")
    private String uploadDir;

    public UUID saveFile(MultipartFile file) throws IOException {
        // Создаем директорию, если она не существует
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Генерируем уникальное имя файла
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        UUID fileId = UUID.randomUUID();
        String filename = fileId.toString() + extension;

        // Сохраняем файл
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        return fileId;
    }

    public byte[] getFile(UUID fileId) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        // Ищем файл по UUID (без расширения)
        return Files.walk(uploadPath)
                .filter(path -> path.getFileName().toString().startsWith(fileId.toString()))
                .findFirst()
                .map(path -> {
                    try {
                        return Files.readAllBytes(path);
                    } catch (IOException e) {
                        throw new RuntimeException("Error reading file", e);
                    }
                })
                .orElseThrow(() -> new IOException("File not found"));
    }
} 
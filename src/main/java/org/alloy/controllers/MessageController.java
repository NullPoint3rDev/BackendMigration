package org.alloy.controllers;

import org.alloy.models.dto.MessageDTO;
import org.alloy.models.dto.AttachmentDTO;
import org.alloy.models.dto.mapper.AttachmentMapper;
import org.alloy.services.MessageService;
import org.alloy.models.entities.Attachment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    @Autowired
    private MessageService messageService;

    @GetMapping("/inbox")
    public List<MessageDTO> getInbox(@RequestParam Integer userId) {
        return messageService.getInbox(userId);
    }

    @GetMapping("/outbox")
    public List<MessageDTO> getOutbox(@RequestParam Integer userId) {
        return messageService.getOutbox(userId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MessageDTO sendMessage(
            @RequestPart("message") MessageDTO messageDTO,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) throws Exception {
        return messageService.sendMessage(messageDTO, files);
    }

    @GetMapping("/{id}")
    public MessageDTO getMessage(@PathVariable Long id) {
        return messageService.getMessage(id);
    }

    @DeleteMapping("/{id}")
    public void deleteMessage(@PathVariable Long id) {
        messageService.deleteMessage(id);
    }

    @PatchMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id) {
        messageService.markAsRead(id);
    }

    @GetMapping("/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) {
        Attachment att = messageService.getAttachment(attachmentId);
        if (att == null) return ResponseEntity.notFound().build();
        Resource file = new FileSystemResource(att.getFilePath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + att.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }
} 
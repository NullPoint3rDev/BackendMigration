package org.alloy.services;

import org.alloy.models.dto.MessageDTO;
import org.alloy.models.dto.mapper.MessageMapper;
import org.alloy.models.entities.Message;
import org.alloy.models.entities.Attachment;
import org.alloy.models.entities.UserAccount;
import org.alloy.repositories.MessageRepository;
import org.alloy.repositories.AttachmentRepository;
import org.alloy.repositories.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageService {
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private AttachmentRepository attachmentRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;

    private final String uploadDir = "uploads/messages/";

    public MessageDTO sendMessage(MessageDTO dto, List<MultipartFile> files) throws Exception {
        UserAccount sender = userAccountRepository.findById(dto.getSender().getId()).orElseThrow();
        UserAccount recipient = userAccountRepository.findById(dto.getRecipient().getId()).orElseThrow();

        Message message = new Message();
        message.setSubject(dto.getSubject());
        message.setBody(dto.getBody());
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setDateSent(LocalDateTime.now());
        message.setIsRead(false);

        if (files != null && !files.isEmpty()) {
            List<Attachment> attachments = files.stream().map(file -> {
                try {
                    String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                    Path path = Paths.get(uploadDir, filename);
                    Files.createDirectories(path.getParent());
                    Files.write(path, file.getBytes());
                    Attachment att = new Attachment();
                    att.setFilename(file.getOriginalFilename());
                    att.setFilePath(path.toString());
                    att.setMessage(message);
                    return att;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
            message.setAttachments(attachments);
        }

        Message saved = messageRepository.save(message);
        return MessageMapper.toDTO(saved);
    }

    public List<MessageDTO> getInbox(Integer userId) {
        UserAccount user = userAccountRepository.findById(userId).orElseThrow();
        return messageRepository.findByRecipient(user).stream()
                .map(MessageMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<MessageDTO> getOutbox(Integer userId) {
        UserAccount user = userAccountRepository.findById(userId).orElseThrow();
        return messageRepository.findBySender(user).stream()
                .map(MessageMapper::toDTO)
                .collect(Collectors.toList());
    }

    public MessageDTO getMessage(Long id) {
        return messageRepository.findById(id)
                .map(MessageMapper::toDTO)
                .orElse(null);
    }

    public void deleteMessage(Long id) {
        messageRepository.deleteById(id);
    }

    public void markAsRead(Long id) {
        Message msg = messageRepository.findById(id).orElseThrow();
        msg.setIsRead(true);
        messageRepository.save(msg);
    }

    public Attachment getAttachment(Long attachmentId) {
        return attachmentRepository.findById(attachmentId).orElse(null);
    }
} 
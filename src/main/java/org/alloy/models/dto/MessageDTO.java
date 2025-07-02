package org.alloy.models.dto;

import java.time.LocalDateTime;
import java.util.List;
import org.alloy.models.dto.AttachmentDTO;

public class MessageDTO {
    private Long id;
    private String subject;
    private String body;
    private UserAccountShortDTO sender;
    private UserAccountShortDTO recipient;
    private LocalDateTime dateSent;
    private Boolean isRead;
    private List<AttachmentDTO> attachments;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public UserAccountShortDTO getSender() { return sender; }
    public void setSender(UserAccountShortDTO sender) { this.sender = sender; }
    public UserAccountShortDTO getRecipient() { return recipient; }
    public void setRecipient(UserAccountShortDTO recipient) { this.recipient = recipient; }
    public LocalDateTime getDateSent() { return dateSent; }
    public void setDateSent(LocalDateTime dateSent) { this.dateSent = dateSent; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    public java.util.List<AttachmentDTO> getAttachments() { return attachments; }
    public void setAttachments(java.util.List<AttachmentDTO> attachments) { this.attachments = attachments; }
} 
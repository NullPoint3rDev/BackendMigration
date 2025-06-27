package org.alloy.models.dto;

public class InboxMessageDTO {
    private Integer id;
    private String subject;
    private String body;
    private UserAccountShortDTO sender;
    private UserAccountShortDTO recipient;
    // ... другие нужные поля
    // Геттеры и сеттеры
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public UserAccountShortDTO getSender() { return sender; }
    public void setSender(UserAccountShortDTO sender) { this.sender = sender; }
    public UserAccountShortDTO getRecipient() { return recipient; }
    public void setRecipient(UserAccountShortDTO recipient) { this.recipient = recipient; }
} 
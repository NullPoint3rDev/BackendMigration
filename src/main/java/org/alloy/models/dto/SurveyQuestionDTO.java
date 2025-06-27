package org.alloy.models.dto;

public class SurveyQuestionDTO {
    private Integer id;
    private String question;
    private String type;
    // ... другие нужные поля

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    // ... геттеры и сеттеры для других полей
} 
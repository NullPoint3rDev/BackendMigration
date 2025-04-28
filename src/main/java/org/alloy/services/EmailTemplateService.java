package org.alloy.services;

import org.alloy.models.entities.EmailTemplate;
import org.alloy.repositories.EmailTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmailTemplateService {
    @Autowired
    private EmailTemplateRepository emailTemplateRepository;

    public List<EmailTemplate> findAll() {
        return emailTemplateRepository.findAll();
    }

    public Optional<EmailTemplate> findById(Integer id) {
        return emailTemplateRepository.findById(id);
    }

    public EmailTemplate save(EmailTemplate emailTemplate) {
        return emailTemplateRepository.save(emailTemplate);
    }

    public void deleteById(Integer id) {
        emailTemplateRepository.deleteById(id);
    }
} 
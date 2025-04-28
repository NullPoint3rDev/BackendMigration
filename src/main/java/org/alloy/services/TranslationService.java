package org.alloy.services;

import org.alloy.models.entities.Translation;
import org.alloy.repositories.TranslationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TranslationService {
    @Autowired
    private TranslationRepository translationRepository;

    public List<Translation> findAll() {
        return translationRepository.findAll();
    }

    public Optional<Translation> findById(Integer id) {
        return translationRepository.findById(id);
    }

    public Translation save(Translation translation) {
        return translationRepository.save(translation);
    }

    public void deleteById(Integer id) {
        translationRepository.deleteById(id);
    }
} 
package org.alloy.services;

import org.alloy.models.entities.Dump;
import org.alloy.repositories.DumpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DumpService {
    @Autowired
    private DumpRepository dumpRepository;

    public List<Dump> findAll() {
        return dumpRepository.findAll();
    }

    public Optional<Dump> findById(Integer id) {
        return dumpRepository.findById(id);
    }

    public Dump save(Dump dump) {
        return dumpRepository.save(dump);
    }

    public void deleteById(Integer id) {
        dumpRepository.deleteById(id);
    }
} 
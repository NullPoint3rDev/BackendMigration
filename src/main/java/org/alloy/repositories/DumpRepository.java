package org.alloy.repositories;

import org.alloy.models.entities.Dump;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DumpRepository extends JpaRepository<Dump, Integer> {
} 
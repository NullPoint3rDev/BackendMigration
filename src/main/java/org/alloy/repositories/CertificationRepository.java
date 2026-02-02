package org.alloy.repositories;

import org.alloy.models.entities.Certification;
import org.alloy.models.entities.Welder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CertificationRepository extends JpaRepository<Certification, Long> {
    List<Certification> findByWelder(Welder welder);
    List<Certification> findByWelderId(Long welderId);
    void deleteByWelderId(Long welderId);
}


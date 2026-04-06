package org.alloy.repositories;

import org.alloy.models.entities.RfidPass;
import org.alloy.models.entities.Welder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RfidPassRepository extends JpaRepository<RfidPass, Long> {

    List<RfidPass> findByWelder(Welder welder);

    @Query("SELECT rp FROM RfidPass rp WHERE rp.welder.id = :welderId")
    List<RfidPass> findByWelderId(@Param("welderId") Long welderId);

    Optional<RfidPass> findByCode(String code);

    List<RfidPass> findAllByCode(String code);

    @Query("SELECT rp FROM RfidPass rp WHERE rp.code = :code AND rp.welder.department = :department")
    List<RfidPass> findByCodeAndDepartment(@Param("code") String code, @Param("department") String department);

    @Query("SELECT rp FROM RfidPass rp WHERE rp.code = :code AND rp.welder.department = :department AND rp.welder.id != :excludeWelderId")
    List<RfidPass> findByCodeAndDepartmentExcludingWelder(@Param("code") String code, @Param("department") String department, @Param("excludeWelderId") Long excludeWelderId);
}


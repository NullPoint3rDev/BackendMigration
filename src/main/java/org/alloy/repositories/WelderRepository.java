package org.alloy.repositories;

import org.alloy.models.entities.Welder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WelderRepository extends JpaRepository<Welder, Long> {
    
    List<Welder> findByStatus(Welder.WelderStatus status);
    
    List<Welder> findByDepartmentContainingIgnoreCase(String department);
    
    List<Welder> findByNameContainingIgnoreCase(String name);
    
    List<Welder> findByGrade(String grade);
    
    @Query("SELECT w FROM Welder w WHERE w.rfidCode = :rfidCode")
    Welder findByRfidCode(@Param("rfidCode") String rfidCode);
    
    @Query("SELECT w FROM Welder w WHERE w.employeeId = :employeeId")
    Welder findByEmployeeId(@Param("employeeId") String employeeId);
    
    @Query("SELECT w FROM Welder w WHERE " +
           "(:name IS NULL OR LOWER(w.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:status IS NULL OR w.status = :status) AND " +
           "(:department IS NULL OR LOWER(w.department) LIKE LOWER(CONCAT('%', :department, '%'))) AND " +
           "(:grade IS NULL OR w.grade = :grade)")
    List<Welder> findByFilters(@Param("name") String name, 
                               @Param("status") Welder.WelderStatus status, 
                               @Param("department") String department, 
                               @Param("grade") String grade);
}

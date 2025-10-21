package org.alloy.repositories;

import org.alloy.models.entities.Employee;
import org.alloy.models.GeneralStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    
    List<Employee> findByStatus(GeneralStatus status);
    
    List<Employee> findByOrganizationUnitId(Long organizationUnitId);
    
    List<Employee> findByUserRoleId(Long userRoleId);
    
    Optional<Employee> findByUsername(String username);
    
    List<Employee> findByFullNameContainingIgnoreCase(String fullName);
    
    List<Employee> findByEmailContainingIgnoreCase(String email);
    
    List<Employee> findByPositionContainingIgnoreCase(String position);
    
    @Query("SELECT e FROM Employee e WHERE " +
           "(:fullName IS NULL OR LOWER(e.fullName) LIKE LOWER(CONCAT('%', :fullName, '%'))) AND " +
           "(:email IS NULL OR LOWER(e.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:position IS NULL OR LOWER(e.position) LIKE LOWER(CONCAT('%', :position, '%'))) AND " +
           "(:organizationUnitId IS NULL OR e.organizationUnit.id = :organizationUnitId) AND " +
           "(:userRoleId IS NULL OR e.userRole.id = :userRoleId) AND " +
           "(:status IS NULL OR e.status = :status)")
    List<Employee> findByFilters(
        @Param("fullName") String fullName,
        @Param("email") String email,
        @Param("position") String position,
        @Param("organizationUnitId") Long organizationUnitId,
        @Param("userRoleId") Long userRoleId,
        @Param("status") GeneralStatus status
    );
    
    @Query("SELECT e FROM Employee e LEFT JOIN FETCH e.userRole LEFT JOIN FETCH e.organizationUnit")
    List<Employee> findAllWithRoles();
}

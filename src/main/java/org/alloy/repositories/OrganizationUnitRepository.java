package org.alloy.repositories;

import org.alloy.models.entities.OrganizationUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationUnitRepository extends JpaRepository<OrganizationUnit, Integer> {

    List<OrganizationUnit> findByOrganizationId(Integer organizationId);

    List<OrganizationUnit> findByOrganizationIdAndStatus(Integer organizationId, org.alloy.models.GeneralStatus status);

    List<OrganizationUnit> findByParentId(Integer parentId);

    Optional<OrganizationUnit> findByNameAndOrganizationId(String name, Integer organizationId);

    Optional<OrganizationUnit> findByNameAndOrganizationIdAndStatus(String name, Integer organizationId, org.alloy.models.GeneralStatus status);

    @Query("SELECT ou FROM OrganizationUnit ou WHERE ou.organizationId = :organizationId AND (ou.name LIKE %:searchTerm% OR ou.description LIKE %:searchTerm%)")
    List<OrganizationUnit> searchOrganizationUnits(@Param("organizationId") Integer organizationId, @Param("searchTerm") String searchTerm);

    @Query("SELECT ou FROM OrganizationUnit ou WHERE ou.id IN :ids")
    List<OrganizationUnit> findByIds(@Param("ids") List<Integer> ids);

    List<OrganizationUnit> findByStatus(org.alloy.models.GeneralStatus status);
}

package org.alloy.repositories;

import org.alloy.models.entities.Organization;
import org.alloy.models.GeneralStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Integer> {

    Optional<Organization> findByName(String name);

    List<Organization> findByStatus(GeneralStatus status);

    List<Organization> findByStatusNot(GeneralStatus status);

    @Query("SELECT o FROM Organization o WHERE o.name LIKE %:searchTerm% OR o.description LIKE %:searchTerm%")
    List<Organization> searchOrganizations(@Param("searchTerm") String searchTerm);

    @Query("SELECT o FROM Organization o WHERE (o.name LIKE %:searchTerm% OR o.description LIKE %:searchTerm%) AND o.status <> :deleted")
    List<Organization> searchOrganizationsNotDeleted(@Param("searchTerm") String searchTerm, @Param("deleted") GeneralStatus deleted);

    @Query("SELECT o FROM Organization o WHERE o.id IN :ids")
    List<Organization> findByIds(@Param("ids") List<Integer> ids);
}

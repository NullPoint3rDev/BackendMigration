package org.alloy.services;

import org.alloy.models.GeneralStatus;
import org.alloy.models.entities.Organization;
import org.alloy.repositories.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    @Autowired
    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    public List<Organization> getAllOrganizations() {
        return organizationRepository.findByStatusNot(GeneralStatus.Deleted);
    }

    public Optional<Organization> getOrganizationById(Integer id) {
        return organizationRepository.findById(id);
    }

    public Optional<Organization> getOrganizationByName(String name) {
        return organizationRepository.findByName(name);
    }

    public List<Organization> getOrganizationsByStatus(GeneralStatus status) {
        return organizationRepository.findByStatus(status);
    }

    public List<Organization> searchOrganizations(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllOrganizations();
        }
        return organizationRepository.searchOrganizationsNotDeleted(searchTerm, GeneralStatus.Deleted);
    }

    @Transactional
    public Organization createOrganization(Organization organization) {
        // Set default status if not provided
        if (organization.getStatus() == null) {
            organization.setStatus(GeneralStatus.Active);
        }

        return organizationRepository.save(organization);
    }

    @Transactional
    public Organization updateOrganization(Organization organization) {
        // Check if organization exists
        if (!organizationRepository.existsById(organization.getId())) {
            throw new IllegalArgumentException("Organization with ID " + organization.getId() + " does not exist");
        }

        return organizationRepository.save(organization);
    }

    @Transactional
    public void deleteOrganization(Integer id) {
        // Soft delete by setting status to Deleted
        Optional<Organization> organizationOpt = organizationRepository.findById(id);
        if (organizationOpt.isPresent()) {
            Organization organization = organizationOpt.get();
            organization.setStatus(GeneralStatus.Deleted);
            organizationRepository.save(organization);
        } else {
            throw new IllegalArgumentException("Organization with ID " + id + " does not exist");
        }
    }

    @Transactional
    public void hardDeleteOrganization(Integer id) {
        organizationRepository.deleteById(id);
    }
}

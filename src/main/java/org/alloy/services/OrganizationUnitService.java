package org.alloy.services;

import org.alloy.models.GeneralStatus;
import org.alloy.models.entities.OrganizationUnit;
import org.alloy.repositories.OrganizationUnitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class OrganizationUnitService {

    private final OrganizationUnitRepository organizationUnitRepository;

    @Autowired
    public OrganizationUnitService(OrganizationUnitRepository organizationUnitRepository) {
        this.organizationUnitRepository = organizationUnitRepository;
    }

    public List<OrganizationUnit> getAllOrganizationUnits() {
        return organizationUnitRepository.findByStatus(GeneralStatus.Active);
    }

    public Optional<OrganizationUnit> getOrganizationUnitById(Integer id) {
        return organizationUnitRepository.findById(id);
    }

    public List<OrganizationUnit> getOrganizationUnitsByOrganizationId(Integer organizationId) {
        return organizationUnitRepository.findByOrganizationId(organizationId);
    }

    public List<OrganizationUnit> getOrganizationUnitsByParentId(Integer parentId) {
        return organizationUnitRepository.findByParentId(parentId);
    }

    public Optional<OrganizationUnit> getOrganizationUnitByNameAndOrganizationId(String name, Integer organizationId) {
        return organizationUnitRepository.findByNameAndOrganizationId(name, organizationId);
    }

    public List<OrganizationUnit> searchOrganizationUnits(Integer organizationId, String searchTerm) {
        return organizationUnitRepository.searchOrganizationUnits(organizationId, searchTerm);
    }

    @Transactional
    public OrganizationUnit createOrganizationUnit(OrganizationUnit organizationUnit) {
        // Set default status if not provided
        if (organizationUnit.getStatus() == null) {
            organizationUnit.setStatus(GeneralStatus.Active);
        }

        // Check if name is already used by an ACTIVE unit in the same organization
        Optional<OrganizationUnit> existingUnit = organizationUnitRepository.findByNameAndOrganizationIdAndStatus(
                organizationUnit.getName(), organizationUnit.getOrganizationId(), GeneralStatus.Active);
        if (existingUnit.isPresent()) {
            throw new IllegalArgumentException("Organization unit with name '" + organizationUnit.getName() +
                    "' already exists in organization with ID " + organizationUnit.getOrganizationId());
        }

        return organizationUnitRepository.save(organizationUnit);
    }

    @Transactional
    public OrganizationUnit updateOrganizationUnit(OrganizationUnit organizationUnit) {
        // Check if organization unit exists
        if (!organizationUnitRepository.existsById(organizationUnit.getId())) {
            throw new IllegalArgumentException("Organization unit with ID " + organizationUnit.getId() + " does not exist");
        }

        // Check if name is already used by another ACTIVE unit in the same organization
        Optional<OrganizationUnit> existingUnit = organizationUnitRepository.findByNameAndOrganizationIdAndStatus(
                organizationUnit.getName(), organizationUnit.getOrganizationId(), GeneralStatus.Active);
        if (existingUnit.isPresent() && !existingUnit.get().getId().equals(organizationUnit.getId())) {
            throw new IllegalArgumentException("Organization unit with name '" + organizationUnit.getName() +
                    "' already exists in organization with ID " + organizationUnit.getOrganizationId());
        }

        return organizationUnitRepository.save(organizationUnit);
    }

    @Transactional
    public void deleteOrganizationUnit(Integer id) {
        // Soft delete by setting status to Deleted
        Optional<OrganizationUnit> organizationUnitOpt = organizationUnitRepository.findById(id);
        if (organizationUnitOpt.isPresent()) {
            OrganizationUnit organizationUnit = organizationUnitOpt.get();
            organizationUnit.setStatus(GeneralStatus.Deleted);
            organizationUnitRepository.save(organizationUnit);
        } else {
            throw new IllegalArgumentException("Organization unit with ID " + id + " does not exist");
        }
    }

    @Transactional
    public void hardDeleteOrganizationUnit(Integer id) {
        organizationUnitRepository.deleteById(id);
    }
}

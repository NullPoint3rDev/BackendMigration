package org.alloy.services;

import org.alloy.models.GeneralStatus;
import org.alloy.models.entities.OrganizationUnit;
import org.alloy.models.entities.Welder;
import org.alloy.repositories.OrganizationUnitRepository;
import org.alloy.repositories.WelderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class OrganizationUnitService {

    private final OrganizationUnitRepository organizationUnitRepository;
    private final WelderRepository welderRepository;

    @Autowired
    public OrganizationUnitService(OrganizationUnitRepository organizationUnitRepository, WelderRepository welderRepository) {
        this.organizationUnitRepository = organizationUnitRepository;
        this.welderRepository = welderRepository;
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
            String unitName = organizationUnit.getName();

            // Переносим сварщиков перед удалением
            transferWeldersToParentOrRoot(unitName, id);

            organizationUnit.setStatus(GeneralStatus.Deleted);
            organizationUnitRepository.save(organizationUnit);
        } else {
            throw new IllegalArgumentException("Organization unit with ID " + id + " does not exist");
        }
    }

    @Transactional
    public void hardDeleteOrganizationUnit(Integer id) {
        Optional<OrganizationUnit> organizationUnitOpt = organizationUnitRepository.findById(id);
        if (organizationUnitOpt.isPresent()) {
            OrganizationUnit organizationUnit = organizationUnitOpt.get();
            String unitName = organizationUnit.getName();

            // Переносим сварщиков перед удалением
            transferWeldersToParentOrRoot(unitName, id);
        }

        organizationUnitRepository.deleteById(id);
    }

    /**
     * Переносит сварщиков из удаляемого подразделения в родительское или корневое подразделение
     */
    private void transferWeldersToParentOrRoot(String deletedUnitName, Integer deletedUnitId) {
        // Находим всех сварщиков, привязанных к удаляемому подразделению
        List<Welder> welders = welderRepository.findByDepartmentContainingIgnoreCase(deletedUnitName);
        if (welders.isEmpty()) {
            return;
        }

        // Фильтруем только тех, у кого department точно совпадает (без учета регистра)
        welders = welders.stream()
                .filter(w -> w.getDepartment() != null && w.getDepartment().equalsIgnoreCase(deletedUnitName))
                .collect(java.util.stream.Collectors.toList());

        if (welders.isEmpty()) {
            return;
        }

        // Определяем целевое подразделение
        String targetDepartmentName = findTargetDepartment(deletedUnitId);

        // Обновляем department у всех найденных сварщиков
        for (Welder welder : welders) {
            welder.setDepartment(targetDepartmentName);
        }
        welderRepository.saveAll(welders);
    }

    /**
     * Находит целевое подразделение для переноса сварщиков:
     * 1. Родительское подразделение удаляемого
     * 2. Если родительского нет, то одно из корневых: "Компания Alloy", "Alloy", "Эллой"
     */
    private String findTargetDepartment(Integer deletedUnitId) {
        Optional<OrganizationUnit> deletedUnitOpt = organizationUnitRepository.findById(deletedUnitId);
        if (!deletedUnitOpt.isPresent()) {
            return findRootDepartment();
        }

        OrganizationUnit deletedUnit = deletedUnitOpt.get();

        // Пытаемся найти родительское подразделение
        if (deletedUnit.getParentId() != null) {
            Optional<OrganizationUnit> parentOpt = organizationUnitRepository.findById(deletedUnit.getParentId());
            if (parentOpt.isPresent()) {
                OrganizationUnit parent = parentOpt.get();
                // Проверяем, что родительское подразделение активно
                if (parent.getStatus() == GeneralStatus.Active) {
                    return parent.getName();
                }
            }
        }

        // Если родительского нет или оно неактивно, ищем корневое подразделение
        return findRootDepartment();
    }

    /**
     * Находит корневое подразделение в порядке приоритета:
     * 1. "Компания Alloy"
     * 2. "Alloy"
     * 3. "Эллой"
     * Если ни одно не найдено, возвращает первое активное корневое подразделение
     */
    private String findRootDepartment() {
        // Список приоритетных названий корневых подразделений
        String[] rootNames = {"Компания Alloy", "Alloy", "Эллой"};

        // Ищем корневые подразделения (без parentId или с parentId = null)
        // Используем findByStatus для оптимизации
        List<OrganizationUnit> rootUnits = organizationUnitRepository.findByStatus(GeneralStatus.Active).stream()
                .filter(unit -> unit.getParentId() == null)
                .collect(java.util.stream.Collectors.toList());

        // Ищем по приоритету
        for (String rootName : rootNames) {
            Optional<OrganizationUnit> found = rootUnits.stream()
                    .filter(unit -> unit.getName().equalsIgnoreCase(rootName))
                    .findFirst();
            if (found.isPresent()) {
                return found.get().getName();
            }
        }

        // Если ни одно приоритетное не найдено, возвращаем первое корневое подразделение
        if (!rootUnits.isEmpty()) {
            return rootUnits.get(0).getName();
        }

        // Если вообще нет корневых подразделений, возвращаем пустую строку
        // (в этом случае сварщики останутся без подразделения, но это крайний случай)
        return "";
    }
}

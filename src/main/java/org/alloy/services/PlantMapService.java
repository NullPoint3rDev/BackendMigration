package org.alloy.services;

import org.alloy.models.dto.PlantMapDTO;
import org.alloy.models.dto.PlantMapElementDTO;
import org.alloy.models.dto.PlantMapWorkshopDTO;
import org.alloy.models.entities.*;
import org.alloy.models.GeneralStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PlantMapService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private WeldingMachineService weldingMachineService;

    /**
     * Получить карту предприятия по ID
     */
    public PlantMapDTO getPlantMap(Integer plantMapId) {
        PlantMap plantMap = entityManager.find(PlantMap.class, plantMapId);
        if (plantMap == null) {
            return null;
        }

        PlantMapDTO dto = convertToDTO(plantMap);
        
        // Загружаем элементы карты
        List<PlantMapElement> elements = entityManager
            .createQuery("SELECT e FROM PlantMapElement e WHERE e.plantMapId = :plantMapId AND e.status = :status", PlantMapElement.class)
            .setParameter("plantMapId", plantMapId)
            .setParameter("status", GeneralStatus.Active)
            .getResultList();

        dto.setElements(elements.stream()
            .map(this::convertElementToDTO)
            .collect(Collectors.toList()));

        // Загружаем цеха
        List<PlantMapWorkshop> workshops = entityManager
            .createQuery("SELECT w FROM PlantMapWorkshop w WHERE w.plantMapId = :plantMapId AND w.status = :status", PlantMapWorkshop.class)
            .setParameter("plantMapId", plantMapId)
            .setParameter("status", GeneralStatus.Active)
            .getResultList();

        dto.setWorkshops(workshops.stream()
            .map(this::convertWorkshopToDTO)
            .collect(Collectors.toList()));

        return dto;
    }

    /**
     * Получить карту предприятия по умолчанию для организации
     */
    public PlantMapDTO getDefaultPlantMap(Integer organizationId) {
        PlantMap plantMap = entityManager
            .createQuery("SELECT p FROM PlantMap p WHERE p.organizationId = :organizationId AND p.isDefault = true AND p.status = :status", PlantMap.class)
            .setParameter("organizationId", organizationId)
            .setParameter("status", GeneralStatus.Active)
            .getResultList()
            .stream()
            .findFirst()
            .orElse(null);

        if (plantMap == null) {
            return null;
        }

        return getPlantMap(plantMap.getId());
    }

    /**
     * Создать новую карту предприятия
     */
    public PlantMapDTO createPlantMap(PlantMapDTO plantMapDTO) {
        PlantMap plantMap = new PlantMap();
        plantMap.setOrganizationId(plantMapDTO.getOrganizationId());
        plantMap.setName(plantMapDTO.getName());
        plantMap.setDescription(plantMapDTO.getDescription());
        plantMap.setWidth(plantMapDTO.getWidth());
        plantMap.setHeight(plantMapDTO.getHeight());
        plantMap.setBackgroundImage(plantMapDTO.getBackgroundImage());
        plantMap.setStatus(GeneralStatus.Active);
        plantMap.setIsDefault(plantMapDTO.getIsDefault());

        entityManager.persist(plantMap);
        entityManager.flush();

        return getPlantMap(plantMap.getId());
    }

    /**
     * Обновить карту предприятия
     */
    public PlantMapDTO updatePlantMap(Integer plantMapId, PlantMapDTO plantMapDTO) {
        PlantMap plantMap = entityManager.find(PlantMap.class, plantMapId);
        if (plantMap == null) {
            return null;
        }

        plantMap.setName(plantMapDTO.getName());
        plantMap.setDescription(plantMapDTO.getDescription());
        plantMap.setWidth(plantMapDTO.getWidth());
        plantMap.setHeight(plantMapDTO.getHeight());
        plantMap.setBackgroundImage(plantMapDTO.getBackgroundImage());
        plantMap.setIsDefault(plantMapDTO.getIsDefault());

        entityManager.merge(plantMap);
        return getPlantMap(plantMapId);
    }

    /**
     * Добавить элемент на карту
     */
    public PlantMapElementDTO addElementToMap(Integer plantMapId, PlantMapElementDTO elementDTO) {
        PlantMapElement element = new PlantMapElement();
        element.setPlantMapId(plantMapId);
        element.setElementType(elementDTO.getElementType());
        element.setElementId(elementDTO.getElementId());
        element.setPositionX(elementDTO.getPositionX());
        element.setPositionY(elementDTO.getPositionY());
        element.setWidth(elementDTO.getWidth());
        element.setHeight(elementDTO.getHeight());
        element.setRotation(elementDTO.getRotation());
        element.setZIndex(elementDTO.getZIndex());
        element.setStatus(GeneralStatus.Active);

        entityManager.persist(element);
        entityManager.flush();

        return convertElementToDTO(element);
    }

    /**
     * Обновить позицию элемента на карте
     */
    public PlantMapElementDTO updateElementPosition(Integer elementId, Double positionX, Double positionY) {
        PlantMapElement element = entityManager.find(PlantMapElement.class, elementId);
        if (element == null) {
            return null;
        }

        element.setPositionX(positionX);
        element.setPositionY(positionY);

        entityManager.merge(element);
        return convertElementToDTO(element);
    }

    /**
     * Удалить элемент с карты
     */
    public boolean removeElementFromMap(Integer elementId) {
        PlantMapElement element = entityManager.find(PlantMapElement.class, elementId);
        if (element == null) {
            return false;
        }

        element.setStatus(GeneralStatus.Inactive);
        entityManager.merge(element);
        return true;
    }

    /**
     * Добавить цех на карту
     */
    public PlantMapWorkshopDTO addWorkshopToMap(Integer plantMapId, PlantMapWorkshopDTO workshopDTO) {
        PlantMapWorkshop workshop = new PlantMapWorkshop();
        workshop.setPlantMapId(plantMapId);
        workshop.setName(workshopDTO.getName());
        workshop.setDescription(workshopDTO.getDescription());
        workshop.setPositionX(workshopDTO.getPositionX());
        workshop.setPositionY(workshopDTO.getPositionY());
        workshop.setWidth(workshopDTO.getWidth());
        workshop.setHeight(workshopDTO.getHeight());
        workshop.setColor(workshopDTO.getColor());
        workshop.setBorderColor(workshopDTO.getBorderColor());
        workshop.setOpacity(workshopDTO.getOpacity());
        workshop.setStatus(GeneralStatus.Active);

        entityManager.persist(workshop);
        entityManager.flush();

        return convertWorkshopToDTO(workshop);
    }

    /**
     * Обновить цех на карте
     */
    public PlantMapWorkshopDTO updateWorkshop(Integer workshopId, PlantMapWorkshopDTO workshopDTO) {
        PlantMapWorkshop workshop = entityManager.find(PlantMapWorkshop.class, workshopId);
        if (workshop == null) {
            return null;
        }

        workshop.setName(workshopDTO.getName());
        workshop.setDescription(workshopDTO.getDescription());
        workshop.setPositionX(workshopDTO.getPositionX());
        workshop.setPositionY(workshopDTO.getPositionY());
        workshop.setWidth(workshopDTO.getWidth());
        workshop.setHeight(workshopDTO.getHeight());
        workshop.setColor(workshopDTO.getColor());
        workshop.setBorderColor(workshopDTO.getBorderColor());
        workshop.setOpacity(workshopDTO.getOpacity());

        entityManager.merge(workshop);
        return convertWorkshopToDTO(workshop);
    }

    /**
     * Удалить цех с карты
     */
    public boolean removeWorkshopFromMap(Integer workshopId) {
        PlantMapWorkshop workshop = entityManager.find(PlantMapWorkshop.class, workshopId);
        if (workshop == null) {
            return false;
        }

        workshop.setStatus(GeneralStatus.Inactive);
        entityManager.merge(workshop);
        return true;
    }

    /**
     * Получить список доступного сварочного оборудования для организации
     */
    public List<WeldingMachine> getAvailableWeldingMachines(Integer organizationId) {
        return entityManager
            .createQuery("SELECT w FROM WeldingMachine w JOIN w.organizationUnit ou WHERE ou.organizationId = :organizationId AND w.status = :status", WeldingMachine.class)
            .setParameter("organizationId", organizationId)
            .setParameter("status", GeneralStatus.Active)
            .getResultList();
    }

    // Приватные методы конвертации
    private PlantMapDTO convertToDTO(PlantMap plantMap) {
        PlantMapDTO dto = new PlantMapDTO();
        dto.setId(plantMap.getId());
        dto.setOrganizationId(plantMap.getOrganizationId());
        dto.setName(plantMap.getName());
        dto.setDescription(plantMap.getDescription());
        dto.setWidth(plantMap.getWidth());
        dto.setHeight(plantMap.getHeight());
        dto.setBackgroundImage(plantMap.getBackgroundImage());
        dto.setStatus(plantMap.getStatus());
        dto.setDateCreated(plantMap.getDateCreated());
        dto.setDateUpdated(plantMap.getDateUpdated());
        dto.setIsDefault(plantMap.getIsDefault());
        return dto;
    }

    private PlantMapElementDTO convertElementToDTO(PlantMapElement element) {
        PlantMapElementDTO dto = new PlantMapElementDTO();
        dto.setId(element.getId());
        dto.setPlantMapId(element.getPlantMapId());
        dto.setElementType(element.getElementType());
        dto.setElementId(element.getElementId());
        dto.setPositionX(element.getPositionX());
        dto.setPositionY(element.getPositionY());
        dto.setWidth(element.getWidth());
        dto.setHeight(element.getHeight());
        dto.setRotation(element.getRotation());
        dto.setZIndex(element.getZIndex());
        dto.setStatus(element.getStatus());
        dto.setDateCreated(element.getDateCreated());
        dto.setDateUpdated(element.getDateUpdated());

        // Загружаем дополнительную информацию об элементе
        if (element.getElementType() == PlantMapElement.ElementType.WELDING_MACHINE) {
            WeldingMachine machine = entityManager.find(WeldingMachine.class, element.getElementId());
            if (machine != null) {
                dto.setElementName(machine.getName());
                dto.setElementDescription(machine.getDescription());
                dto.setIcon("🔧"); // Иконка сварочного аппарата
            }
        }

        return dto;
    }

    private PlantMapWorkshopDTO convertWorkshopToDTO(PlantMapWorkshop workshop) {
        PlantMapWorkshopDTO dto = new PlantMapWorkshopDTO();
        dto.setId(workshop.getId());
        dto.setPlantMapId(workshop.getPlantMapId());
        dto.setName(workshop.getName());
        dto.setDescription(workshop.getDescription());
        dto.setPositionX(workshop.getPositionX());
        dto.setPositionY(workshop.getPositionY());
        dto.setWidth(workshop.getWidth());
        dto.setHeight(workshop.getHeight());
        dto.setColor(workshop.getColor());
        dto.setBorderColor(workshop.getBorderColor());
        dto.setOpacity(workshop.getOpacity());
        dto.setStatus(workshop.getStatus());
        dto.setDateCreated(workshop.getDateCreated());
        dto.setDateUpdated(workshop.getDateUpdated());
        return dto;
    }
}

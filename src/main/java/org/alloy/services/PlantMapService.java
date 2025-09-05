package org.alloy.services;

import org.alloy.models.dto.PlantMapDTO;
import org.alloy.models.dto.PlantMapElementDTO;
import org.alloy.models.dto.PlantMapWorkshopDTO;
import org.alloy.models.entities.*;
import org.alloy.models.GeneralStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(PlantMapService.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private WeldingMachineService weldingMachineService;

    /**
     * Получить карту предприятия по ID
     */
    public PlantMapDTO getPlantMap(Integer plantMapId) {
        logger.debug("Поиск карты предприятия с ID: {}", plantMapId);
        
        PlantMap plantMap = entityManager.find(PlantMap.class, plantMapId);
        if (plantMap == null) {
            logger.warn("Карта предприятия с ID {} не найдена в базе данных", plantMapId);
            return null;
        }

        logger.debug("Карта предприятия с ID {} найдена, загружаем элементы", plantMapId);
        PlantMapDTO dto = convertToDTO(plantMap);
        
        // Загружаем элементы карты
        List<PlantMapElement> elements = entityManager
            .createQuery("SELECT e FROM PlantMapElement e WHERE e.plantMapId = :plantMapId AND e.status = :status", PlantMapElement.class)
            .setParameter("plantMapId", plantMapId)
            .setParameter("status", GeneralStatus.Active)
            .getResultList();

        logger.debug("Найдено {} элементов на карте", elements.size());
        dto.setElements(elements.stream()
            .map(this::convertElementToDTO)
            .collect(Collectors.toList()));

        // Загружаем цеха
        List<PlantMapWorkshop> workshops = entityManager
            .createQuery("SELECT w FROM PlantMapWorkshop w WHERE w.plantMapId = :plantMapId AND w.status = :status", PlantMapWorkshop.class)
            .setParameter("plantMapId", plantMapId)
            .setParameter("status", GeneralStatus.Active)
            .getResultList();

        logger.debug("Найдено {} цехов на карте", workshops.size());
        dto.setWorkshops(workshops.stream()
            .map(this::convertWorkshopToDTO)
            .collect(Collectors.toList()));

        logger.info("Карта предприятия с ID {} успешно загружена с {} элементами и {} цехами", 
                   plantMapId, elements.size(), workshops.size());
        return dto;
    }

    /**
     * Получить карту предприятия по умолчанию для организации
     */
    public PlantMapDTO getDefaultPlantMap(Integer organizationId) {
        logger.debug("Поиск карты по умолчанию для организации ID: {}", organizationId);
        
        try {
            List<PlantMap> plantMaps = entityManager
                .createQuery("SELECT p FROM PlantMap p WHERE p.organizationId = :organizationId AND p.isDefault = true AND p.status = :status", PlantMap.class)
                .setParameter("organizationId", organizationId)
                .setParameter("status", GeneralStatus.Active)
                .getResultList();

            if (plantMaps.isEmpty()) {
                logger.warn("Карта по умолчанию для организации ID {} не найдена, создаем новую", organizationId);
                return createDefaultPlantMap(organizationId);
            }

            PlantMap plantMap = plantMaps.get(0);
            logger.debug("Найдена карта по умолчанию с ID: {} для организации ID: {}", plantMap.getId(), organizationId);
            
            return getPlantMap(plantMap.getId());
        } catch (Exception e) {
            logger.error("Ошибка при поиске карты по умолчанию для организации ID {}: {}", organizationId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Создать карту по умолчанию для организации
     */
    private PlantMapDTO createDefaultPlantMap(Integer organizationId) {
        logger.info("Создание карты по умолчанию для организации ID: {}", organizationId);
        
        try {
            PlantMap plantMap = new PlantMap();
            plantMap.setOrganizationId(organizationId);
            plantMap.setName("Основная карта предприятия");
            plantMap.setDescription("Схематичная карта основного производственного комплекса");
            plantMap.setWidth(1200);
            plantMap.setHeight(800);
            plantMap.setBackgroundImage(null);
            plantMap.setStatus(GeneralStatus.Active);
            plantMap.setIsDefault(true);

            entityManager.persist(plantMap);
            entityManager.flush();

            logger.info("Карта по умолчанию успешно создана с ID: {} для организации ID: {}", 
                       plantMap.getId(), organizationId);

            return getPlantMap(plantMap.getId());
        } catch (Exception e) {
            logger.error("Ошибка при создании карты по умолчанию для организации ID {}: {}", 
                        organizationId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Создать новую карту предприятия
     */
    public PlantMapDTO createPlantMap(PlantMapDTO plantMapDTO) {
        logger.info("Создание новой карты предприятия для организации ID: {}", plantMapDTO.getOrganizationId());
        
        try {
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

            logger.info("Карта предприятия успешно создана с ID: {} для организации ID: {}", 
                       plantMap.getId(), plantMapDTO.getOrganizationId());

            return getPlantMap(plantMap.getId());
        } catch (Exception e) {
            logger.error("Ошибка при создании карты предприятия для организации ID {}: {}", 
                        plantMapDTO.getOrganizationId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Обновить карту предприятия
     */
    public PlantMapDTO updatePlantMap(Integer plantMapId, PlantMapDTO plantMapDTO) {
        logger.info("Обновление карты предприятия с ID: {}", plantMapId);
        
        try {
            PlantMap plantMap = entityManager.find(PlantMap.class, plantMapId);
            if (plantMap == null) {
                logger.warn("Карта предприятия с ID {} не найдена для обновления", plantMapId);
                return null;
            }

            plantMap.setName(plantMapDTO.getName());
            plantMap.setDescription(plantMapDTO.getDescription());
            plantMap.setWidth(plantMapDTO.getWidth());
            plantMap.setHeight(plantMapDTO.getHeight());
            plantMap.setBackgroundImage(plantMapDTO.getBackgroundImage());
            plantMap.setIsDefault(plantMapDTO.getIsDefault());

            entityManager.merge(plantMap);
            logger.info("Карта предприятия с ID {} успешно обновлена", plantMapId);
            
            return getPlantMap(plantMapId);
        } catch (Exception e) {
            logger.error("Ошибка при обновлении карты предприятия с ID {}: {}", plantMapId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Добавить элемент на карту
     */
    public PlantMapElementDTO addElementToMap(Integer plantMapId, PlantMapElementDTO elementDTO) {
        logger.info("Добавление элемента типа {} на карту ID: {}", elementDTO.getElementType(), plantMapId);
        
        try {
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

            logger.info("Элемент типа {} успешно добавлен на карту ID: {} с ID элемента: {}", 
                       elementDTO.getElementType(), plantMapId, element.getId());

            return convertElementToDTO(element);
        } catch (Exception e) {
            logger.error("Ошибка при добавлении элемента на карту ID {}: {}", plantMapId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Обновить позицию элемента на карте
     */
    public PlantMapElementDTO updateElementPosition(Integer elementId, Double positionX, Double positionY) {
        logger.debug("Обновление позиции элемента ID: {} на координаты ({}, {})", elementId, positionX, positionY);
        
        try {
            PlantMapElement element = entityManager.find(PlantMapElement.class, elementId);
            if (element == null) {
                logger.warn("Элемент с ID {} не найден для обновления позиции", elementId);
                return null;
            }

            element.setPositionX(positionX);
            element.setPositionY(positionY);

            entityManager.merge(element);
            logger.debug("Позиция элемента ID {} успешно обновлена на ({}, {})", elementId, positionX, positionY);
            
            return convertElementToDTO(element);
        } catch (Exception e) {
            logger.error("Ошибка при обновлении позиции элемента ID {}: {}", elementId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Удалить элемент с карты
     */
    public boolean removeElementFromMap(Integer elementId) {
        logger.info("Удаление элемента с ID: {}", elementId);
        
        try {
            PlantMapElement element = entityManager.find(PlantMapElement.class, elementId);
            if (element == null) {
                logger.warn("Элемент с ID {} не найден для удаления", elementId);
                return false;
            }

            element.setStatus(GeneralStatus.Inactive);
            entityManager.merge(element);
            logger.info("Элемент с ID {} успешно удален (статус изменен на Inactive)", elementId);
            return true;
        } catch (Exception e) {
            logger.error("Ошибка при удалении элемента с ID {}: {}", elementId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Добавить цех на карту
     */
    public PlantMapWorkshopDTO addWorkshopToMap(Integer plantMapId, PlantMapWorkshopDTO workshopDTO) {
        logger.info("Добавление цеха '{}' на карту ID: {}", workshopDTO.getName(), plantMapId);
        
        try {
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

            logger.info("Цех '{}' успешно добавлен на карту ID: {} с ID цеха: {}", 
                       workshopDTO.getName(), plantMapId, workshop.getId());

            return convertWorkshopToDTO(workshop);
        } catch (Exception e) {
            logger.error("Ошибка при добавлении цеха на карту ID {}: {}", plantMapId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Обновить цех на карте
     */
    public PlantMapWorkshopDTO updateWorkshop(Integer workshopId, PlantMapWorkshopDTO workshopDTO) {
        logger.info("Обновление цеха с ID: {}", workshopId);
        
        try {
            PlantMapWorkshop workshop = entityManager.find(PlantMapWorkshop.class, workshopId);
            if (workshop == null) {
                logger.warn("Цех с ID {} не найден для обновления", workshopId);
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
            logger.info("Цех с ID {} успешно обновлен", workshopId);
            
            return convertWorkshopToDTO(workshop);
        } catch (Exception e) {
            logger.error("Ошибка при обновлении цеха с ID {}: {}", workshopId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Удалить цех с карты
     */
    public boolean removeWorkshopFromMap(Integer workshopId) {
        logger.info("Удаление цеха с ID: {}", workshopId);
        
        try {
            PlantMapWorkshop workshop = entityManager.find(PlantMapWorkshop.class, workshopId);
            if (workshop == null) {
                logger.warn("Цех с ID {} не найден для удаления", workshopId);
                return false;
            }

            workshop.setStatus(GeneralStatus.Inactive);
            entityManager.merge(workshop);
            logger.info("Цех с ID {} успешно удален (статус изменен на Inactive)", workshopId);
            return true;
        } catch (Exception e) {
            logger.error("Ошибка при удалении цеха с ID {}: {}", workshopId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Получить список доступного сварочного оборудования для организации
     */
    public List<WeldingMachine> getAvailableWeldingMachines(Integer organizationId) {
        logger.debug("Поиск доступного сварочного оборудования для организации ID: {}", organizationId);
        
        try {
            // Используем более надежный запрос с явным JOIN через organizationUnitId
            List<WeldingMachine> machines = entityManager
                .createQuery("SELECT w FROM WeldingMachine w JOIN OrganizationUnit ou ON w.organizationUnitId = ou.id WHERE ou.organizationId = :organizationId AND w.status = :status", WeldingMachine.class)
                .setParameter("organizationId", organizationId)
                .setParameter("status", GeneralStatus.Active)
                .getResultList();

            logger.info("Для организации ID {} найдено {} единиц доступного сварочного оборудования", organizationId, machines.size());
            
            // Дополнительная диагностика
            if (machines.isEmpty()) {
                logger.warn("Не найдено сварочных аппаратов для организации ID {}. Проверяем данные в базе:", organizationId);
                
                // Проверяем, есть ли подразделения для этой организации
                Long unitCount = entityManager
                    .createQuery("SELECT COUNT(ou) FROM OrganizationUnit ou WHERE ou.organizationId = :organizationId AND ou.status = :status", Long.class)
                    .setParameter("organizationId", organizationId)
                    .setParameter("status", GeneralStatus.Active)
                    .getSingleResult();
                logger.warn("Найдено {} активных подразделений для организации ID {}", unitCount, organizationId);
                
                // Проверяем общее количество сварочных аппаратов
                Long machineCount = entityManager
                    .createQuery("SELECT COUNT(w) FROM WeldingMachine w WHERE w.status = :status", Long.class)
                    .setParameter("status", GeneralStatus.Active)
                    .getSingleResult();
                logger.warn("Всего активных сварочных аппаратов в системе: {}", machineCount);
            }
            
            return machines;
        } catch (Exception e) {
            logger.error("Ошибка при поиске доступного сварочного оборудования для организации ID {}: {}", 
                        organizationId, e.getMessage(), e);
            throw e;
        }
    }

    // Приватные методы конвертации
    private PlantMapDTO convertToDTO(PlantMap plantMap) {
        logger.debug("Конвертация PlantMap в DTO для ID: {}", plantMap.getId());
        
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
        logger.debug("Конвертация PlantMapElement в DTO для ID: {}", element.getId());
        
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
            try {
                WeldingMachine machine = entityManager.find(WeldingMachine.class, element.getElementId());
                if (machine != null) {
                    dto.setElementName(machine.getName());
                    dto.setElementDescription(machine.getDescription());
                    dto.setIcon("🔧"); // Иконка сварочного аппарата
                    logger.debug("Дополнительная информация для сварочного аппарата ID: {} загружена", element.getElementId());
                } else {
                    logger.warn("Сварочный аппарат с ID {} не найден", element.getElementId());
                }
            } catch (Exception e) {
                logger.error("Ошибка при загрузке дополнительной информации для элемента ID {}: {}", 
                            element.getElementId(), e.getMessage());
            }
        }

        return dto;
    }

    private PlantMapWorkshopDTO convertWorkshopToDTO(PlantMapWorkshop workshop) {
        logger.debug("Конвертация PlantMapWorkshop в DTO для ID: {}", workshop.getId());
        
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

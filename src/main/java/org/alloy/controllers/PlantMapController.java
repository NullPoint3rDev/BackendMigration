package org.alloy.controllers;

import org.alloy.models.dto.PlantMapDTO;
import org.alloy.models.dto.PlantMapElementDTO;
import org.alloy.models.dto.PlantMapWorkshopDTO;
import org.alloy.models.entities.WeldingMachine;
import org.alloy.models.entities.OrganizationUnit;
import org.alloy.services.PlantMapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/plant-map")
@CrossOrigin(origins = "*")
public class PlantMapController {

    private static final Logger logger = LoggerFactory.getLogger(PlantMapController.class);

    @Autowired
    private PlantMapService plantMapService;

    /**
     * Простой тестовый эндпоинт для проверки работы контроллера
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        logger.info("Получен тестовый запрос к PlantMapController");
        return ResponseEntity.ok("PlantMapController работает! Время: " + System.currentTimeMillis());
    }

    /**
     * Получить карту предприятия по ID
     */
    @GetMapping("/{plantMapId}")
    public ResponseEntity<PlantMapDTO> getPlantMap(@PathVariable Integer plantMapId) {
        PlantMapDTO plantMap = plantMapService.getPlantMap(plantMapId);
        if (plantMap == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(plantMap);
    }

    /**
     * Получить карту предприятия по умолчанию для организации
     */
    @GetMapping("/default/{organizationId}")
    public ResponseEntity<PlantMapDTO> getDefaultPlantMap(@PathVariable Integer organizationId) {
        PlantMapDTO plantMap = plantMapService.getDefaultPlantMap(organizationId);
        if (plantMap == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(plantMap);
    }

    /**
     * Создать новую карту предприятия
     */
    @PostMapping
    public ResponseEntity<PlantMapDTO> createPlantMap(@RequestBody PlantMapDTO plantMapDTO) {
        PlantMapDTO createdPlantMap = plantMapService.createPlantMap(plantMapDTO);
        return ResponseEntity.ok(createdPlantMap);
    }

    /**
     * Обновить карту предприятия
     */
    @PutMapping("/{plantMapId}")
    public ResponseEntity<PlantMapDTO> updatePlantMap(@PathVariable Integer plantMapId, @RequestBody PlantMapDTO plantMapDTO) {
        PlantMapDTO updatedPlantMap = plantMapService.updatePlantMap(plantMapId, plantMapDTO);
        if (updatedPlantMap == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedPlantMap);
    }

    /**
     * Добавить элемент на карту
     */
    @PostMapping("/{plantMapId}/elements")
    public ResponseEntity<PlantMapElementDTO> addElementToMap(@PathVariable Integer plantMapId, @RequestBody PlantMapElementDTO elementDTO) {
        PlantMapElementDTO addedElement = plantMapService.addElementToMap(plantMapId, elementDTO);
        return ResponseEntity.ok(addedElement);
    }

    /**
     * Обновить позицию элемента на карте
     */
    @PutMapping("/elements/{elementId}/position")
    public ResponseEntity<PlantMapElementDTO> updateElementPosition(
            @PathVariable Integer elementId,
            @RequestParam Double positionX,
            @RequestParam Double positionY) {
        PlantMapElementDTO updatedElement = plantMapService.updateElementPosition(elementId, positionX, positionY);
        if (updatedElement == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedElement);
    }

    /**
     * Удалить элемент с карты
     */
    @DeleteMapping("/elements/{elementId}")
    public ResponseEntity<Void> removeElementFromMap(@PathVariable Integer elementId) {
        boolean removed = plantMapService.removeElementFromMap(elementId);
        if (!removed) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Добавить цех на карту
     */
    @PostMapping("/{plantMapId}/workshops")
    public ResponseEntity<PlantMapWorkshopDTO> addWorkshopToMap(@PathVariable Integer plantMapId, @RequestBody PlantMapWorkshopDTO workshopDTO) {
        PlantMapWorkshopDTO addedWorkshop = plantMapService.addWorkshopToMap(plantMapId, workshopDTO);
        return ResponseEntity.ok(addedWorkshop);
    }

    /**
     * Обновить цех на карте
     */
    @PutMapping("/workshops/{workshopId}")
    public ResponseEntity<PlantMapWorkshopDTO> updateWorkshop(@PathVariable Integer workshopId, @RequestBody PlantMapWorkshopDTO workshopDTO) {
        PlantMapWorkshopDTO updatedWorkshop = plantMapService.updateWorkshop(workshopId, workshopDTO);
        if (updatedWorkshop == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedWorkshop);
    }

    /**
     * Удалить цех с карты
     */
    @DeleteMapping("/workshops/{workshopId}")
    public ResponseEntity<Void> removeWorkshopFromMap(@PathVariable Integer workshopId) {
        boolean removed = plantMapService.removeWorkshopFromMap(workshopId);
        if (!removed) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Получить список подразделений организации для размещения на карте
     */
    @GetMapping("/available-units/{organizationId}")
    public ResponseEntity<List<OrganizationUnit>> getAvailableOrganizationUnits(@PathVariable Integer organizationId) {
        List<OrganizationUnit> units = plantMapService.getAvailableOrganizationUnits(organizationId);
        return ResponseEntity.ok(units);
    }

    /**
     * Получить список доступного сварочного оборудования для организации
     */
    @GetMapping("/available-equipment/{organizationId}")
    public ResponseEntity<List<WeldingMachine>> getAvailableWeldingMachines(@PathVariable Integer organizationId) {
        List<WeldingMachine> machines = plantMapService.getAvailableWeldingMachines(organizationId);
        return ResponseEntity.ok(machines);
    }
}

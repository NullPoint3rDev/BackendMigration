package org.alloy.services;

import org.alloy.models.entities.WeldingMachine;
import org.alloy.models.entities.WeldingMachineState;
import org.alloy.models.weldingmachine.StateSummary;
import org.alloy.models.WeldingMachineStatus;
import org.alloy.repositories.WeldingMachineRepository;
import org.alloy.repositories.WeldingMachineStateRepository;
import org.alloy.repositories.OrganizationUnitRepository;
import org.alloy.repositories.WeldingMachineTypeRepository;
import org.alloy.repositories.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.alloy.models.GeneralStatus;
import org.alloy.models.entities.WeldingMachineParameterValue;
import org.alloy.models.weldingmachine.StateSummaryPropertyValue;

@Service
@Transactional
public class WeldingMachineStateService {

    @Autowired
    private WeldingMachineRepository weldingMachineRepository;

    @Autowired
    private WeldingMachineStateRepository weldingMachineStateRepository;

    @Autowired
    private OrganizationUnitRepository organizationUnitRepository;

    @Autowired
    private WeldingMachineTypeRepository weldingMachineTypeRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private WeldingMachineParameterValueService parameterValueService;

    // ===== НОВЫЕ МЕТОДЫ ДЛЯ ИНТЕГРАЦИИ СО СВАРОЧНЫМИ АППАРАТАМИ =====

     @Transactional
    public void saveMachineState(String mac, StateSummary stateSummary) {
        try {
            // Ищем сварочный аппарат по MAC
            Optional<WeldingMachine> machineOpt = weldingMachineRepository.findByMac(mac);
            WeldingMachine machine;
            
            if (!machineOpt.isPresent()) {
                // Создаем новый сварочный аппарат если не найден
               // System.out.println("[STATE-SERVICE] 🔧 Создание нового сварочного аппарата с MAC: " + mac);
                machine = new WeldingMachine();
                machine.setMac(mac);
                machine.setName("Сварочный аппарат " + mac);
                machine.setDescription("Автоматически созданный аппарат");
                machine.setStatus(GeneralStatus.Active);
                machine.setDateCreated(LocalDateTime.now());
                machine.setLastOnlineOn(LocalDateTime.now());
                
                // Ищем существующие записи для обязательных полей
                List<org.alloy.models.entities.OrganizationUnit> orgUnits = organizationUnitRepository.findAll();
                if (!orgUnits.isEmpty()) {
                    machine.setOrganizationUnitId(orgUnits.get(0).getId());
                   // System.out.println("[STATE-SERVICE] Используем организационную единицу: " + orgUnits.get(0).getName());
                } else {
                    // Создаем организационную единицу если её нет
                  //  System.out.println("[STATE-SERVICE] Создаем организационную единицу...");
                    
                    // Сначала создаем организацию
                    List<org.alloy.models.entities.Organization> organizations = organizationRepository.findAll();
                    org.alloy.models.entities.Organization organization;
                    if (!organizations.isEmpty()) {
                        organization = organizations.get(0);
                      //  System.out.println("[STATE-SERVICE] Используем существующую организацию: " + organization.getName());
                    } else {
                     //   System.out.println("[STATE-SERVICE] Создаем организацию...");
                        organization = new org.alloy.models.entities.Organization();
                        organization.setName("Основная организация");
                        organization.setDescription("Автоматически созданная организация");
                        organization.setStatus(GeneralStatus.Active);
                        organization.setDateCreated(LocalDateTime.now());
                        organization = organizationRepository.save(organization);
                      //  System.out.println("[STATE-SERVICE] ✅ Организация создана с ID: " + organization.getId());
                    }
                    
                    // Теперь создаем организационную единицу
                    org.alloy.models.entities.OrganizationUnit orgUnit = new org.alloy.models.entities.OrganizationUnit();
                    orgUnit.setName("Основная организационная единица");
                    orgUnit.setDescription("Автоматически созданная организационная единица");
                    orgUnit.setStatus(GeneralStatus.Active);
                    orgUnit.setDateCreated(LocalDateTime.now());
                    orgUnit.setOrganizationId(organization.getId());
                    orgUnit = organizationUnitRepository.save(orgUnit);
                    machine.setOrganizationUnitId(orgUnit.getId());
                  //  System.out.println("[STATE-SERVICE] ✅ Организационная единица создана с ID: " + orgUnit.getId());
                }
                
                List<org.alloy.models.entities.WeldingMachineType> machineTypes = weldingMachineTypeRepository.findAll();
                if (!machineTypes.isEmpty()) {
                    machine.setWeldingMachineTypeId(machineTypes.get(0).getId());
                  //  System.out.println("[STATE-SERVICE] Используем тип аппарата: " + machineTypes.get(0).getName());
                } else {
                    // Создаем тип сварочного аппарата если его нет
                   // System.out.println("[STATE-SERVICE] Создаем тип сварочного аппарата...");
                    org.alloy.models.entities.WeldingMachineType machineType = new org.alloy.models.entities.WeldingMachineType();
                    machineType.setName("MP-500");
                    machineType.setDescription("Автоматически созданный тип аппарата");
                    machineType.setStatus(GeneralStatus.Active);
                    machineType.setDateCreated(LocalDateTime.now());
                    machineType = weldingMachineTypeRepository.save(machineType);
                    machine.setWeldingMachineTypeId(machineType.getId());
                  //  System.out.println("[STATE-SERVICE] ✅ Тип аппарата создан с ID: " + machineType.getId());
                }
                
                // Сохраняем аппарат
                machine = weldingMachineRepository.save(machine);
               // System.out.println("[STATE-SERVICE] ✅ Сварочный аппарат создан с ID: " + machine.getId());
            } else {
                machine = machineOpt.get();
                // Обновляем время последнего подключения
                machine.setLastOnlineOn(LocalDateTime.now());
                weldingMachineRepository.save(machine);
            }
            
            // Создаем состояние
            WeldingMachineState state = new WeldingMachineState();
            state.setWeldingMachineId(machine.getId());
            state.setDateCreated(LocalDateTime.now());
            state.setDateUpdated(LocalDateTime.now());
            state.setWeldingMachineStatus(stateSummary.getStatus());
            state.setControl(stateSummary.getControl());
            state.setControlState(stateSummary.getControlState());
            state.setErrorCode(stateSummary.getErrorCode());
            state.setLimitsExceeded(stateSummary.isLimitsExceeded());
            state.setStateDurationMs(0L);
            
            // Сохраняем состояние
            weldingMachineStateRepository.save(state);
            
            // Сохраняем параметры (State.I, State.U и др.)
            if (stateSummary.getProperties() != null && !stateSummary.getProperties().isEmpty()) {
                for (Map.Entry<String, StateSummaryPropertyValue> entry : stateSummary.getProperties().entrySet()) {
                    try {
                        WeldingMachineParameterValue paramValue = new WeldingMachineParameterValue();
                        paramValue.setWeldingMachineStateId(state.getId());
                        paramValue.setPropertyCode(entry.getKey());
                        paramValue.setValue(entry.getValue().getValue());
                        paramValue.setPropertyType(entry.getValue().getPropertyType());
                        paramValue.setRawValue(entry.getValue().getRawValue());
                        paramValue.setLimitsExceeded(entry.getValue().isLimitsExceeded());
                        paramValue.setLimitMin(entry.getValue().getLimitMin());
                        paramValue.setLimitMax(entry.getValue().getLimitMax());
                        
                        parameterValueService.createParameterValue(paramValue);
                       // System.out.println("[STATE-SERVICE] ✅ Параметр " + entry.getKey() + " = " + entry.getValue().getValue() + " сохранен");
                    } catch (Exception paramError) {
                     //   System.err.println("[STATE-SERVICE] ⚠️ Ошибка сохранения параметра " + entry.getKey() + ": " + paramError.getMessage());
                    }
                }
            }
            
          //  System.out.println("[STATE-SERVICE] ✅ Состояние сохранено для аппарата " + mac);
            
        } catch (Exception e) {
          //  System.err.println("[STATE-SERVICE] ❌ Ошибка сохранения состояния: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public StateSummary getCurrentState(String mac) {
        try {
            Optional<WeldingMachine> machineOpt = weldingMachineRepository.findByMac(mac);
            if (!machineOpt.isPresent()) {
                return null;
            }

            WeldingMachine machine = machineOpt.get();
            Optional<WeldingMachineState> stateOpt = weldingMachineStateRepository
                    .findTopByWeldingMachineIdOrderByDateCreatedDesc(machine.getId());

            if (stateOpt.isPresent()) {
                WeldingMachineState state = stateOpt.get();
                return convertToStateSummary(state);
            }

        } catch (Exception e) {
           // System.err.println("[STATE-SERVICE] Ошибка получения состояния: " + e.getMessage());
        }

        return null;
    }

    private StateSummary convertToStateSummary(WeldingMachineState state) {
        StateSummary summary = new StateSummary();
        summary.setWeldingMachineStateId(state.getId());
        summary.setDateCreated(state.getDateCreated());
        summary.setLastDatetimeUpdate(state.getDateUpdated());
        summary.setStatus(state.getWeldingMachineStatus());
        summary.setStateDurationMs(state.getStateDurationMs());
        summary.setErrorCode(state.getErrorCode());
        summary.setWeldingMaterialId(state.getWeldingMaterialId());
        summary.setLimitsExceeded(state.getLimitsExceeded() != null ? state.getLimitsExceeded() : false);
        summary.setWeldingLimitProgramId(state.getWeldingLimitProgramId());
        summary.setWeldingLimitProgramName(state.getWeldingLimitProgramName());

        return summary;
    }

    // ===== СТАРЫЕ МЕТОДЫ ДЛЯ WeldingMachineStateController =====

    public List<WeldingMachineState> getAllWeldingMachineStates() {
        return weldingMachineStateRepository.findAll();
    }

    public Optional<WeldingMachineState> getWeldingMachineStateById(Long id) {
        return weldingMachineStateRepository.findById(id);
    }

    public List<WeldingMachineState> getWeldingMachineStatesByMachineId(Integer machineId) {
        return weldingMachineStateRepository.findByWeldingMachineId(machineId);
    }

    public Optional<WeldingMachineState> getLatestWeldingMachineState(Integer machineId) {
        return weldingMachineStateRepository.findTopByWeldingMachineIdOrderByDateCreatedDesc(machineId);
    }

    public List<WeldingMachineState> getWeldingMachineStatesByStatus(Integer machineId, WeldingMachineStatus status) {
        return weldingMachineStateRepository.findByWeldingMachineIdAndWeldingMachineStatus(machineId, status);
    }

    public WeldingMachineState createWeldingMachineState(WeldingMachineState state) {
        // Validate required fields
        if (state.getWeldingMachineId() == null) {
            throw new IllegalArgumentException("Welding machine ID is required");
        }
        if (state.getWeldingMachineStatus() == null) {
            throw new IllegalArgumentException("Status is required");
        }

        // Set creation date
        state.setDateCreated(LocalDateTime.now());

        return weldingMachineStateRepository.save(state);
    }

    public WeldingMachineState updateWeldingMachineState(WeldingMachineState state) {
        // Validate ID
        if (state.getId() == null) {
            throw new IllegalArgumentException("ID is required for update");
        }

        // Check if state exists
        WeldingMachineState existingState = weldingMachineStateRepository.findById(state.getId())
                .orElseThrow(() -> new IllegalArgumentException("Welding machine state not found"));

        // Preserve creation date
        state.setDateCreated(existingState.getDateCreated());

        return weldingMachineStateRepository.save(state);
    }

    public void deleteWeldingMachineState(Long id) {
        if (!weldingMachineStateRepository.existsById(id)) {
            throw new IllegalArgumentException("Welding machine state not found");
        }
        weldingMachineStateRepository.deleteById(id);
    }

    public void deleteAllWeldingMachineStates(Integer machineId) {
        List<WeldingMachineState> states = weldingMachineStateRepository.findByWeldingMachineId(machineId);
        weldingMachineStateRepository.deleteAll(states);
    }
}

package org.alloy.services;

import org.alloy.models.entities.WeldingMachineParameterValue;
import org.alloy.repositories.WeldingMachineParameterValueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class WeldingMachineParameterValueService {

    private final WeldingMachineParameterValueRepository parameterValueRepository;

    @Autowired
    public WeldingMachineParameterValueService(WeldingMachineParameterValueRepository parameterValueRepository) {
        this.parameterValueRepository = parameterValueRepository;
    }

    public List<WeldingMachineParameterValue> getAllParameterValues() {
        return parameterValueRepository.findAll();
    }

    public Optional<WeldingMachineParameterValue> getParameterValueById(Long id) {
        return parameterValueRepository.findById(id);
    }

    public List<WeldingMachineParameterValue> getParameterValuesByStateId(Long stateId) {
        return parameterValueRepository.findByWeldingMachineStateId(stateId);
    }

    public Optional<WeldingMachineParameterValue> getParameterValueByStateIdAndPropertyCode(Long stateId, String propertyCode) {
        return parameterValueRepository.findByWeldingMachineStateIdAndPropertyCode(stateId, propertyCode);
    }

    public List<WeldingMachineParameterValue> getExceededParameterValues(Long stateId) {
        return parameterValueRepository.findByWeldingMachineStateIdAndLimitsExceededTrue(stateId);
    }

    public WeldingMachineParameterValue createParameterValue(WeldingMachineParameterValue value) {
        // Validate required fields
        if (value.getWeldingMachineStateId() == null) {
            throw new IllegalArgumentException("Welding machine state ID is required");
        }
        if (value.getPropertyCode() == null || value.getPropertyCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Property code is required");
        }
        if (value.getValue() == null) {
            throw new IllegalArgumentException("Value is required");
        }

        // Check if parameter value already exists for this state and property
        if (parameterValueRepository.findByWeldingMachineStateIdAndPropertyCode(
                value.getWeldingMachineStateId(), value.getPropertyCode()).isPresent()) {
            throw new IllegalArgumentException("Parameter value already exists for this state and property");
        }

        // Check limits if provided
        if (value.getLimitMin() != null && value.getLimitMax() != null) {
            double numericValue = Double.parseDouble(value.getValue());
            value.setLimitsExceeded(numericValue < value.getLimitMin() || numericValue > value.getLimitMax());
        }

        return parameterValueRepository.save(value);
    }

    public WeldingMachineParameterValue updateParameterValue(WeldingMachineParameterValue value) {
        // Validate ID
        if (value.getId() == null) {
            throw new IllegalArgumentException("ID is required for update");
        }

        // Check if parameter value exists
        WeldingMachineParameterValue existingValue = parameterValueRepository.findById(value.getId())
                .orElseThrow(() -> new IllegalArgumentException("Parameter value not found"));

        // Check if new property code conflicts with existing one for this state
        if (!existingValue.getPropertyCode().equals(value.getPropertyCode())) {
            if (parameterValueRepository.findByWeldingMachineStateIdAndPropertyCode(
                    value.getWeldingMachineStateId(), value.getPropertyCode()).isPresent()) {
                throw new IllegalArgumentException("Parameter value already exists for this state and property");
            }
        }

        // Check limits if provided
        if (value.getLimitMin() != null && value.getLimitMax() != null) {
            double numericValue = Double.parseDouble(value.getValue());
            value.setLimitsExceeded(numericValue < value.getLimitMin() || numericValue > value.getLimitMax());
        }

        return parameterValueRepository.save(value);
    }

    public void deleteParameterValue(Long id) {
        if (!parameterValueRepository.existsById(id)) {
            throw new IllegalArgumentException("Parameter value not found");
        }
        parameterValueRepository.deleteById(id);
    }

    public void deleteAllParameterValues(Long stateId) {
        List<WeldingMachineParameterValue> values = parameterValueRepository.findByWeldingMachineStateId(stateId);
        parameterValueRepository.deleteAll(values);
    }
}

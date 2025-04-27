package org.alloy.services;

import org.alloy.models.configuration.PropertyCodes;
import org.alloy.models.configuration.WeldingMachineTypeConfiguration;
import org.alloy.models.configuration.WeldingMachineTypeConfiguration.MessageProperty;
import org.alloy.models.configuration.WeldingMachineTypeConfiguration.PropertyLimit;
import org.alloy.models.machine.ProgramControls;
import org.alloy.models.machine.ProgramControls.ProgramControlItem;
import org.alloy.models.machine.ProgramControls.ProgramControlItemType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgramControlsBuilder {
    private final String[] sortedImportantParameters = {
            PropertyCodes.StateCtrl,
            PropertyCodes.CtrlParm,
            PropertyCodes.CtrlParmL,
            PropertyCodes.CtrlParmR
    };

    private final WeldingMachineTypeConfiguration configuration;

    public ProgramControls buildDefault() {
        ProgramControls controls = new ProgramControls();
        controls.setName("");
        controls.setItems(new ArrayList<>());
        controls.setWeldingMaterialId(0);
        controls.setGasWeldingMaterialId(0);

        // First, process sorted important parameters
        Arrays.stream(sortedImportantParameters)
                .forEach(propertyCode -> {
                    List<ProgramControlItem> items = buildProgramControlItems(propertyCode);
                    if (items != null) {
                        controls.getItems().addAll(items);
                    }
                });

        // Then process all other controlled parameters
        Arrays.stream(configuration.getOutbound().getBody())
                .filter(p -> p.getPropertyCode() != null && !p.getPropertyCode().isEmpty())
                .filter(p -> !Arrays.asList(sortedImportantParameters).contains(p.getPropertyCode()))
                .forEach(p -> {
                    List<ProgramControlItem> items = buildProgramControlItems(p.getPropertyCode());
                    if (items != null) {
                        controls.getItems().addAll(items);
                    }
                });

        return controls;
    }

    private List<ProgramControlItem> buildProgramControlItems(String propertyCode) {
        if (PropertyCodes.CRC8.equals(propertyCode)) {
            return null;
        }

        MessageProperty property = Arrays.stream(configuration.getOutbound().getBody())
                .filter(p -> p.getPropertyCode().equals(propertyCode))
                .findFirst()
                .orElse(null);

        if (property == null) {
            return null;
        }

        List<ProgramControlItem> result = new ArrayList<>();

        // Handle special cases first
        switch (propertyCode) {
            case PropertyCodes.CtrlParm:
            case PropertyCodes.CtrlParmL:
            case PropertyCodes.CtrlParmR:
                handleControlParameters(property, result);
                break;
            default:
                handleDefaultParameters(property, result);
                break;
        }

        return result;
    }

    private void handleControlParameters(MessageProperty property, List<ProgramControlItem> result) {
        // Create dropdown for the Control
        ProgramControlItem item = new ProgramControlItem();
        item.setId(property.getPropertyCode());
        item.setLabel(property.getDescription());
        item.setType(ProgramControlItemType.OPTION);
        item.setOptions(new HashMap<>());

        // Add options for selection
        Arrays.stream(property.getEnums()).forEach(e -> {
            String key = e.getValue();
            String value = e.getDescription();

            // Lookup for property description
            value = Arrays.stream(configuration.getInbound().getBody())
                    .filter(p -> p.getPropertyCode().equals(value))
                    .findFirst()
                    .map(MessageProperty::getDescription)
                    .orElseGet(() -> Arrays.stream(configuration.getPropertyLimits().getLimits())
                            .filter(l -> l.getPropertyCode().equals(value))
                            .findFirst()
                            .map(PropertyLimit::getDescription)
                            .orElse(value));

            if (key != null && !key.isEmpty() && !item.getOptions().containsKey(key)) {
                item.getOptions().put(key, value);
            }
        });

        result.add(item);

        // Add dependent parameters
        Arrays.stream(property.getEnums()).forEach(e -> {
            String propCode = e.getDescription();
            PropertyLimit limit = Arrays.stream(configuration.getPropertyLimits().getLimits())
                    .filter(l -> l.getPropertyCode().equals(propCode))
                    .findFirst()
                    .orElse(null);

            if (limit != null) {
                ProgramControlItem subItem = new ProgramControlItem();
                subItem.setId(propCode);
                subItem.setLabel(limit.getDescription());
                subItem.setType(ProgramControlItemType.NUMERIC_RANGE);
                subItem.setRangeMinValue(limit.getMinValue());
                subItem.setRangeMaxValue(limit.getMaxValue());
                subItem.setVisibilityItemId(property.getPropertyCode());
                subItem.setVisibilityItemValue(e.getValue());
                subItem.setStep(limit.getStep() > 0 ? limit.getStep() : 1);

                result.add(subItem);
            }
        });
    }

    private void handleDefaultParameters(MessageProperty property, List<ProgramControlItem> result) {
        switch (property.getPropertyType()) {
            case "number":
                handleNumberType(property, result);
                break;
            case "enum":
                handleEnumType(property, result);
                break;
            case "flag":
                handleFlagType(property, result);
                break;
            case "range_min":
                handleRangeMinType(property, result);
                break;
        }
    }

    private void handleNumberType(MessageProperty property, List<ProgramControlItem> result) {
        PropertyLimit limit = Arrays.stream(configuration.getPropertyLimits().getLimits())
                .filter(l -> l.getPropertyCode().equals(property.getPropertyCode()))
                .findFirst()
                .orElse(null);

        if (limit != null) {
            ProgramControlItem item = new ProgramControlItem();
            item.setId(property.getPropertyCode());
            item.setLabel(limit.getDescription().isEmpty() ? property.getDescription() : limit.getDescription());
            item.setType(ProgramControlItemType.NUMBER);
            item.setRangeMinValue(limit.getMinValue());
            item.setRangeMaxValue(limit.getMaxValue());
            item.setStep(limit.getStep() > 0 ? limit.getStep() : 1);

            result.add(item);
        }
    }

    private void handleEnumType(MessageProperty property, List<ProgramControlItem> result) {
        ProgramControlItem item = new ProgramControlItem();
        item.setId(property.getPropertyCode());
        item.setLabel(property.getDescription());
        item.setType(ProgramControlItemType.OPTION);
        item.setOptions(Arrays.stream(property.getEnums())
                .collect(Collectors.toMap(
                        e -> e.getValue(),
                        e -> e.getDescription()
                )));

        result.add(item);
    }

    private void handleFlagType(MessageProperty property, List<ProgramControlItem> result) {
        ProgramControlItem item = new ProgramControlItem();
        item.setId(property.getPropertyCode());
        item.setLabel(property.getDescription());
        item.setType(ProgramControlItemType.MULTIPLE_OPTION);
        item.setOptions(Arrays.stream(property.getEnums())
                .collect(Collectors.toMap(
                        e -> e.getValue(),
                        e -> e.getDescription()
                )));

        result.add(item);
    }

    private void handleRangeMinType(MessageProperty property, List<ProgramControlItem> result) {
        String rangePropertyCode = property.getRangeSource();
        if (rangePropertyCode == null || rangePropertyCode.isEmpty()) {
            return;
        }

        PropertyLimit limit = Arrays.stream(configuration.getPropertyLimits().getLimits())
                .filter(l -> l.getPropertyCode().equals(rangePropertyCode))
                .findFirst()
                .orElse(null);

        if (limit != null) {
            ProgramControlItem subItem = new ProgramControlItem();
            subItem.setId(rangePropertyCode);
            subItem.setLabel(limit.getDescription());
            subItem.setType(ProgramControlItemType.NUMERIC_RANGE);
            subItem.setRangeMinValue(limit.getMinValue());
            subItem.setRangeMaxValue(limit.getMaxValue());
            subItem.setStep(limit.getStep() > 0 ? limit.getStep() : 1);

            result.add(subItem);
        }
    }
}

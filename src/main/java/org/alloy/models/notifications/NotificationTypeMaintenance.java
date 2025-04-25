package org.alloy.models.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NotificationTypeMaintenance extends NotificationTypeBase {
    private NotificationMaintenanceParameters parameters;

    public NotificationTypeMaintenance(int machineId, String machineName, String mac, String labels,
                                       int hoursBeforeService, int daysSinceLastService) {
        this.parameters = new NotificationMaintenanceParameters();
        this.parameters.setWeldingMachineId(machineId);
        this.parameters.setWeldingMachineName(machineName);
        this.parameters.setWeldingMachineLabels(labels);
        this.parameters.setHoursBeforeService(hoursBeforeService);
        this.parameters.setDaysSinceLastService(daysSinceLastService);
    }

    @Override
    public String getType() {
        return "NotificationMaintenance";
    }

    @Override
    public String generateKey() {
        return parameters == null ? null :
                String.format("%d_%d", parameters.getWeldingMachineId(), parameters.getHoursBeforeService());
    }

    @Override
    public String generateJson() {
        if (parameters == null) {
            return "";
        }
        try {
            return new ObjectMapper().writeValueAsString(parameters);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public String buildContent() {
        if (parameters == null) {
            return null;
        }
        return String.format("Machine %s (ID: %d) requires maintenance. Hours before service: %d, Days since last service: %d",
                parameters.getWeldingMachineName(),
                parameters.getWeldingMachineId(),
                parameters.getHoursBeforeService(),
                parameters.getDaysSinceLastService());
    }

    @Data
    @NoArgsConstructor
    public static class NotificationMaintenanceParameters {
        private int weldingMachineId;
        private String weldingMachineName;
        private String weldingMachineLabels;
        private int hoursBeforeService;
        private int daysSinceLastService;
    }
}

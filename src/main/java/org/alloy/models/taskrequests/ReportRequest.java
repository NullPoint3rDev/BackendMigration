package org.alloy.models.taskrequests;

import com.welding.models.utils.Repeating;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ReportRequest {
    private LocalDateTime dateCreated;
    private String reportType;
    private String reportName;
    private String lang;
    private LocalDateTime date;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private String timeFrom;
    private String timeTo;
    private Integer userAccountId;
    private List<Integer> workerUserAccountIds = new ArrayList<>();
    private Integer organizationUnitId;
    private Integer weldingMachineId;
    private Integer weldingMachineTypeId;
    private List<Integer> weldingMachineIds = new ArrayList<>();
    private List<Integer> organizationUnitIds = new ArrayList<>();
    private String splitBy;
    private List<String> propertyCodes = new ArrayList<>();

    // Schedule options
    private Integer editScheduleTaskId;
    private boolean schedule;
    private String scheduledTime;
    private String scheduledRecipientEmails;
    private String scheduledTaskDescription;
    private Repeating repeating;
    private Integer scheduleTaskId;
}

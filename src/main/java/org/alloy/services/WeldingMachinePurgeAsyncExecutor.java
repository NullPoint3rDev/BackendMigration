package org.alloy.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Фоновая очистка данных аппарата по machine id (вне HTTP-потока).
 */
@Component
public class WeldingMachinePurgeAsyncExecutor {

    private static final Logger log = LoggerFactory.getLogger(WeldingMachinePurgeAsyncExecutor.class);

    @Autowired
    @Lazy
    private WeldingMachineService weldingMachineService;

    @Async
    public void purgeAsync(Integer machineId) {
        if (machineId == null) {
            return;
        }
        try {
            weldingMachineService.purgeWeldingMachineData(machineId);
        } catch (Exception e) {
            log.error("purgeAsync failed for machineId={}", machineId, e);
        }
    }
}

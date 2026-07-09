package org.alloy.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Планировщик отложенной очистки аппаратов в статусе Purging.
 * Раз в N минут подхватывает записи старше retention и чистит данные по id.
 */
@Component
public class WeldingMachinePurgeScheduler {

    private static final Logger log = LoggerFactory.getLogger(WeldingMachinePurgeScheduler.class);

    @Autowired
    private WeldingMachineService weldingMachineService;

    @Scheduled(fixedDelayString = "${welding.machine.purge.scheduler-delay-ms:3600000}")
    public void purgeDueMachines() {
        try {
            int n = weldingMachineService.purgeDueWeldingMachines();
            if (n > 0) {
                log.info("purgeDueMachines: scheduled {} machine(s)", n);
            }
        } catch (Exception e) {
            log.error("purgeDueMachines failed", e);
        }
    }
}

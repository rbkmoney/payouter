package com.rbkmoney.payouter.handler;

import com.rbkmoney.damsel.claim_management.*;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.payouter.exception.InvalidChangesetException;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.service.DominantService;
import com.rbkmoney.payouter.service.SchedulerService;
import com.rbkmoney.payouter.util.SchedulerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PartyModificationCommitHandler implements CommitHandler<ScheduleModification> {

    private final SchedulerService schedulerService;

    private final DominantService dominantService;

    @Override
    public void accept(String partyId, String shopId, ScheduleModification scheduleModification) {
        log.info("Trying to accept payout schedule modification, partyId='{}', scheduleModification='{}'",
                partyId, scheduleModification);
        if (scheduleModification.isSetSchedule()) {
            BusinessScheduleRef schedule = scheduleModification.getSchedule();
            checkSchedule(schedule);
        }
        log.info("Payout schedule modification have been accepted, partyId='{}', scheduleModification='{}'",
                partyId, scheduleModification);
    }

    @Override
    public void commit(String partyId, String shopId, ScheduleModification scheduleModification) {
        log.info("Trying to commit schedule modification, partyId='{}', scheduleModification='{}'",
                partyId, scheduleModification);
        if (scheduleModification.isSetSchedule()) {
            schedulerService.registerJob(partyId, shopId, scheduleModification.getSchedule());
        } else {
            schedulerService.deregisterJob(partyId, shopId);
        }
        log.info("Schedule modification have been committed, partyId='{}', scheduleModification='{}'",
                partyId, scheduleModification);
    }

    private void checkSchedule(BusinessScheduleRef schedule) {
        if (schedule != null) {
            try {
                BusinessSchedule businessSchedule = dominantService.getBusinessSchedule(schedule);
                SchedulerUtil.buildCron(businessSchedule.getSchedule());
            } catch (IllegalArgumentException | NotFoundException ex) {
                log.warn("Invalid business schedule", ex);
                throw new InvalidChangesetException("Invalid business schedule", ex);
            }
        }
    }

}

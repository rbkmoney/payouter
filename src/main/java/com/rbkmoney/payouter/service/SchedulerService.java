package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.domain.ScheduleRef;

public interface SchedulerService {

    void registerJob(String partyId, String contractId, String payoutToolId, ScheduleRef scheduleRef);

    void deregisterJob(String partyId, String contractId, String payoutToolId);

}

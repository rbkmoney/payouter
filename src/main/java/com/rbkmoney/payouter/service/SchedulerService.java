package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.domain.ScheduleRef;

public interface SchedulerService {

    void registerJob(String partyId, String shopId, ScheduleRef scheduleRef);

    void deregisterJob(String partyId, String shopId);

}

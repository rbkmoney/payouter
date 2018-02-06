package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.domain.ScheduleRef;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.exception.ScheduleProcessingException;
import com.rbkmoney.payouter.exception.StorageException;

public interface SchedulerService {

    void registerJob(String partyId, String shopId, ScheduleRef scheduleRef) throws NotFoundException, ScheduleProcessingException, StorageException;

    void deregisterJob(String partyId, String shopId) throws NotFoundException, ScheduleProcessingException, StorageException;

}

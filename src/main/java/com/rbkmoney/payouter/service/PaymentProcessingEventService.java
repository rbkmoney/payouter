package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.exception.StorageException;

public interface PaymentProcessingEventService {

    void processEvent(MachineEvent machineEvent, EventPayload eventPayload) throws StorageException, NotFoundException;

}

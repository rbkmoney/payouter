package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.payment_processing.PartyEventData;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.exception.StorageException;

public interface PartyManagementEventService {

    void processPayloadEvent(MachineEvent machineEvent, PartyEventData eventPayload)
            throws StorageException, NotFoundException;

}

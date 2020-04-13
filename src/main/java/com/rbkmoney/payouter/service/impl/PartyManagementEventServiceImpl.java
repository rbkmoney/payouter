package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.damsel.payment_processing.PartyChange;
import com.rbkmoney.damsel.payment_processing.PartyEventData;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.payouter.exception.DaoException;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.exception.StorageException;
import com.rbkmoney.payouter.poller.handler.PartyManagementHandler;
import com.rbkmoney.payouter.service.PartyManagementEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartyManagementEventServiceImpl implements PartyManagementEventService {

    private final List<PartyManagementHandler> handlers;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processPayloadEvent(MachineEvent event, PartyEventData eventPayload)
            throws StorageException, NotFoundException {
        long eventId = event.getEventId();
        String createdAt = event.getCreatedAt();
        log.debug("Trying to save eventId, eventId={}, eventCreatedAt={}", eventId, createdAt);
        if (eventPayload.isSetChanges()) {
            for (PartyChange change : eventPayload.getChanges()) {
                PartyManagementHandler handler = getHandler(change);
                if (handler != null) {
                    log.debug("Trying to handle change, change='{}', event='{}'", change, event);
                    try {
                        handler.handle(change, event);
                        log.info("Change have been handled, eventId='{}', change='{}'", eventId, change);
                    } catch (DaoException ex) {
                        throw new StorageException(String.format("Failed to save event, eventId='%d', change='%s'",
                                eventId, change), ex);
                    }
                }
            }
            log.info("Event id have been saved, eventId={}, eventCreatedAt={}", eventId, createdAt);
        }
    }

    private PartyManagementHandler getHandler(PartyChange change) {
        for (PartyManagementHandler handler : handlers) {
            if (handler.accept(change)) {
                return handler;
            }
        }
        return null;
    }

}

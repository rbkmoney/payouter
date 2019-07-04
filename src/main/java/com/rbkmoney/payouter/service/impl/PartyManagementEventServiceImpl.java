package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.damsel.event_stock.SourceEvent;
import com.rbkmoney.damsel.event_stock.StockEvent;
import com.rbkmoney.damsel.payment_processing.Event;
import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.damsel.payment_processing.PartyChange;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.payouter.dao.EventStockMetaDao;
import com.rbkmoney.payouter.domain.tables.pojos.EventStockMeta;
import com.rbkmoney.payouter.exception.DaoException;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.exception.StorageException;
import com.rbkmoney.payouter.poller.handler.Handler;
import com.rbkmoney.payouter.poller.handler.PartyManagementHandler;
import com.rbkmoney.payouter.service.PartyManagementEventService;
import com.rbkmoney.payouter.service.PaymentProcessingEventService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PartyManagementEventServiceImpl implements PartyManagementEventService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final EventStockMetaDao eventStockMetaDao;

    private final List<PartyManagementHandler> handlers;

    private final PaymentProcessingEventService paymentProcessingEventService;

    @Override
    public Optional<EventStockMeta> getLastEventId() throws StorageException {
        try {
            return Optional.ofNullable(eventStockMetaDao.getLastEventMeta());
        } catch (DaoException ex) {
            throw new StorageException("Failed to get last event id", ex);
        }
    }

    @Override
    public void setLastEventId(long eventId, LocalDateTime eventCreatedAt) throws StorageException {
        try {
            eventStockMetaDao.setLastEventMeta(eventId, eventCreatedAt);
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to change last event id, eventId='%d', eventCreatedAt='%s'", eventId, eventCreatedAt), ex);
        }
    }


    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processStockEvent(StockEvent stockEvent) throws StorageException, NotFoundException {
        SourceEvent sourceEvent = stockEvent.getSourceEvent();
        if (sourceEvent.isSetProcessingEvent()) {
            Event event = sourceEvent.getProcessingEvent();
            log.debug("Trying to save eventId, eventId={}, eventCreatedAt={}", event.getId(), event.getCreatedAt());
            setLastEventId(event.getId(), TypeUtil.stringToLocalDateTime(event.getCreatedAt()));

            EventPayload payload = event.getPayload();
            if(payload.isSetPartyChanges()) {
                for (PartyChange change : payload.getPartyChanges()) {
                    Handler handler = getHandler(change);
                    if (handler != null) {
                        log.debug("Trying to handle change, change='{}', event='{}'", change, event);
                        try {
                            handler.handle(change, event);
                            log.info("Change have been handled, eventId='{}', change='{}'", event.getId(), change);
                        } catch (DaoException ex) {
                            throw new StorageException(String.format("Failed to save event, eventId='%d', change='%s'", event.getId(), change), ex);
                        }
                    }
                }
            } else if (payload.isSetInvoiceChanges()) {
                MachineEvent machineEvent = new MachineEvent()
                        .setCreatedAt(event.getCreatedAt())
                        .setSourceId(event.getSource().getInvoiceId())
                        .setEventId(event.getSequence());
                paymentProcessingEventService.processEvent(machineEvent, payload);
            }
            log.info("Event id have been saved, eventId={}, eventCreatedAt={}", event.getId(), event.getCreatedAt());
        }
    }

    private <T> Handler getHandler(T change) {
        for (Handler handler : handlers) {
            if (handler.accept(change)) {
                return handler;
            }
        }
        return null;
    }

}

package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.damsel.event_stock.SourceEvent;
import com.rbkmoney.damsel.event_stock.StockEvent;
import com.rbkmoney.damsel.payment_processing.Event;
import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.damsel.payment_processing.PartyChange;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.payouter.dao.EventStockMetaDao;
import com.rbkmoney.payouter.domain.tables.pojos.EventStockMeta;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartyManagementEventServiceImpl implements PartyManagementEventService {

    private final EventStockMetaDao eventStockMetaDao;

    private final List<PartyManagementHandler> handlers;

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
            if (payload.isSetPartyChanges()) {
                for (PartyChange change : payload.getPartyChanges()) {
                    PartyManagementHandler handler = getHandler(change);
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
            }
            log.info("Event id have been saved, eventId={}, eventCreatedAt={}", event.getId(), event.getCreatedAt());
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

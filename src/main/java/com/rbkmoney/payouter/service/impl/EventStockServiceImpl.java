package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.damsel.event_stock.SourceEvent;
import com.rbkmoney.damsel.event_stock.StockEvent;
import com.rbkmoney.damsel.payment_processing.Event;
import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.payouter.dao.EventStockMetaDao;
import com.rbkmoney.payouter.domain.tables.pojos.EventStockMeta;
import com.rbkmoney.payouter.exception.DaoException;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.exception.StorageException;
import com.rbkmoney.payouter.poller.handler.Handler;
import com.rbkmoney.payouter.service.EventStockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EventStockServiceImpl implements EventStockService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final EventStockMetaDao eventStockMetaDao;

    private final List<Handler> handlers;

    @Autowired
    public EventStockServiceImpl(EventStockMetaDao eventStockMetaDao, List<Handler> handlers) {
        this.eventStockMetaDao = eventStockMetaDao;
        this.handlers = handlers;
    }

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
            throw new StorageException(String.format("Failed to change last event id, eventId=%d, eventCreatedAt='%s'", eventId, eventCreatedAt));
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
            processChanges((List) payload.getFieldValue(), event);
            log.info("Event id have been saved, eventId={}, eventCreatedAt={}", event.getId(), event.getCreatedAt());
        }
    }

    private <T> void processChanges(List<T> changes, Event event) {
        for (T change : changes) {
            Handler handler = getHandler(change);
            if (handler != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Trying to handle change, change='{}', event='{}'", change, event);
                }
                try {
                    handler.handle(change, event);
                    log.info("Change have been handled, eventId='{}', change='{}'", event.getId(), change);
                } catch (DaoException ex) {
                    throw new StorageException(String.format("Failed to save event, eventId='%d', change='%s'", event.getId(), change), ex);
                }
            }
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

package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.damsel.event_stock.SourceEvent;
import com.rbkmoney.damsel.event_stock.StockEvent;
import com.rbkmoney.damsel.payment_processing.Event;
import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
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
            if (payload.isSetInvoiceChanges()) {
                for (InvoiceChange invoiceChange : payload.getInvoiceChanges()) {
                    Handler handler = getHandler(invoiceChange);
                    if (handler != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("Trying to handle invoice change, invoiceChange={}, eventId={}", invoiceChange, event.getId());
                        }
                        try {
                            handler.handle(invoiceChange, stockEvent);
                            log.info("Invoice change have been handled, eventId={}", event.getId());
                        } catch (DaoException ex) {
                            throw new StorageException(String.format("Failed to save event, eventId=%d", event.getId()), ex);
                        }
                    }
                }
            }
            log.info("Event id have been saved, eventId={}, eventCreatedAt={}", event.getId(), event.getCreatedAt());
        }
    }

    private Handler getHandler(InvoiceChange invoiceChange) {
        for (Handler handler : handlers) {
            if (handler.accept(invoiceChange)) {
                return handler;
            }
        }
        return null;
    }

}

package com.rbkmoney.payouter.poller;

import com.rbkmoney.damsel.event_stock.StockEvent;
import com.rbkmoney.eventstock.client.EventAction;
import com.rbkmoney.eventstock.client.EventHandler;
import com.rbkmoney.payouter.exception.DaoException;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.exception.StorageException;
import com.rbkmoney.payouter.service.EventStockService;
import com.rbkmoney.woody.api.flow.error.WRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedRuntimeException;
import org.springframework.stereotype.Component;

@Component
public class EventStockHandler implements EventHandler<StockEvent> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final EventStockService eventStockService;

    @Autowired
    public EventStockHandler(EventStockService eventStockService) {
        this.eventStockService = eventStockService;
    }

    @Override
    public EventAction handle(StockEvent stockEvent, String subsKey) {
        try {
            eventStockService.processStockEvent(stockEvent);
            return EventAction.CONTINUE;
        } catch (Exception ex) {
            log.warn("Failed to handle event, retry", ex);
            return EventAction.DELAYED_RETRY;
        }
    }

}

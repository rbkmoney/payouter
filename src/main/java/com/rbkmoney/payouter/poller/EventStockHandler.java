package com.rbkmoney.payouter.poller;

import com.rbkmoney.damsel.event_stock.StockEvent;
import com.rbkmoney.eventstock.client.EventAction;
import com.rbkmoney.eventstock.client.EventHandler;
import com.rbkmoney.payouter.exception.DaoException;
import com.rbkmoney.payouter.service.EventStockService;
import com.rbkmoney.woody.api.flow.error.WRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
        } catch (DaoException | WRuntimeException ex) {
            return EventAction.DELAYED_RETRY;
        } catch(Exception ex) {
            return EventAction.INTERRUPT;
        }
    }

}

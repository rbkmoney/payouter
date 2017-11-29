package com.rbkmoney.payouter.poller;

import com.rbkmoney.damsel.event_stock.StockEvent;
import com.rbkmoney.eventstock.client.EventAction;
import com.rbkmoney.eventstock.client.EventHandler;
import com.rbkmoney.payouter.service.EventStockService;
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
        eventStockService.processStockEvent(stockEvent);
        return EventAction.CONTINUE;
    }

}

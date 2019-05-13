package com.rbkmoney.payouter.poller;

import com.rbkmoney.damsel.event_stock.StockEvent;
import com.rbkmoney.eventstock.client.EventAction;
import com.rbkmoney.eventstock.client.EventHandler;
import com.rbkmoney.payouter.service.PartyManagementEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventStockHandler implements EventHandler<StockEvent> {

    private final PartyManagementEventService eventStockService;

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

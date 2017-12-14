package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.event_stock.StockEvent;

public interface EventStockService {

//    Long getLastEventId() throws StorageException;

    void processStockEvent(StockEvent stockEvent);

}

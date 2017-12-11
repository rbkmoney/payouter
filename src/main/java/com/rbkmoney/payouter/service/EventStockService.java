package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.event_stock.StockEvent;
import com.rbkmoney.payouter.domain.tables.pojos.EventStockMeta;
import com.rbkmoney.payouter.exception.StorageException;

import java.util.Optional;

public interface EventStockService {

    Optional<EventStockMeta> getLastEventId() throws StorageException;

    void processStockEvent(StockEvent stockEvent);

}

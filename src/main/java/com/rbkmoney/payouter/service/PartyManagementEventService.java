package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.event_stock.StockEvent;
import com.rbkmoney.payouter.domain.tables.pojos.EventStockMeta;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.exception.StorageException;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PartyManagementEventService {

    Optional<EventStockMeta> getLastEventId() throws StorageException;

    void setLastEventId(long eventId, LocalDateTime eventCreatedAt) throws StorageException;

    void processStockEvent(StockEvent stockEvent) throws StorageException, NotFoundException;

}

package com.rbkmoney.payouter.service;

import com.rbkmoney.payouter.domain.tables.pojos.PayoutEvent;
import com.rbkmoney.payouter.exception.StorageException;

import java.util.List;
import java.util.Optional;

public interface EventSinkService {

    Long getLastEventId() throws StorageException;

    PayoutEvent getEvent(long eventId) throws StorageException;

    List<PayoutEvent> getEvents(Optional<Long> after, int limit) throws StorageException;

    void saveEvent(PayoutEvent payoutEvent) throws StorageException;

}

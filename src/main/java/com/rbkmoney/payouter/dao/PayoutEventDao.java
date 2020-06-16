package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.domain.tables.pojos.PayoutEvent;
import com.rbkmoney.payouter.exception.DaoException;

import java.util.List;
import java.util.Optional;

public interface PayoutEventDao extends GenericDao {

    Long getLastEventId() throws DaoException;

    PayoutEvent getEvent(long eventId) throws DaoException;

    List<PayoutEvent> getEvents(String payoutId, Optional<Long> after, int limit) throws DaoException;

    List<PayoutEvent> getEvents(Optional<Long> after, int limit) throws DaoException;

    long saveEvent(PayoutEvent payoutEvent) throws DaoException;

}

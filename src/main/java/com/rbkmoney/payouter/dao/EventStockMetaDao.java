package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.domain.tables.pojos.EventStockMeta;
import com.rbkmoney.payouter.exception.DaoException;

import java.time.LocalDateTime;

public interface EventStockMetaDao extends GenericDao {

    EventStockMeta getLastEventMeta() throws DaoException;

    void setLastEventMeta(long eventId, LocalDateTime eventCreatedAt) throws DaoException;

}

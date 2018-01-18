package com.rbkmoney.payouter.dao.impl;

import com.rbkmoney.payouter.dao.EventStockMetaDao;
import com.rbkmoney.payouter.dao.mapper.RecordRowMapper;
import com.rbkmoney.payouter.domain.tables.pojos.EventStockMeta;
import com.rbkmoney.payouter.exception.DaoException;
import org.jooq.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;

import static com.rbkmoney.payouter.domain.Tables.EVENT_STOCK_META;

@Component
public class EventStockMetaDaoImpl extends AbstractGenericDao implements EventStockMetaDao {

    private final RowMapper<EventStockMeta> eventStockMetaRowMapper;

    @Autowired
    public EventStockMetaDaoImpl(DataSource dataSource) {
        super(dataSource);
        eventStockMetaRowMapper = new RecordRowMapper<>(EVENT_STOCK_META, EventStockMeta.class);
    }

    @Override
    public EventStockMeta getLastEventMeta() throws DaoException {
        Query query = getDslContext()
                .selectFrom(EVENT_STOCK_META);
        return fetchOne(query, eventStockMetaRowMapper);
    }

    @Override
    public void setLastEventMeta(long eventId, LocalDateTime eventCreatedAt) throws DaoException {
        Query query = getDslContext().update(EVENT_STOCK_META)
                    .set(EVENT_STOCK_META.LAST_EVENT_ID, eventId)
                    .set(EVENT_STOCK_META.LAST_EVENT_CREATED_AT, eventCreatedAt);
        executeOne(query);
    }
}

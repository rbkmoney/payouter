package com.rbkmoney.payouter.dao.impl;

import com.rbkmoney.payouter.dao.PayoutEventDao;
import com.rbkmoney.payouter.dao.mapper.RecordRowMapper;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutEvent;
import com.rbkmoney.payouter.exception.DaoException;
import org.jooq.Condition;
import org.jooq.Query;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.rbkmoney.payouter.domain.Tables.PAYOUT_EVENT;

@Component
public class PayoutEventDaoImpl extends AbstractGenericDao implements PayoutEventDao {

    private final RowMapper<PayoutEvent> rowMapper;

    @Autowired
    public PayoutEventDaoImpl(DataSource dataSource) {
        super(dataSource);
        rowMapper = new RecordRowMapper<>(PAYOUT_EVENT, PayoutEvent.class);
    }

    @Override
    public Long getLastEventId() throws DaoException {
        Query query = getDslContext().select(DSL.max(PAYOUT_EVENT.EVENT_ID)).from(PAYOUT_EVENT);
        return fetchOne(query, Long.class);
    }

    @Override
    public PayoutEvent getEvent(long eventId) throws DaoException {
        Query query = getDslContext().selectFrom(PAYOUT_EVENT)
                .where(PAYOUT_EVENT.EVENT_ID.eq(eventId));

        return fetchOne(query, rowMapper);
    }

    @Override
    public List<PayoutEvent> getEvents(String payoutId, int limit) throws DaoException {
        Query query = getDslContext().selectFrom(PAYOUT_EVENT)
                .where(PAYOUT_EVENT.PAYOUT_ID.eq(payoutId))
                .orderBy(PAYOUT_EVENT.EVENT_ID)
                .limit(limit);

        return fetch(query, rowMapper);
    }

    @Override
    public List<PayoutEvent> getEvents(Optional<Long> after, int limit) throws DaoException {
        List<Condition> conditions = new ArrayList<>();
        if (after.isPresent()) {
            conditions.add(PAYOUT_EVENT.EVENT_ID.gt(after.get()));
        }
        Query query = getDslContext().selectFrom(PAYOUT_EVENT).where(conditions)
                .orderBy(PAYOUT_EVENT.EVENT_ID)
                .limit(limit);

        return fetch(query, rowMapper);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public long saveEvent(PayoutEvent payoutEvent) throws DaoException {
        execute("LOCK TABLE sht.payout_event IN ACCESS EXCLUSIVE MODE");

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        Query query = getDslContext().insertInto(PAYOUT_EVENT)
                .set(getDslContext().newRecord(PAYOUT_EVENT, payoutEvent))
                .returning(PAYOUT_EVENT.EVENT_ID);

        executeOneWithReturn(query, keyHolder);
        return keyHolder.getKey().longValue();
    }

}

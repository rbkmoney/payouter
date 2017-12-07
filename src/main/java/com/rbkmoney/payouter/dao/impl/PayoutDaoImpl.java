package com.rbkmoney.payouter.dao.impl;

import com.rbkmoney.payouter.dao.PayoutDao;
import com.rbkmoney.payouter.dao.mapper.RecordRowMapper;
import com.rbkmoney.payouter.domain.enums.PayoutStatus;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.exception.DaoException;
import org.jooq.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.List;

import static com.rbkmoney.payouter.domain.Tables.PAYOUT;

@Component
public class PayoutDaoImpl extends AbstractGenericDao implements PayoutDao {

    private final RowMapper<Payout> payoutRowMapper;

    @Autowired
    public PayoutDaoImpl(DataSource dataSource) {
        super(dataSource);
        payoutRowMapper = new RecordRowMapper<>(PAYOUT, Payout.class);
    }

    @Override
    public Payout get(long payoutId) throws DaoException {
        Query query = getDslContext().selectFrom(PAYOUT)
                .where(PAYOUT.ID.eq(payoutId));

        return fetchOne(query, payoutRowMapper);
    }

    @Override
    public List<Payout> get(Collection<Long> payoutIds) throws DaoException {
        Query query = getDslContext().selectFrom(PAYOUT)
                .where(PAYOUT.ID.in(payoutIds));

        return fetch(query, payoutRowMapper);
    }

    @Override
    public long save(Payout payout) throws DaoException {
        Query query = getDslContext().insertInto(PAYOUT)
                .set(getDslContext().newRecord(PAYOUT, payout))
                .returning(PAYOUT.ID);

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        executeOneWithReturn(query, keyHolder);

        return keyHolder.getKey().longValue();
    }

    @Override
    public void changeStatus(long payoutId, PayoutStatus payoutStatus) throws DaoException {
        Query query = getDslContext().update(PAYOUT)
                .set(PAYOUT.STATUS, payoutStatus)
                .where(PAYOUT.ID.eq(payoutId));

        executeOne(query);
    }

    @Override
    public List<Payout> getUnpaidPayouts() throws DaoException {
        Query query = getDslContext().selectFrom(PAYOUT)
                .where(PAYOUT.STATUS.eq(PayoutStatus.UNPAID))
                .forUpdate();

        return fetch(query, payoutRowMapper);
    }
}

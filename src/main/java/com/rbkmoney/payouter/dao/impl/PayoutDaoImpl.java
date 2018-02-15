package com.rbkmoney.payouter.dao.impl;

import com.rbkmoney.damsel.payout_processing.ShopParams;
import com.rbkmoney.payouter.dao.PayoutDao;
import com.rbkmoney.payouter.dao.mapper.RecordRowMapper;
import com.rbkmoney.payouter.domain.enums.PayoutStatus;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.exception.DaoException;
import org.jooq.Query;
import org.jooq.SelectQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.rbkmoney.payouter.domain.Tables.PAYMENT;
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
    public Payout getExclusive(long payoutId) throws DaoException {
        Query query = getDslContext().selectFrom(PAYOUT)
                .where(PAYOUT.ID.eq(payoutId))
                .forUpdate();

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
    public void changePurpose(long payoutId, String purpose) throws DaoException {
        Query query = getDslContext().update(PAYOUT)
                .set(PAYOUT.PURPOSE, purpose)
                .where(PAYOUT.ID.eq(payoutId));

        executeOne(query);
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

    @Override
    public List<ShopParams> getUnpaidShops(LocalDateTime fromTime, LocalDateTime toTime) throws DaoException {
        Query query = getDslContext()
                .select(PAYMENT.PARTY_ID, PAYMENT.SHOP_ID)
                .from(PAYMENT)
                .where(PAYMENT.PAYOUT_ID.isNull())
                .and(PAYMENT.TEST.eq(false))
                .and(PAYMENT.CAPTURED_AT.ge(fromTime))
                .and(PAYMENT.CAPTURED_AT.lt(toTime))
                .groupBy(PAYMENT.PARTY_ID, PAYMENT.SHOP_ID);

        return fetch(query, (rs, i) -> new ShopParams(rs.getString(PAYMENT.PARTY_ID.getName()), rs.getString(PAYMENT.SHOP_ID.getName())));
    }

    @Override
    public List<Payout> search(Optional<PayoutStatus> payoutStatus, Optional<LocalDateTime> fromTime, Optional<LocalDateTime> toTime, Optional<List<Long>> payoutIds, Optional<Long> fromIdOptional, Optional<Integer> sizeOptional) throws DaoException {
        SelectQuery query = getDslContext().selectQuery();
        query.addFrom(PAYOUT);
        payoutStatus.ifPresent(ps -> query.addConditions(PAYOUT.STATUS.eq(ps)));
        fromTime.ifPresent(from -> query.addConditions(PAYOUT.CREATED_AT.ge(from)));
        toTime.ifPresent(to -> query.addConditions(PAYOUT.CREATED_AT.lt(to)));
        payoutIds.ifPresent(ids -> query.addConditions(PAYOUT.ID.in(ids)));
        fromIdOptional.ifPresent(fromId -> query.addConditions(PAYOUT.ID.gt(fromId)));
        sizeOptional.ifPresent(size -> query.addLimit(size));
        return fetch(query, payoutRowMapper);
    }
}

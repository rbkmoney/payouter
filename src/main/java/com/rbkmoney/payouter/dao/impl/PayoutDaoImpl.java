package com.rbkmoney.payouter.dao.impl;

import com.rbkmoney.damsel.domain.CurrencyRef;
import com.rbkmoney.payouter.dao.PayoutDao;
import com.rbkmoney.payouter.dao.mapper.RecordRowMapper;
import com.rbkmoney.payouter.domain.enums.PayoutAccountType;
import com.rbkmoney.payouter.domain.enums.PayoutStatus;
import com.rbkmoney.payouter.domain.enums.PayoutType;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutRangeData;
import com.rbkmoney.payouter.exception.DaoException;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.SelectQuery;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.rbkmoney.payouter.domain.Tables.*;

@Component
public class PayoutDaoImpl extends AbstractGenericDao implements PayoutDao {

    private final RowMapper<Payout> payoutRowMapper;

    private final RowMapper<PayoutRangeData> payoutRangeDataRowMapper;

    @Autowired
    public PayoutDaoImpl(DataSource dataSource) {
        super(dataSource);
        payoutRowMapper = new RecordRowMapper<>(PAYOUT, Payout.class);
        payoutRangeDataRowMapper = new RecordRowMapper<>(PAYOUT_RANGE_DATA, PayoutRangeData.class);
    }

    @Override
    public Payout get(String payoutId) throws DaoException {
        Query query = getDslContext().selectFrom(PAYOUT)
                .where(PAYOUT.PAYOUT_ID.eq(payoutId));

        return fetchOne(query, payoutRowMapper);
    }

    @Override
    public Payout getExclusive(String payoutId) throws DaoException {
        Query query = getDslContext().selectFrom(PAYOUT)
                .where(PAYOUT.PAYOUT_ID.eq(payoutId))
                .forUpdate();

        return fetchOne(query, payoutRowMapper);
    }

    @Override
    public List<Payout> get(List<String> payoutIds) throws DaoException {
        Query query = getDslContext().selectFrom(PAYOUT)
                .where(PAYOUT.PAYOUT_ID.in(payoutIds));

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
    public void changeStatus(String payoutId, PayoutStatus payoutStatus) throws DaoException {
        Query query = getDslContext().update(PAYOUT)
                .set(PAYOUT.STATUS, payoutStatus)
                .where(PAYOUT.PAYOUT_ID.eq(payoutId));

        executeOne(query);
    }

    @Override
    public PayoutRangeData getRangeData(String payoutId) throws DaoException {
        Query query = getDslContext().selectFrom(PAYOUT_RANGE_DATA)
                .where(PAYOUT_RANGE_DATA.PAYOUT_ID.eq(payoutId));

        return fetchOne(query, payoutRangeDataRowMapper);
    }

    @Override
    public long saveRangeData(String payoutId, String partyId, String shopId, LocalDateTime fromTime, LocalDateTime toTime) throws DaoException {
        Query query = getDslContext()
                .insertInto(PAYOUT_RANGE_DATA)
                .set(PAYOUT_RANGE_DATA.PARTY_ID, partyId)
                .set(PAYOUT_RANGE_DATA.SHOP_ID, shopId)
                .set(PAYOUT_RANGE_DATA.PAYOUT_ID, payoutId)
                .set(PAYOUT_RANGE_DATA.FROM_TIME, fromTime)
                .set(PAYOUT_RANGE_DATA.TO_TIME, toTime);

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        executeOneWithReturn(query, keyHolder);

        return keyHolder.getKey().longValue();
    }

    @Override
    public int includeUnpaid(String payoutId, String partyId, String shopId) throws DaoException {
        Query query = getDslContext().update(PAYOUT)
                .set(PAYOUT.PAYOUT_REF, payoutId)
                .where(PAYOUT.STATUS.eq(PayoutStatus.CONFIRMED)
                        .and(PAYOUT.PARTY_ID.eq(partyId))
                        .and(PAYOUT.SHOP_ID.eq(shopId))
                        .and(PAYOUT.TYPE.eq(PayoutType.wallet))
                        .and(PAYOUT.PAYOUT_REF.isNull()));
        return execute(query);
    }

    @Override
    public int excludeFromPayout(String payoutId) throws DaoException {
        Query query = getDslContext().update(PAYOUT)
                .set(PAYOUT.PAYOUT_REF, (String) null)
                .where(PAYOUT.PAYOUT_REF.eq(payoutId));
        return execute(query);
    }

    @Override
    public long getAvailableAmount(String payoutId) throws DaoException {
        Field<Long> paymentAmount = getDslContext()
                .select(DSL.coalesce(DSL.sum(PAYMENT.AMOUNT.minus(PAYMENT.FEE).minus(PAYMENT.GUARANTEE_DEPOSIT)), 0L))
                .from(PAYMENT).where(PAYMENT.PAYOUT_ID.eq(payoutId)).asField();

        Field<Long> refundAmount = getDslContext()
                .select(DSL.coalesce(DSL.sum(REFUND.AMOUNT.plus(REFUND.FEE)), 0L))
                .from(REFUND).where(REFUND.PAYOUT_ID.eq(payoutId)).asField();

        Field<Long> adjustmentAmount = getDslContext()
                .select(DSL.coalesce(DSL.sum(ADJUSTMENT.PAYMENT_FEE.minus(ADJUSTMENT.NEW_FEE)), 0L))
                .from(ADJUSTMENT).where(ADJUSTMENT.PAYOUT_ID.eq(payoutId)).asField();

        Field<Long> payoutAmount = getDslContext()
                .select(DSL.coalesce(DSL.sum(PAYOUT.AMOUNT), 0L)) //payout fee?
                .from(PAYOUT).where(PAYOUT.PAYOUT_REF.eq(payoutId)).asField();

        Query query = getDslContext().select(
                paymentAmount
                        .plus(adjustmentAmount)
                        .minus(refundAmount)
                        .minus(payoutAmount)
        );

        return fetchOne(query, Long.class);
    }

    @Override
    public List<Payout> getUnpaidPayoutsByAccountType(PayoutAccountType accountType) throws DaoException {
        Query query = getDslContext().selectFrom(PAYOUT)
                .where(
                        PAYOUT.STATUS.eq(PayoutStatus.UNPAID)
                                .and(PAYOUT.ACCOUNT_TYPE.eq(accountType))
                )
                .forUpdate();

        return fetch(query, payoutRowMapper);
    }

    @Override
    public List<Payout> search(
            Optional<PayoutStatus> payoutStatus,
            Optional<LocalDateTime> fromTime,
            Optional<LocalDateTime> toTime,
            Optional<List<Long>> payoutIds,
            Optional<Long> minAmountOptional,
            Optional<Long> maxAmountOptional,
            Optional<CurrencyRef> currencyOptional,
            Optional<Long> fromIdOptional,
            Optional<Integer> sizeOptional
    ) throws DaoException {
        SelectQuery query = getDslContext().selectQuery();
        query.addFrom(PAYOUT);
        payoutStatus.ifPresent(ps -> query.addConditions(PAYOUT.STATUS.eq(ps)));
        fromTime.ifPresent(from -> query.addConditions(PAYOUT.CREATED_AT.ge(from)));
        toTime.ifPresent(to -> query.addConditions(PAYOUT.CREATED_AT.lt(to)));
        payoutIds.ifPresent(ids -> query.addConditions(PAYOUT.ID.in(ids)));
        minAmountOptional.ifPresent(minAmount -> query.addConditions(PAYOUT.AMOUNT.ge(minAmount)));
        maxAmountOptional.ifPresent(maxAmount -> query.addConditions(PAYOUT.AMOUNT.le(maxAmount)));
        currencyOptional.ifPresent(currencyRef -> query.addConditions(PAYOUT.CURRENCY_CODE.eq(currencyRef.getSymbolicCode())));
        fromIdOptional.ifPresent(fromId -> query.addConditions(PAYOUT.ID.gt(fromId)));
        sizeOptional.ifPresent(size -> query.addLimit(size));
        query.addOrderBy(PAYOUT.CREATED_AT.desc());
        return fetch(query, payoutRowMapper);
    }

    @Override
    public List<Payout> getByIds(Set<String> payoutIds) throws DaoException {
        Query query = getDslContext()
                .selectFrom(PAYOUT)
                .where(PAYOUT.PAYOUT_ID.in(payoutIds))
                .orderBy(PAYOUT.ID);

        return fetch(query, payoutRowMapper);
    }
}

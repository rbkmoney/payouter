package com.rbkmoney.payouter.dao.impl;

import com.rbkmoney.damsel.domain.CurrencyRef;
import com.rbkmoney.payouter.dao.PayoutDao;
import com.rbkmoney.payouter.dao.mapper.RecordRowMapper;
import com.rbkmoney.payouter.domain.enums.PayoutAccountType;
import com.rbkmoney.payouter.domain.enums.PayoutStatus;
import com.rbkmoney.payouter.domain.enums.PayoutSummaryOperationType;
import com.rbkmoney.payouter.domain.enums.PayoutType;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutRangeData;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutSummary;
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
import java.util.Set;

import static com.rbkmoney.payouter.domain.Tables.*;
import static com.rbkmoney.payouter.domain.tables.Refund.REFUND;

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
                .set(PAYOUT_RANGE_DATA.TO_TIME, toTime)
                .returning(PAYOUT_RANGE_DATA.ID);

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
                                .and(PAYOUT.TYPE.eq(PayoutType.bank_account))
                                .and(PAYOUT.ACCOUNT_TYPE.eq(accountType))
                )
                .forUpdate();

        return fetch(query, payoutRowMapper);
    }

    @Override
    public List<Payout> search(
            PayoutStatus payoutStatus,
            LocalDateTime fromTime,
            LocalDateTime toTime,
            List<String> payoutIds,
            Long minAmount,
            Long maxAmount,
            CurrencyRef currency,
            PayoutType payoutType,
            Long fromId,
            int size
    ) throws DaoException {
        SelectQuery query = getDslContext().selectQuery();
        query.addFrom(PAYOUT);
        if (payoutStatus != null) {
            query.addConditions(PAYOUT.STATUS.eq(payoutStatus));
        }
        if (fromTime != null) {
            query.addConditions(PAYOUT.CREATED_AT.ge(fromTime));
        }
        if (toTime != null) {
            query.addConditions(PAYOUT.CREATED_AT.lt(toTime));
        }
        if (payoutIds != null) {
            query.addConditions(PAYOUT.PAYOUT_ID.in(payoutIds));
        }
        if (minAmount != null) {
            query.addConditions(PAYOUT.AMOUNT.ge(minAmount));
        }
        if (maxAmount != null) {
            query.addConditions(PAYOUT.AMOUNT.le(maxAmount));
        }
        if (currency != null) {
            query.addConditions(PAYOUT.CURRENCY_CODE.eq(currency.getSymbolicCode()));
        }
        if (payoutType != null) {
            query.addConditions(PAYOUT.TYPE.eq(payoutType));
        }
        if (fromId != null) {
            query.addConditions(PAYOUT.ID.lt(fromId));
        }
        query.addLimit(size);
        query.addOrderBy(PAYOUT.ID.desc());
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

    @Override
    public PayoutSummary getSummary(String payoutId) throws DaoException {
        Field currencyCodeField = PAYOUT.CURRENCY_CODE;
        Field amountField = DSL.sum(PAYOUT.AMOUNT).as("amount");
        Field feeField = DSL.sum(PAYOUT.FEE).as("fee");
        Field countField = DSL.count().as("count");
        Field fromTimeField = DSL.min(PAYOUT.CREATED_AT).as("from_time");
        Field toTimeField = DSL.max(PAYOUT.CREATED_AT).as("to_time");

        Query query = getDslContext()
                .select(
                        currencyCodeField,
                        amountField,
                        feeField,
                        countField,
                        fromTimeField,
                        toTimeField
                ).from(PAYOUT)
                .where(PAYOUT.PAYOUT_REF.eq(payoutId))
                .groupBy(currencyCodeField);
        return fetchOne(query, (resultSet, i) -> {
            PayoutSummary payoutSummary = new PayoutSummary();
            payoutSummary.setPayoutId(payoutId);
            payoutSummary.setAmount(resultSet.getLong(amountField.getName()));
            payoutSummary.setFee(resultSet.getLong(feeField.getName()));
            payoutSummary.setCount(resultSet.getInt(countField.getName()));
            payoutSummary.setFromTime(resultSet.getObject(fromTimeField.getName(), LocalDateTime.class));
            payoutSummary.setToTime(resultSet.getObject(toTimeField.getName(), LocalDateTime.class));
            payoutSummary.setCurrencyCode(resultSet.getString(currencyCodeField.getName()));
            payoutSummary.setCashFlowType(PayoutSummaryOperationType.payout);
            return payoutSummary;
        });
    }
}

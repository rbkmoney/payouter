package com.rbkmoney.payouter.dao.impl;

import com.rbkmoney.payouter.dao.RefundDao;
import com.rbkmoney.payouter.dao.mapper.RecordRowMapper;
import com.rbkmoney.payouter.domain.enums.PayoutSummaryOperationType;
import com.rbkmoney.payouter.domain.enums.RefundStatus;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutSummary;
import com.rbkmoney.payouter.domain.tables.pojos.Refund;
import com.rbkmoney.payouter.domain.tables.records.RefundRecord;
import com.rbkmoney.payouter.exception.DaoException;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;

import static com.rbkmoney.payouter.domain.tables.Payment.PAYMENT;
import static com.rbkmoney.payouter.domain.tables.Refund.REFUND;

@Component
public class RefundDaoImpl extends AbstractGenericDao implements RefundDao {

    private final RowMapper<Refund> refundRowMapper;

    @Autowired
    public RefundDaoImpl(DataSource dataSource) {
        super(dataSource);
        refundRowMapper = new RecordRowMapper<>(REFUND, Refund.class);
    }

    @Override
    public void save(Refund refund) throws DaoException {
        RefundRecord refundRecord = getDslContext().newRecord(REFUND, refund);
        Query query = getDslContext().insertInto(REFUND)
                .set(refundRecord)
                .onConflict(REFUND.INVOICE_ID, REFUND.PAYMENT_ID, REFUND.REFUND_ID)
                .doUpdate()
                .set(refundRecord);
        executeOne(query);
    }

    @Override
    public Refund get(String invoiceId, String paymentId, String refundId) throws DaoException {
        Query query = getDslContext().selectFrom(REFUND)
                .where(REFUND.INVOICE_ID.eq(invoiceId)
                        .and(REFUND.PAYMENT_ID.eq(paymentId))
                        .and(REFUND.REFUND_ID.eq(refundId)));

        return fetchOne(query, refundRowMapper);
    }

    @Override
    public void markAsSucceeded(long eventId, String invoiceId, String paymentId, String refundId, LocalDateTime succeededAt) throws DaoException {
        Query query = getDslContext().update(REFUND)
                .set(REFUND.STATUS, RefundStatus.SUCCEEDED)
                .set(REFUND.SUCCEEDED_AT, succeededAt)
                .where(REFUND.INVOICE_ID.eq(invoiceId)
                        .and(REFUND.PAYMENT_ID.eq(paymentId)
                                .and(REFUND.REFUND_ID.eq(refundId))));
        executeOne(query);
    }

    @Override
    public void markAsFailed(long eventId, String invoiceId, String paymentId, String refundId) throws DaoException {
        Query query = getDslContext().update(REFUND)
                .set(REFUND.STATUS, RefundStatus.FAILED)
                .where(
                        REFUND.INVOICE_ID.eq(invoiceId)
                                .and(REFUND.PAYMENT_ID.eq(paymentId))
                                .and(REFUND.REFUND_ID.eq(refundId))
                                .and(REFUND.PAYOUT_ID.isNull())
                );
        executeOne(query);
    }

    @Override
    public int includeUnpaid(String payoutId, String partyId, String shopId) throws DaoException {
        Query query = getDslContext()
                .update(REFUND)
                .set(REFUND.PAYOUT_ID, payoutId)
                .from(PAYMENT)
                .where(REFUND.INVOICE_ID.eq(PAYMENT.INVOICE_ID)
                        .and(REFUND.PAYMENT_ID.eq(PAYMENT.PAYMENT_ID))
                        .and(REFUND.PAYOUT_ID.isNull())
                        .and(PAYMENT.PARTY_ID.eq(partyId))
                        .and(PAYMENT.SHOP_ID.eq(shopId))
                        .and(REFUND.STATUS.eq(RefundStatus.SUCCEEDED)));
        return execute(query);
    }

    @Override
    public int excludeFromPayout(String payoutId) throws DaoException {
        Query query = getDslContext().update(REFUND)
                .set(REFUND.PAYOUT_ID, (String) null)
                .where(REFUND.PAYOUT_ID.eq(payoutId));
        return execute(query);
    }

    @Override
    public PayoutSummary getSummary(String payoutId) throws DaoException {
        Field currencyCodeField = REFUND.CURRENCY_CODE;
        Field amountField = DSL.sum(REFUND.AMOUNT).as("amount");
        Field feeField = DSL.sum(REFUND.FEE).as("fee");
        Field countField = DSL.count().as("count");
        Field fromTimeField = DSL.min(REFUND.SUCCEEDED_AT).as("from_time");
        Field toTimeField = DSL.max(REFUND.SUCCEEDED_AT).as("to_time");


        Query query = getDslContext()
                .select(
                        currencyCodeField,
                        amountField,
                        feeField,
                        countField,
                        fromTimeField,
                        toTimeField
                ).from(REFUND)
                .where(REFUND.PAYOUT_ID.eq(payoutId))
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
            payoutSummary.setCashFlowType(PayoutSummaryOperationType.refund);
            return payoutSummary;
        });
    }
}

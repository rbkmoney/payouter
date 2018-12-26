package com.rbkmoney.payouter.dao.impl;

import com.rbkmoney.payouter.dao.PaymentDao;
import com.rbkmoney.payouter.dao.mapper.RecordRowMapper;
import com.rbkmoney.payouter.domain.enums.PaymentStatus;
import com.rbkmoney.payouter.domain.enums.PayoutSummaryOperationType;
import com.rbkmoney.payouter.domain.tables.pojos.Payment;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutSummary;
import com.rbkmoney.payouter.domain.tables.records.PaymentRecord;
import com.rbkmoney.payouter.exception.DaoException;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;

import static com.rbkmoney.payouter.domain.Tables.PAYMENT;

@Component
public class PaymentDaoImpl extends AbstractGenericDao implements PaymentDao {

    private final RowMapper<Payment> paymentRowMapper;

    @Autowired
    public PaymentDaoImpl(DataSource dataSource) {
        super(dataSource);
        paymentRowMapper = new RecordRowMapper<>(PAYMENT, Payment.class);
    }

    @Override
    public void save(Payment payment) throws DaoException {
        PaymentRecord paymentRecord = getDslContext().newRecord(PAYMENT, payment);
        Query query = getDslContext()
                .insertInto(PAYMENT)
                .set(paymentRecord)
                .onDuplicateKeyUpdate()
                .set(paymentRecord);
        executeOne(query);
    }

    @Override
    public Payment get(String invoiceId, String paymentId) throws DaoException {
        Query query = getDslContext().selectFrom(PAYMENT)
                .where(PAYMENT.INVOICE_ID.eq(invoiceId).and(PAYMENT.PAYMENT_ID.eq(paymentId)));

        return fetchOne(query, paymentRowMapper);
    }

    @Override
    public int excludeFromPayout(String payoutId) throws DaoException {
        Query query = getDslContext().update(PAYMENT)
                .set(PAYMENT.PAYOUT_ID, (String) null)
                .where(PAYMENT.PAYOUT_ID.eq(payoutId));
        return execute(query);
    }

    @Override
    public int includeUnpaid(String payoutId, String partyId, String shopId, LocalDateTime to) throws DaoException {
        Query query = getDslContext().update(PAYMENT)
                .set(PAYMENT.PAYOUT_ID, payoutId)
                .where(PAYMENT.STATUS.eq(PaymentStatus.CAPTURED)
                        .and(PAYMENT.PARTY_ID.eq(partyId))
                        .and(PAYMENT.SHOP_ID.eq(shopId))
                        .and(PAYMENT.CAPTURED_AT.lessThan(to))
                        .and(PAYMENT.PAYOUT_ID.isNull()));
        return execute(query);
    }

    @Override
    public void markAsCaptured(Long eventId, String invoiceId, String paymentId, LocalDateTime capturedAt) throws DaoException {
        Query query = getDslContext().update(PAYMENT)
                .set(PAYMENT.EVENT_ID, eventId)
                .set(PAYMENT.STATUS, PaymentStatus.CAPTURED)
                .set(PAYMENT.CAPTURED_AT, capturedAt)
                .where(PAYMENT.INVOICE_ID.eq(invoiceId).and(PAYMENT.PAYMENT_ID.eq(paymentId)));

        executeOne(query);
    }

    @Override
    public void markAsCancelled(Long eventId, String invoiceId, String paymentId) throws DaoException {
        Query query = getDslContext().update(PAYMENT)
                .set(PAYMENT.EVENT_ID, eventId)
                .set(PAYMENT.STATUS, PaymentStatus.CANCELLED)
                .where(PAYMENT.INVOICE_ID.eq(invoiceId).and(PAYMENT.PAYMENT_ID.eq(paymentId)));

        executeOne(query);
    }

    @Override
    public PayoutSummary getSummary(String payoutId) throws DaoException {
        Field currencyCodeField = PAYMENT.CURRENCY_CODE;
        Field amountField = DSL.sum(PAYMENT.AMOUNT).as("amount");
        Field feeField = DSL.sum(PAYMENT.FEE).as("fee");
        Field countField = DSL.count().as("count");
        Field fromTimeField = DSL.min(PAYMENT.CAPTURED_AT).as("from_time");
        Field toTimeField = DSL.max(PAYMENT.CAPTURED_AT).as("to_time");

        Query query = getDslContext()
                .select(
                        currencyCodeField,
                        amountField,
                        feeField,
                        countField,
                        fromTimeField,
                        toTimeField
                ).from(PAYMENT)
                .where(PAYMENT.PAYOUT_ID.eq(payoutId))
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
            payoutSummary.setCashFlowType(PayoutSummaryOperationType.payment);
            return payoutSummary;
        });
    }
}

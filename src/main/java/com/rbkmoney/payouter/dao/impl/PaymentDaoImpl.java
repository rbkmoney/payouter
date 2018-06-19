package com.rbkmoney.payouter.dao.impl;

import com.rbkmoney.payouter.dao.PaymentDao;
import com.rbkmoney.payouter.dao.mapper.RecordRowMapper;
import com.rbkmoney.payouter.domain.enums.PaymentStatus;
import com.rbkmoney.payouter.domain.tables.pojos.Payment;
import com.rbkmoney.payouter.exception.DaoException;
import org.jooq.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        Query query = getDslContext()
                .insertInto(PAYMENT).set(getDslContext().newRecord(PAYMENT, payment));
        executeOne(query);
    }

    @Override
    public void updatePaymentMeta(String invoiceId, String paymentId, String contractId, Long partyRevision) throws DaoException {
        Query query = getDslContext()
                .update(PAYMENT)
                .set(PAYMENT.CONTRACT_ID, contractId)
                .set(PAYMENT.PARTY_REVISION, partyRevision)
                .where(
                        PAYMENT.INVOICE_ID.eq(invoiceId)
                        .and(PAYMENT.PAYMENT_ID.eq(paymentId))
                );

        executeOne(query);
    }

    @Override
    public Optional<Long> getLastUpdatedEventId() throws DaoException {
        Query query = getDslContext()
                .select(PAYMENT.EVENT_ID.min())
                .from(PAYMENT)
                .where(PAYMENT.CONTRACT_ID.isNull());

        return Optional.ofNullable(fetchOne(query, Long.class));
    }

    @Override
    public Payment get(String invoiceId, String paymentId) throws DaoException {
        Query query = getDslContext().selectFrom(PAYMENT)
                .where(PAYMENT.INVOICE_ID.eq(invoiceId).and(PAYMENT.PAYMENT_ID.eq(paymentId)));

        return fetchOne(query, paymentRowMapper);
    }

    @Override
    public List<Payment> getByPayoutId(long payoutId) throws DaoException {
        Query query = getDslContext().selectFrom(PAYMENT)
                .where(PAYMENT.PAYOUT_ID.eq(payoutId))
                .orderBy(PAYMENT.CAPTURED_AT);
        return fetch(query, paymentRowMapper);
    }

    @Override
    public void includeToPayout(long payoutId, List<Payment> payments) throws DaoException {
        Set<Long> paymentsIds = payments.stream()
                .map(payment -> payment.getId())
                .collect(Collectors.toSet());

        Query query = getDslContext().update(PAYMENT)
                .set(PAYMENT.PAYOUT_ID, payoutId)
                .where(PAYMENT.ID.in(paymentsIds));
        execute(query, paymentsIds.size());
    }

    @Override
    public int excludeFromPayout(long payoutId) throws DaoException {
        Query query = getDslContext().update(PAYMENT)
                .set(PAYMENT.PAYOUT_ID, (Long) null)
                .where(PAYMENT.PAYOUT_ID.eq(payoutId));
        return execute(query);
    }

    @Override
    public List<Payment> getUnpaid(String partyId, String shopId, LocalDateTime to) throws DaoException {
        Query query = getDslContext().select().from(PAYMENT)
                .where(PAYMENT.STATUS.eq(PaymentStatus.CAPTURED)
                        .and(PAYMENT.PARTY_ID.eq(partyId))
                        .and(PAYMENT.SHOP_ID.eq(shopId))
                        .and(PAYMENT.CAPTURED_AT.lessThan(to))
                        .and(PAYMENT.PAYOUT_ID.isNull()));
        return fetch(query, paymentRowMapper);
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
}

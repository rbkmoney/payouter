package com.rbkmoney.payouter.dao.impl;

import com.rbkmoney.payouter.dao.PaymentDao;
import com.rbkmoney.payouter.dao.mapper.RecordRowMapper;
import com.rbkmoney.payouter.domain.enums.PaymentStatus;
import com.rbkmoney.payouter.domain.tables.pojos.Payment;
import com.rbkmoney.payouter.domain.tables.records.PaymentRecord;
import com.rbkmoney.payouter.exception.DaoException;
import org.jooq.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedRuntimeException;
import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;

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
    public List<Payment> getByPayoutId(long payoutId) throws DaoException {
        Query query = getDslContext().selectFrom(PAYMENT)
                .where(PAYMENT.PAYOUT_ID.eq(payoutId))
                .orderBy(PAYMENT.CAPTURED_AT);
        return fetch(query, paymentRowMapper);
    }

    @Override
    public List<String> getContracts(String partyId, String shopId, LocalDateTime to) throws DaoException {
        Query query = getDslContext().select(PAYMENT.CONTRACT_ID).from(PAYMENT)
                .where(
                        PAYMENT.PARTY_ID.eq(partyId)
                                .and(PAYMENT.SHOP_ID.eq(shopId))
                                .and(PAYMENT.CAPTURED_AT.lt(to))
                                .and(PAYMENT.PAYOUT_ID.isNull())
                ).groupBy(PAYMENT.CONTRACT_ID);
        return fetch(query, new SingleColumnRowMapper<>(String.class));
    }

    @Override
    public void includeToPayout(long payoutId, List<Payment> payments) throws DaoException {
        try {
            String batchSql = "update sht.payment set payout_id = ? where id = ?";
            int[][] updateCounts = getJdbcTemplate().batchUpdate(
                    batchSql,
                    payments,
                    1000,
                    (ps, payment) -> {
                        ps.setLong(1, payoutId);
                        ps.setLong(2, payment.getId());
                    });
            boolean checked = false;
            for (int i = 0; i < updateCounts.length; ++i) {
                for (int j = 0; j < updateCounts[i].length; ++j) {
                    checked = true;
                    if (updateCounts[i][j] != 1) {
                        throw new JdbcUpdateAffectedIncorrectNumberOfRowsException(batchSql, 1, updateCounts[i][j]);
                    }
                }
            }
            if (!checked) {
                throw new JdbcUpdateAffectedIncorrectNumberOfRowsException(batchSql, 1, 0);
            }
        } catch (NestedRuntimeException ex) {
            throw new DaoException(ex);
        }
    }

    @Override
    public int excludeFromPayout(long payoutId) throws DaoException {
        Query query = getDslContext().update(PAYMENT)
                .set(PAYMENT.PAYOUT_ID, (Long) null)
                .where(PAYMENT.PAYOUT_ID.eq(payoutId));
        return execute(query);
    }

    @Override
    public List<Payment> getUnpaid(String partyId, String shopId, String contractId, LocalDateTime to) throws DaoException {
        Query query = getDslContext().select().from(PAYMENT)
                .where(PAYMENT.STATUS.eq(PaymentStatus.CAPTURED)
                        .and(PAYMENT.PARTY_ID.eq(partyId))
                        .and(PAYMENT.SHOP_ID.eq(shopId))
                        .and(PAYMENT.CONTRACT_ID.eq(contractId))
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

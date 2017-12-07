package com.rbkmoney.payouter.dao.impl;

import com.rbkmoney.payouter.dao.AdjustmentDao;
import com.rbkmoney.payouter.dao.mapper.RecordRowMapper;
import com.rbkmoney.payouter.domain.enums.AdjustmentStatus;
import com.rbkmoney.payouter.domain.tables.pojos.Adjustment;
import com.rbkmoney.payouter.exception.DaoException;
import org.jooq.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rbkmoney.payouter.domain.Tables.ADJUSTMENT;
import static com.rbkmoney.payouter.domain.Tables.PAYMENT;

@Component
public class AdjustmentDaoImpl extends AbstractGenericDao implements AdjustmentDao {

    private final RowMapper<Adjustment> adjustmentRowMapper;

    @Autowired
    public AdjustmentDaoImpl(DataSource dataSource) {
        super(dataSource);
        adjustmentRowMapper = new RecordRowMapper<>(ADJUSTMENT, Adjustment.class);
    }

    @Override
    public void save(Adjustment adjustment) throws DaoException {
        Query query = getDslContext().insertInto(ADJUSTMENT)
                .set(getDslContext().newRecord(ADJUSTMENT, adjustment));
        executeOne(query);
    }

    @Override
    public Adjustment get(String invoiceId, String paymentId, String adjustmentId) throws DaoException {
        Query query = getDslContext().selectFrom(ADJUSTMENT)
                .where(ADJUSTMENT.INVOICE_ID.eq(invoiceId)
                        .and(ADJUSTMENT.PAYMENT_ID.eq(paymentId))
                        .and(ADJUSTMENT.ADJUSTMENT_ID.eq(adjustmentId)));

        return fetchOne(query, adjustmentRowMapper);
    }

    @Override
    public void markAsCaptured(long eventId, String invoiceId, String paymentId, String adjustmentId, LocalDateTime capturedAt) throws DaoException {
        Query query = getDslContext().update(ADJUSTMENT)
                .set(ADJUSTMENT.STATUS, AdjustmentStatus.CAPTURED)
                .set(ADJUSTMENT.CAPTURED_AT, capturedAt)
                .where(ADJUSTMENT.PAYMENT_ID.eq(paymentId)
                        .and(ADJUSTMENT.ADJUSTMENT_ID.eq(adjustmentId)
                                .and(ADJUSTMENT.INVOICE_ID.eq(invoiceId))));
        executeOne(query);
    }

    @Override
    public void markAsCancelled(long eventId, String invoiceId, String paymentId, String adjustmentId) throws DaoException {
        Query query = getDslContext().update(ADJUSTMENT)
                .set(ADJUSTMENT.STATUS, AdjustmentStatus.CANCELLED)
                .where(ADJUSTMENT.PAYMENT_ID.eq(paymentId)
                        .and(ADJUSTMENT.ADJUSTMENT_ID.eq(adjustmentId)
                                .and(ADJUSTMENT.INVOICE_ID.eq(invoiceId))));
        executeOne(query);
    }

    @Override
    public List<Adjustment> getUnpaid(String partyId, String shopId, LocalDateTime to) throws DaoException {
        Query query = getDslContext().select()
                .from(ADJUSTMENT)
                .join(PAYMENT).on(ADJUSTMENT.INVOICE_ID.eq(PAYMENT.INVOICE_ID)
                        .and(ADJUSTMENT.PAYMENT_ID.eq(PAYMENT.PAYMENT_ID))
                        .and(PAYMENT.CAPTURED_AT.lessThan(to)))
                .where(ADJUSTMENT.STATUS.eq(AdjustmentStatus.CAPTURED)
                        .and(ADJUSTMENT.PARTY_ID.eq(partyId))
                        .and(ADJUSTMENT.SHOP_ID.eq(shopId))
                        .and(ADJUSTMENT.PAYOUT_ID.isNull()));
        return fetch(query, adjustmentRowMapper);
    }

    @Override
    public List<Adjustment> getByPayoutId(long payoutId) throws DaoException {
        Query query = getDslContext().selectFrom(ADJUSTMENT)
                .where(ADJUSTMENT.PAYOUT_ID.eq(payoutId))
                .orderBy(ADJUSTMENT.CAPTURED_AT);
        return fetch(query, adjustmentRowMapper);
    }

    @Override
    public void includeToPayout(long payoutId, List<Adjustment> adjustments) throws DaoException {
        Set<Long> adjustmentsIds = adjustments.stream()
                .map(adjustment -> adjustment.getId())
                .collect(Collectors.toSet());

        Query query = getDslContext().update(ADJUSTMENT)
                .set(ADJUSTMENT.PAYOUT_ID, payoutId)
                .where(ADJUSTMENT.ID.in(adjustmentsIds));
        execute(query, adjustmentsIds.size());
    }

    @Override
    public int excludeFromPayout(long payoutId) throws DaoException {
        Query query = getDslContext().update(ADJUSTMENT)
                .set(ADJUSTMENT.PAYOUT_ID, (Long) null)
                .where(ADJUSTMENT.PAYOUT_ID.eq(payoutId));
        return execute(query);
    }
}

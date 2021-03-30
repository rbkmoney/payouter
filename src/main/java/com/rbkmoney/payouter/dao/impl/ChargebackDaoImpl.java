package com.rbkmoney.payouter.dao.impl;

import com.rbkmoney.payouter.dao.ChargebackDao;
import com.rbkmoney.payouter.dao.mapper.RecordRowMapper;
import com.rbkmoney.payouter.domain.enums.ChargebackStatus;
import com.rbkmoney.payouter.domain.tables.pojos.Chargeback;
import com.rbkmoney.payouter.domain.tables.records.ChargebackRecord;
import com.rbkmoney.payouter.exception.DaoException;
import org.jooq.Query;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;

import static com.rbkmoney.payouter.domain.tables.Chargeback.CHARGEBACK;

@Component
public class ChargebackDaoImpl extends AbstractGenericDao implements ChargebackDao {

    private final RowMapper<Chargeback> chargebackRowMapper;

    public ChargebackDaoImpl(DataSource dataSource) {
        super(dataSource);
        this.chargebackRowMapper = new RecordRowMapper<>(CHARGEBACK, Chargeback.class);
    }


    @Override
    public void save(Chargeback chargeback) throws DaoException {
        ChargebackRecord chargebackRecord = getDslContext().newRecord(CHARGEBACK, chargeback);
        chargebackRecord.reset(CHARGEBACK.PAYOUT_ID);
        Query query = getDslContext().insertInto(CHARGEBACK)
                .set(chargebackRecord)
                .onConflict(CHARGEBACK.INVOICE_ID, CHARGEBACK.PAYMENT_ID, CHARGEBACK.CHARGEBACK_ID)
                .doUpdate()
                .set(chargebackRecord);
        executeOne(query);
    }

    @Override
    public Chargeback get(String invoiceId, String paymentId, String chargebackId) throws DaoException {
        Query query = getDslContext().selectFrom(CHARGEBACK)
                .where(CHARGEBACK.INVOICE_ID.eq(invoiceId)
                        .and(CHARGEBACK.PAYMENT_ID.eq(paymentId))
                        .and(CHARGEBACK.CHARGEBACK_ID.eq(chargebackId)));

        return fetchOne(query, chargebackRowMapper);
    }

    @Override
    public void markAsAccepted(long eventId, String invoiceId, String paymentId, String chargebackId,
                               LocalDateTime succeededAt) throws DaoException {
        Query query = getDslContext().update(CHARGEBACK)
                .set(CHARGEBACK.STATUS, ChargebackStatus.SUCCEEDED)
                .set(CHARGEBACK.SUCCEEDED_AT, succeededAt)
                .where(CHARGEBACK.INVOICE_ID.eq(invoiceId)
                        .and(CHARGEBACK.PAYMENT_ID.eq(paymentId)
                                .and(CHARGEBACK.CHARGEBACK_ID.eq(chargebackId))));
        executeOne(query);
    }

    @Override
    public void markAsRejected(long eventId, String invoiceId, String paymentId, String chargebackId)
            throws DaoException {
        Query query = getDslContext().update(CHARGEBACK)
                .set(CHARGEBACK.STATUS, ChargebackStatus.REJECTED)
                .where(
                        CHARGEBACK.INVOICE_ID.eq(invoiceId)
                                .and(CHARGEBACK.PAYMENT_ID.eq(paymentId))
                                .and(CHARGEBACK.CHARGEBACK_ID.eq(chargebackId))
                                .and(CHARGEBACK.PAYOUT_ID.isNull())
                );
        executeOne(query);
    }

    @Override
    public void markAsCancelled(long eventId, String invoiceId, String paymentId, String chargebackId)
            throws DaoException {
        Query query = getDslContext().update(CHARGEBACK)
                .set(CHARGEBACK.STATUS, ChargebackStatus.CANCELLED)
                .where(
                        CHARGEBACK.INVOICE_ID.eq(invoiceId)
                                .and(CHARGEBACK.PAYMENT_ID.eq(paymentId))
                                .and(CHARGEBACK.CHARGEBACK_ID.eq(chargebackId))
                                .and(CHARGEBACK.PAYOUT_ID.isNull())
                );
        executeOne(query);
    }

    @Override
    public int includeUnpaid(String payoutId, String partyId, String shopId) throws DaoException {
        Query query = getDslContext()
                .update(CHARGEBACK)
                .set(CHARGEBACK.PAYOUT_ID, payoutId)
                .where(
                        CHARGEBACK.PARTY_ID.eq(partyId)
                                .and(CHARGEBACK.SHOP_ID.eq(shopId))
                                .and(CHARGEBACK.PAYOUT_ID.isNull())
                                .and(CHARGEBACK.STATUS.eq(ChargebackStatus.SUCCEEDED))
                );
        return execute(query);
    }

    @Override
    public int excludeFromPayout(String payoutId) throws DaoException {
        Query query = getDslContext().update(CHARGEBACK)
                .set(CHARGEBACK.PAYOUT_ID, (String) null)
                .where(CHARGEBACK.PAYOUT_ID.eq(payoutId));
        return execute(query);
    }

}

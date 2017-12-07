package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.domain.tables.pojos.Refund;
import com.rbkmoney.payouter.exception.DaoException;

import java.time.LocalDateTime;
import java.util.List;

public interface RefundDao extends GenericDao {

    void save(Refund refund) throws DaoException;

    Refund get(String invoiceId, String paymentId, String refundId) throws DaoException;

    void markAsSucceeded(long eventId, String invoiceId, String paymentId, String refundId, LocalDateTime succeededAt) throws DaoException;

    List<Refund> getUnpaid(String partyId, String shopId, LocalDateTime to) throws DaoException;

    void includeToPayout(long payoutId, List<Refund> refunds) throws DaoException;

    List<Refund> getByPayoutId(long payoutId) throws DaoException;

    int excludeFromPayout(long payoutId) throws DaoException;

}

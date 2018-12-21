package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.domain.tables.pojos.Payment;
import com.rbkmoney.payouter.exception.DaoException;

import java.time.LocalDateTime;

public interface PaymentDao extends GenericDao {

    void save(Payment payment) throws DaoException;

    Payment get(String invoiceId, String paymentId) throws DaoException;

    int excludeFromPayout(String payoutId) throws DaoException;

    int includeUnpaid(String payoutId, String partyId, String shopId, LocalDateTime to) throws DaoException;

    void markAsCaptured(Long eventId, String invoiceId, String paymentId, LocalDateTime capturedAt) throws DaoException;

    void markAsCancelled(Long eventId, String invoiceId, String paymentId) throws DaoException;

}

package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.domain.tables.pojos.Adjustment;
import com.rbkmoney.payouter.exception.DaoException;

import java.time.LocalDateTime;
import java.util.List;

public interface AdjustmentDao extends GenericDao {

    void save(Adjustment adjustment) throws DaoException;

    Adjustment get(String invoiceId, String paymentId, String adjustmentId) throws DaoException;

    void markAsCaptured(long eventId, String invoiceId, String paymentId, String adjustmentId, LocalDateTime capturedAt) throws DaoException;

    void markAsCancelled(long eventId, String invoiceId, String paymentId, String adjustmentId) throws DaoException;

    int includeUnpaid(String payoutId, String partyId, String shopId, LocalDateTime to) throws DaoException;

    int excludeFromPayout(String payoutId) throws DaoException;

}

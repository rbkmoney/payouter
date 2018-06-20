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

    List<String> getContracts(String partyId, String shopId, LocalDateTime to) throws DaoException;

    List<Adjustment> getUnpaid(String partyId, String shopId, String contractId, LocalDateTime to) throws DaoException;

    List<Adjustment> getByPayoutId(long payoutId) throws DaoException;

    void includeToPayout(long payoutId, List<Adjustment> adjustments) throws DaoException;

    int excludeFromPayout(long payoutId) throws DaoException;

}

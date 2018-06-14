package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.domain.tables.pojos.Invoice;
import com.rbkmoney.payouter.exception.DaoException;

import java.time.LocalDateTime;

public interface InvoiceDao extends GenericDao {

    void save(String invoiceId, String partyId, String shopId, String contractId, Long partyRevision, LocalDateTime createdAt) throws DaoException;

    Invoice get(String invoiceId) throws DaoException;

}

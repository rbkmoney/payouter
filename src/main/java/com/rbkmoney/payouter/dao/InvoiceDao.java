package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.domain.tables.pojos.Invoice;
import com.rbkmoney.payouter.exception.DaoException;

public interface InvoiceDao extends GenericDao {

    void save(String invoiceId, String partyId, String shopId) throws DaoException;

    Invoice getInvoice(String invoiceId) throws DaoException;

}

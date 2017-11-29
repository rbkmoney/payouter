package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.AbstractIntegrationTest;
import com.rbkmoney.payouter.domain.tables.pojos.Invoice;
import com.rbkmoney.payouter.exception.DaoException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.Assert.assertEquals;

public class InvoiceDaoTest extends AbstractIntegrationTest {

    @Autowired
    InvoiceDao invoiceDao;

    @Test
    public void testSaveAndGet() throws DaoException {
        Invoice invoice = random(Invoice.class);

        invoiceDao.save(invoice.getId(), invoice.getPartyId(), invoice.getShopId());
        //save again
        invoiceDao.save(invoice.getId(), invoice.getPartyId(), invoice.getShopId());

        assertEquals(invoice, invoiceDao.getInvoice(invoice.getId()));
    }

}

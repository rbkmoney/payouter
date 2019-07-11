package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.AbstractIntegrationTest;
import com.rbkmoney.payouter.domain.tables.pojos.Refund;
import com.rbkmoney.payouter.exception.DaoException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.Assert.assertEquals;

public class RefundDaoTest extends AbstractIntegrationTest {

    @Autowired
    RefundDao refundDao;

    @Test
    public void testSaveAndGet() throws DaoException {
        Refund refund = random(Refund.class, "payoutId");
        refundDao.save(refund);

        Refund secondRefund = new Refund(refund);
        secondRefund.setId(null);
        refundDao.save(secondRefund);

        assertEquals(refund, refundDao.get(refund.getInvoiceId(), refund.getPaymentId(), refund.getRefundId()));
    }

    @Test
    public void testSaveOnlyNonNullValues() throws DaoException {
        Refund refund = random(Refund.class, "reason", "payoutId");
        refundDao.save(refund);
        assertEquals(refund, refundDao.get(refund.getInvoiceId(), refund.getPaymentId(), refund.getRefundId()));
    }

}

package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.AbstractIntegrationTest;
import com.rbkmoney.payouter.domain.tables.pojos.Refund;
import com.rbkmoney.payouter.exception.DaoException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import java.util.Arrays;
import java.util.List;

import static com.rbkmoney.payouter.dao.DaoTestUtil.getNullColumnNames;
import static com.rbkmoney.payouter.domain.tables.Refund.REFUND;
import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.github.benas.randombeans.api.EnhancedRandom.randomListOf;
import static org.junit.Assert.*;

public class RefundDaoTest extends AbstractIntegrationTest {

    @Autowired
    RefundDao refundDao;

    @Test
    public void testSaveAndGet() throws DaoException {
        Refund refund = random(Refund.class);

        refundDao.save(refund);

        //save again
        try {
            refundDao.save(refund);
            fail();
        } catch (DaoException ex) {
            assertTrue(DuplicateKeyException.class.isAssignableFrom(ex.getCause().getClass()));
        }

        assertEquals(refund, refundDao.get(refund.getInvoiceId(), refund.getPaymentId(), refund.getRefundId()));
    }

    @Test
    public void testSaveOnlyNonNullValues() throws DaoException {
        Refund refund = random(Refund.class, getNullColumnNames(REFUND));
        refundDao.save(refund);
    }

    @Test
    public void testIncludeAndExcludeFromPayout() throws DaoException {
        List<Refund> refunds = randomListOf(10, Refund.class, "payoutId");
        long payoutId = 1;

        refunds.stream().forEach(payment -> refundDao.save(payment));

        assertTrue(refundDao.getByPayoutId(payoutId).isEmpty());
        assertEquals(refunds.size(), refundDao.includeToPayout(payoutId, refunds));
        assertEquals(refunds.size(), refundDao.getByPayoutId(payoutId).size());
        assertEquals(refunds.size(), refundDao.excludeFromPayout(payoutId));
        assertTrue(refundDao.getByPayoutId(payoutId).isEmpty());
    }

}

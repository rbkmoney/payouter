package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.AbstractIntegrationTest;
import com.rbkmoney.payouter.domain.tables.pojos.Adjustment;
import com.rbkmoney.payouter.exception.DaoException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.github.benas.randombeans.api.EnhancedRandom.randomListOf;
import static org.junit.Assert.*;

public class AdjustmentDaoTest extends AbstractIntegrationTest {

    @Autowired
    AdjustmentDao adjustmentDao;

    @Test
    public void testSaveAndGet() throws DaoException {
        Adjustment adjustment = random(Adjustment.class);

        adjustmentDao.save(adjustment);

        //save again
        try {
            adjustmentDao.save(adjustment);
            fail();
        } catch (DaoException ex) {
            assertTrue(DuplicateKeyException.class.isAssignableFrom(ex.getCause().getClass()));
        }

        assertEquals(adjustment, adjustmentDao.get(adjustment.getInvoiceId(), adjustment.getPaymentId(), adjustment.getAdjustmentId()));
    }

    @Test
    public void testSaveOnlyNonNullValues() throws DaoException {
        Adjustment adjustment = random(Adjustment.class, "payoutId");
        adjustmentDao.save(adjustment);
        assertEquals(adjustment, adjustmentDao.get(adjustment.getInvoiceId(), adjustment.getPaymentId(), adjustment.getAdjustmentId()));
    }

    @Test
    public void testIncludeAndExcludeFromPayout() throws DaoException {
        List<Adjustment> adjustments = randomListOf(10, Adjustment.class, "payoutId");
        long payoutId = 1;

        adjustments.stream().forEach(payment -> adjustmentDao.save(payment));

        assertTrue(adjustmentDao.getByPayoutId(payoutId).isEmpty());
        adjustmentDao.includeToPayout(payoutId, adjustments);
        assertEquals(adjustments.size(), adjustmentDao.getByPayoutId(payoutId).size());
        assertEquals(adjustments.size(), adjustmentDao.excludeFromPayout(payoutId));
        assertTrue(adjustmentDao.getByPayoutId(payoutId).isEmpty());
    }

}

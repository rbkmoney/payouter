package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.AbstractIntegrationTest;
import com.rbkmoney.payouter.domain.tables.pojos.Adjustment;
import com.rbkmoney.payouter.exception.DaoException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.Assert.assertEquals;

public class AdjustmentDaoTest extends AbstractIntegrationTest {

    @Autowired
    AdjustmentDao adjustmentDao;

    @Test
    public void testSaveAndGet() throws DaoException {
        Adjustment adjustment = random(Adjustment.class, "payoutId");
        adjustmentDao.save(adjustment);

        Adjustment secondAdjustment = new Adjustment(adjustment);
        adjustmentDao.save(secondAdjustment);

        assertEquals(
                adjustment,
                adjustmentDao.get(adjustment.getInvoiceId(), adjustment.getPaymentId(), adjustment.getAdjustmentId()));
    }

    @Test
    public void testSaveOnlyNonNullValues() throws DaoException {
        Adjustment adjustment = random(Adjustment.class, "payoutId");
        adjustmentDao.save(adjustment);
        assertEquals(
                adjustment,
                adjustmentDao.get(adjustment.getInvoiceId(), adjustment.getPaymentId(), adjustment.getAdjustmentId()));
    }

}

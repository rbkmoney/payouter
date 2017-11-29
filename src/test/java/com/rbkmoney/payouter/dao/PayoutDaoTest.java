package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.AbstractIntegrationTest;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.exception.DaoException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import static com.rbkmoney.payouter.dao.DaoTestUtil.getNullColumnNames;
import static com.rbkmoney.payouter.domain.Tables.PAYMENT;
import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.Assert.*;

public class PayoutDaoTest extends AbstractIntegrationTest {

    @Autowired
    PayoutDao payoutDao;

    @Test
    public void testSaveAndGet() throws DaoException {
        Payout payout = random(Payout.class, "id");

        long payoutId = payoutDao.save(payout);

        //save again
        try {
            payoutDao.save(payout);
            fail();
        } catch (DaoException ex) {
            assertTrue(DuplicateKeyException.class.isAssignableFrom(ex.getCause().getClass()));
        }

        assertEquals(payout, payoutDao.get(payoutId));
    }

    @Test
    public void testSaveOnlyNonNullValues() throws DaoException {
        Payout payout = random(Payout.class, getNullColumnNames(PAYMENT));
        payoutDao.save(payout);
    }

}

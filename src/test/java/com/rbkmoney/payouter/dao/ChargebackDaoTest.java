package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.AbstractIntegrationTest;
import com.rbkmoney.payouter.domain.enums.ChargebackCategory;
import com.rbkmoney.payouter.domain.tables.pojos.Chargeback;
import com.rbkmoney.payouter.exception.DaoException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.Assert.assertEquals;

public class ChargebackDaoTest extends AbstractIntegrationTest {

    @Autowired
    ChargebackDao chargebackDao;

    @Test
    public void testSaveAndGet() throws DaoException {
        Chargeback chargeback = random(Chargeback.class, "payoutId");
        chargebackDao.save(chargeback);

        Chargeback secChargeback = new Chargeback(chargeback);
        secChargeback.setId(null);
        chargebackDao.save(secChargeback);

        assertEquals(
                chargeback,
                chargebackDao.get(chargeback.getInvoiceId(), chargeback.getPaymentId(), chargeback.getChargebackId()));
    }

    @Test
    public void testSaveOnlyNonNullValues() throws DaoException {
        Chargeback chargeback = random(Chargeback.class, "reason", "payoutId");
        chargeback.setReasonCategory(ChargebackCategory.fraud);
        chargebackDao.save(chargeback);
        assertEquals(
                chargeback,
                chargebackDao.get(chargeback.getInvoiceId(), chargeback.getPaymentId(), chargeback.getChargebackId()));
    }

}

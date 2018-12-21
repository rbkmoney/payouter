package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.AbstractIntegrationTest;
import com.rbkmoney.payouter.domain.enums.PayoutStatus;
import com.rbkmoney.payouter.domain.enums.PayoutType;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.exception.DaoException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.Assert.*;

public class PayoutDaoTest extends AbstractIntegrationTest {

    @Autowired
    PayoutDao payoutDao;

    @Test
    public void testSaveAndGet() throws DaoException {
        Payout payout = random(Payout.class, "id");

        long payoutId = payoutDao.save(payout);
        payout.setId(payoutId);

        assertEquals(payout, payoutDao.get(payout.getPayoutId()));
    }

    @Test
    public void testSaveOnlyNonNullValues() throws DaoException {
        Payout payout = new Payout();
        payout.setPayoutId("kek");
        payout.setCreatedAt(LocalDateTime.now());
        payout.setPartyId("kek");
        payout.setShopId("kek");
        payout.setContractId("kek");
        payout.setStatus(PayoutStatus.PAID);
        payout.setType(PayoutType.bank_account);

        long payoutId = payoutDao.save(payout);
        payout.setId(payoutId);

        assertEquals(payout, payoutDao.get(payout.getPayoutId()));
    }

}

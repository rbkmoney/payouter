package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.accounter.AccounterSrv;
import com.rbkmoney.damsel.accounter.InvalidPostingParams;
import com.rbkmoney.payouter.AbstractIntegrationTest;
import com.rbkmoney.payouter.dao.PayoutDao;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.exception.AccounterException;
import org.apache.thrift.TException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

public class ShumwayServiceTest extends AbstractIntegrationTest {

    @Autowired
    private ShumwayService shumwayService;

    @Autowired
    private PayoutDao payoutDao;

    @MockBean
    private AccounterSrv.Iface shumwayClient;

    @Test
    @Transactional
    public void testRevertWhenHoldIsFailed() throws TException {
        given(shumwayClient.hold(any()))
                .willThrow(InvalidPostingParams.class);
        given(shumwayClient.rollbackPlan(any()))
                .willThrow(InvalidPostingParams.class);

        Payout payout = random(Payout.class);
        payoutDao.save(payout);
        try {
            shumwayService.revert(payout.getId());
            fail();
        } catch (AccounterException ex) {
            Throwable throwable = ex.getCause();
            assertEquals(1, throwable.getSuppressed().length);
            assertTrue(throwable.getSuppressed()[0] instanceof InvalidPostingParams);
        }
    }

}

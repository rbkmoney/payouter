package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.accounter.AccounterSrv;
import com.rbkmoney.damsel.accounter.InvalidPostingParams;
import com.rbkmoney.payouter.AbstractIntegrationTest;
import org.apache.thrift.TException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

public class ShumwayServiceTest extends AbstractIntegrationTest {

    @Autowired
    private ShumwayService shumwayService;

    @MockBean
    private AccounterSrv.Iface shumwayClient;

    @Test
    public void testRevertWhenHoldIsFailed() throws TException {
        given(shumwayClient.hold(any()))
                .willThrow(InvalidPostingParams.class);
        given(shumwayClient.rollbackPlan(any()))
                .willThrow(InvalidPostingParams.class);

        try {
            shumwayService.revert(3, Collections.emptyList());
            fail();
        } catch (RuntimeException ex) {
            Throwable throwable = ex.getCause();
            assertEquals(1, throwable.getSuppressed().length);
            assertTrue(throwable.getSuppressed()[0].getMessage().startsWith("Failed to rollback postings from revert action"));
        }
    }

}

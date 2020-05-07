package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.AbstractIntegrationTest;
import com.rbkmoney.payouter.domain.enums.PayoutSummaryOperationType;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutSummary;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class PayoutSummaryDaoTest extends AbstractIntegrationTest {

    @Autowired
    PayoutSummaryDao payoutSummaryDao;


    @Test
    public void saveAndGet() {
        ArrayList<PayoutSummary> payoutSummaries = new ArrayList<>();
        PayoutSummary e = new PayoutSummary();
        String payoutId = "1";
        e.setPayoutId(payoutId);
        e.setAmount(123L);
        e.setFee(12L);
        e.setCashFlowType(PayoutSummaryOperationType.payment);
        e.setCurrencyCode("RUB");
        e.setCount(22);
        e.setFromTime(LocalDateTime.now());
        e.setToTime(LocalDateTime.now());
        payoutSummaries.add(e);
        e = new PayoutSummary();
        e.setPayoutId(payoutId);
        e.setAmount(143L);
        e.setFee(14L);
        e.setCashFlowType(PayoutSummaryOperationType.refund);
        e.setCurrencyCode("RUB");
        e.setCount(55);
        e.setFromTime(LocalDateTime.now());
        e.setToTime(LocalDateTime.now());
        payoutSummaries.add(e);
        e = new PayoutSummary();
        e.setPayoutId(payoutId);
        e.setAmount(153L);
        e.setFee(16L);
        e.setCashFlowType(PayoutSummaryOperationType.chargeback);
        e.setCurrencyCode("RUB");
        e.setCount(64);
        e.setFromTime(LocalDateTime.now());
        e.setToTime(LocalDateTime.now());
        payoutSummaries.add(e);
        payoutSummaryDao.save(payoutSummaries);
        Assert.assertEquals(3, payoutSummaryDao.get(payoutId).size());
    }
}

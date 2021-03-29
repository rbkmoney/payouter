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
        PayoutSummary payoutSummary = new PayoutSummary();
        String payoutId = "1";
        payoutSummary.setPayoutId(payoutId);
        payoutSummary.setAmount(123L);
        payoutSummary.setFee(12L);
        payoutSummary.setCashFlowType(PayoutSummaryOperationType.payment);
        payoutSummary.setCurrencyCode("RUB");
        payoutSummary.setCount(22);
        payoutSummary.setFromTime(LocalDateTime.now());
        payoutSummary.setToTime(LocalDateTime.now());
        ArrayList<PayoutSummary> payoutSummaries = new ArrayList<>();
        payoutSummaries.add(payoutSummary);
        payoutSummary = new PayoutSummary();
        payoutSummary.setPayoutId(payoutId);
        payoutSummary.setAmount(143L);
        payoutSummary.setFee(14L);
        payoutSummary.setCashFlowType(PayoutSummaryOperationType.refund);
        payoutSummary.setCurrencyCode("RUB");
        payoutSummary.setCount(55);
        payoutSummary.setFromTime(LocalDateTime.now());
        payoutSummary.setToTime(LocalDateTime.now());
        payoutSummaries.add(payoutSummary);
        payoutSummary = new PayoutSummary();
        payoutSummary.setPayoutId(payoutId);
        payoutSummary.setAmount(153L);
        payoutSummary.setFee(16L);
        payoutSummary.setCashFlowType(PayoutSummaryOperationType.chargeback);
        payoutSummary.setCurrencyCode("RUB");
        payoutSummary.setCount(64);
        payoutSummary.setFromTime(LocalDateTime.now());
        payoutSummary.setToTime(LocalDateTime.now());
        payoutSummaries.add(payoutSummary);
        payoutSummaryDao.save(payoutSummaries);
        Assert.assertEquals(3, payoutSummaryDao.get(payoutId).size());
    }
}

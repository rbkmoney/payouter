package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.AbstractIntegrationTest;
import com.rbkmoney.payouter.domain.enums.CashFlowType;
import com.rbkmoney.payouter.domain.tables.pojos.CashFlowDescription;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class CashFlowDescriptionDaoTest extends AbstractIntegrationTest {

    @Autowired
    CashFlowDescriptionDao cashFlowDescriptionDao;


    @Test
    public void saveAndGet() {
        ArrayList<CashFlowDescription> cashFlowDescription = new ArrayList<>();
        CashFlowDescription e = new CashFlowDescription();
        String payoutId = "1";
        e.setPayoutId(payoutId);
        e.setAmount(123L);
        e.setFee(12L);
        e.setCashFlowType(CashFlowType.payment);
        e.setCurrencyCode("RUB");
        e.setCount(22);
        e.setFromTime(LocalDateTime.now());
        e.setToTime(LocalDateTime.now());
        cashFlowDescription.add(e);
        e = new CashFlowDescription();
        e.setPayoutId(payoutId);
        e.setAmount(143L);
        e.setFee(14L);
        e.setCashFlowType(CashFlowType.refund);
        e.setCurrencyCode("RUB");
        e.setCount(55);
        e.setFromTime(LocalDateTime.now());
        e.setToTime(LocalDateTime.now());
        cashFlowDescription.add(e);
        cashFlowDescriptionDao.save(cashFlowDescription);
        Assert.assertEquals(2, cashFlowDescriptionDao.get(payoutId).size());
    }
}